# osgi-scala

[Apache Karaf](https://karaf.apache.org) [feature repository](https://karaf.apache.org/manual/latest/#_features_repositories_2) for popular Scala libraries.

It is also used in [Http4s whiteboard](https://github.com/p-pavel/http4s-whiteboard) project.

Feel free to share your ideas and discuss features at [gitter](https://app.element.io/#/room/#http4s-whiteboard:gitter.im)

**BTW: as of November 2023 I'm actively looking for the Scala job. Please contact me if you have any proposals.**

Also, you can 

<a href="https://www.buymeacoffee.com/perikov" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-blue.png" alt="Buy Me A Beer" style="height: 60px !important;width: 217px !important;" ></a>


## Motivation

Scala is a fantastic rogramming language. OSGi is a fantastic specification. Karaf is a fantastic container.

The problem is: Scala's libraries aren't often provide bundle headers.

I wrapped some well-known libraries using `wrap:` URL handler.

## Running

```sh
karaf
karaf@root()> feature:repo-add https://raw.githubusercontent.com/p-pavel/osgi-scala/main/scala-libs.xml # Scala libraries
karaf@root()> feature:install http4s
karaf@root()> feature:list -i
```

## Future work

- participate in providing bundle headers for the original libraries using [sbt-osgi plugin](https://github.com/sbt/sbt-osgi)
- provide more libraries
- provide Scala 3 versions (contributors welcome, I don't use Scala 2)
- automate version updating
