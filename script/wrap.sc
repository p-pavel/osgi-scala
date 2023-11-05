#! env -S scala-cli shebang -deprecation

/* Naive utilities to generate wrap:mvn urls */

//>using scala "3.2.2"
//>using lib "com.lihaoyi::scalatags:0.12.0"

/** maven artifact. Do not use sbt's ModuleID to keep thinks self-contained
  */
case class Artifact(
    organization: String,
    name: String,
    version: String,
    readableName: String,
    scalaVersion: Option[String] = Option("3"),
    requiredBundle: Option[String] = None,
    explicitPackageExport: Option[String] = None
):
  def withoutScalaVersion                  = copy(scalaVersion = None)
  def withRequiredBundle(k: String)        = copy(requiredBundle = Some(k))
  def withExplicitPackageExport(k: String) =
    copy(explicitPackageExport = Some(k))

  def versionSuffix = scalaVersion.fold("")(v => s"_$v")
  def baseURL       = s"wrap:mvn:$organization/$name$versionSuffix/$version"
  def bundleVersion = s"Bundle-Version=$version"
  def bundleName    =
    import java.net.URLEncoder as enc
    s"Bundle-Name=${enc.encode(readableName, "UTF-8")}"

  def requiredBundleArg = requiredBundle.fold("")(k => s"Require-Bundle=$k")

  def bundleSymbolicName    = s"$organization.$name"
  def bundleSymbolicNameArg = s"Bundle-SymbolicName=$bundleSymbolicName"
  def packageExport         =
    val exportString = explicitPackageExport.getOrElse(s"*;version=$version")
    s"Export-Package=$exportString"
  def packageImport         = "Import-Package=*"

  def wrapUrl: String =
    baseURL + "$" + Seq(
      bundleVersion,
      bundleName,
      bundleSymbolicNameArg,
      requiredBundleArg,
      packageExport
      // packageImport
    ).mkString("&")
end Artifact

//TODO: Generic ?
import scalatags.Text.all.*

case class KarafFeature(
    name: String,
    version: String,
    description: String,
    bundles: Seq[Artifact],
    features: Seq[KarafFeature] = Seq.empty
):
  def xmlTags: ConcreteHtmlTag[String] = tag("feature")(attr("name") := name)(
    attr("version")     := version,
    attr("description") := description,
    tag("feature")(attr("prerequisite") := true, "wrap"),
    features.map(f => tag("feature")(f.name)),
    bundles.map { b =>
      tag("bundle")(b.wrapUrl)
    }
  )
end KarafFeature

case class KarafFeatureRepository(
    name: String,
    features: KarafFeature*
):
  def xmlTags = tag("features")(
    xmlns        := "http://karaf.apache.org/xmlns/features/v1.4.0",
    attr("name") := name
  )(
    features.map(_.xmlTags)
  )

object bundles:
  val scalaExportedPackages = """scala
    scala.annotation
    scala.annotation.internal
    scala.annotation.meta
    scala.annotation.unchecked
    scala.beans
    scala.collection
    scala.collection.concurrent
    scala.collection.convert
    scala.collection.convert.impl
    scala.collection.generic
    scala.collection.immutable
    scala.collection.mutable
    scala.compat
    scala.compiletime
    scala.compiletime.ops
    scala.compiletime.testing
    scala.concurrent
    scala.concurrent.duration
    scala.concurrent.impl
    scala.deriving
    scala.io
    scala.jdk
    scala.jdk.javaapi
    scala.math
    scala.quoted
    scala.quoted.runtime
    scala.ref
    scala.reflect
    scala.reflect.macros.internal
    scala.runtime
    scala.runtime.coverage
    scala.runtime.function
    scala.runtime.java8
    scala.runtime.stdLibPatches
    scala.sys
    scala.sys.process
    scala.util
    scala.util.control
    scala.util.hashing
    scala.util.matching""".linesIterator
    .map(_.trim)
    .filter(_.nonEmpty)
    .mkString(";")

  def scribe(v: String) = //TODO: imports/exports not correct
    Seq(
      "scribe"       -> "Scribe",
      "scribe-slf4j" -> "Scribe :: SLF4J",
      "scribe-cats"  -> "Scribe :: Cats"
    ).map(
      Artifact( "com.outr", _, v, _))

  def sourceCode(v: String) = Artifact(
    "com.lihaoyi",
    "sourcecode",
    v,
    "SourceCode"
  )

  def scala213Lib = Artifact(
    "org.scala-lang",
    "scala-library",
    "2.13.10",
    "Scala :: Library"
  ).withoutScalaVersion

  def scala322Lib = Artifact(
    "org.scala-lang",
    "scala3-library",
    "3.2.2",
    "Scala3 :: Library"
  )
    .withRequiredBundle(
      scala213Lib.bundleSymbolicName + ";bundle-version=\"[2.13.10,3)\""
    )
    .withExplicitPackageExport(s"$scalaExportedPackages;version=3.2.2")

  /** typelevel is pervasive in the ecosystem, so we have a helper for it
    */
  def typelevel(version: String)(name: String, readableName: String) =
    Artifact("org.typelevel", name, version, readableName)

  def cats(v: String): Seq[Artifact] =
    Seq(
      "cats-core"   -> "Cats :: Core",
      "cats-kernel" -> "Cats :: Kernel"
    ) map typelevel(v)

  def catsEffect(v: String): Seq[Artifact] =
    Seq(
      "cats-effect"        -> "Cats :: Effect",
      "cats-effect-kernel" -> "Cats :: Effect :: Kernel",
      "cats-effect-std"    -> "Cats :: Effect :: Std"
    ) map typelevel(v)

  def http4s(v: String) =
    Seq(
      "http4s-core"         -> "Http4s :: Core",
      "http4s-dsl"          -> "Http4s :: DSL",
      "http4s-ember-core"   -> "Http4s :: Ember :: Core",
      "http4s-ember-server" -> "Http4s :: Ember :: Server",
      "http4s-ember-client" -> "Http4s :: Ember :: Client",
      "http4s-server"       -> "Http4s :: Server"
    ).map(Artifact("org.http4s", _, v, _)) ++ Seq(
      // org.http4s package is exported from both core and client
      Artifact("org.http4s", "http4s-client", v, "Http4s :: Client")
        .withExplicitPackageExport(s"!org.http4s;*;version=$v")
    )

  /** Twitter's hpack is used by http4s */
  def hpack(v: String) = Artifact(
    "com.twitter",
    "hpack",
    v,
    "Twitter :: HPack"
  ).withoutScalaVersion

  def http4sCrypto(v: String) = Artifact(
    "org.http4s",
    "http4s-crypto",
    v,
    "Http4s :: Crypto"
  )

  def ip4s(v: String) =
    Artifact("com.comcast", "ip4s-core", v, "IP4S")

  def scodec(v: String) =
    Artifact("org.scodec", "scodec-bits", v, "Scodec")

  def fs2(v: String) = Seq(
    "fs2-core" -> "FS2 :: Core",
    "fs2-io"   -> "FS2 :: IO"
  ) map { Artifact("co.fs2", _, v, _) }

  def log4cats(v: String) =
    Seq(
      "log4cats-core"  -> "Log4Cats :: Core",
      "log4cats-slf4j" -> "Log4Cats :: SLF4J"
    ).map(typelevel(v))

  def vault(v: String) = typelevel(v)("vault", "Typelevel :: Vault")

  def catsParse(v: String) =
    typelevel(v)("cats-parse", "Cats :: Parse")

  def keypool(v: String) =
    typelevel(v)("keypool", "Typelevel :: KeyPool")

  def literally(v: String) =
    typelevel(v)("literally", "Typelevel :: Literally")

  def caseInsensitive(v: String) =
    typelevel(v)("case-insensitive", "Typelevel :: Case Insensitive")
end bundles

object features:
  import bundles as b
  def feature(
      name: String,
      version: String,
      description: String,
      bundles: String => Seq[Artifact],
      deps: KarafFeature*
  ) =
    KarafFeature(name, version, description, bundles(version), deps)

  def stdLib = feature(
    "scala-std-lib",
    "3.2.2",
    "Scala3 :: Standard Library",
    v => Seq(b.scala213Lib, b.scala322Lib)
  )

  def cats =
    feature("cats", "2.9.0", "Cats", b.cats, deps = stdLib)

  def catsEffect =
    feature("cats-effect", "3.5.0", "Cats Effect", b.catsEffect, cats)

  def scribe = 
    feature("scribe", "3.11.1", "Scribe", b.scribe)

  def fs2      =
    feature(
      "fs2",
      "3.7.0",
      "FS2",
      v =>
        b.fs2(v) ++ Seq(
          b.scodec("1.1.37"),
          b.ip4s("3.3.0"),
          b.literally("1.1.0")
        ),
      catsEffect
    )
  def log4cats =
    feature(
      "log4cats",
      "2.6.0",
      "Log4Cats",
      b.log4cats,
      catsEffect
    )

  def catsParse =
    feature(
      "cats-parse",
      "0.3.9",
      "Cats :: Parse",
      b.catsParse andThen (Seq(_)),
      cats
    )

  def http4s =
    feature(
      "http4s",
      "0.23.19",
      "Http4s",
      v =>
        b.http4s(v) ++ Seq(
          b.hpack("1.0.2"),
          b.http4sCrypto("0.2.4"),
          b.vault("3.5.0"),
          b.caseInsensitive("1.3.0"),
          b.keypool("0.4.8")
        ),
      fs2,
      log4cats,
      catsParse
    )
  def repo   = KarafFeatureRepository(
    "scala-libs",
    stdLib,
    cats,
    catsEffect,
    fs2,
    http4s,
    catsParse,
    log4cats,
    // scribe
  )

end features

import bundles.*

println(features.repo.xmlTags.render)
