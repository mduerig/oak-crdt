# Conflict handling with Oak Demo

This is the demo code for the presentation [Conflict handling with Oak](https://www.slideshare.net/secret/2C1upVJ5j8bMBd).

Build with [Maven 3](http://maven.apache.org/) and Java 7 (or higher):

    mvn clean install

The test cases give a good indication on how the individual conflict handlers
are intended to work.

Alternatively there is an interactive Scala shell, which can be used for experimenting
with a transient repository including all conflict handlers from this project (Atomic
counter, Last writer wins, multi value register and atomic set). To start the shell type:

    java -jar target/oak-crdt-*-jar-with-dependencies.jar

The shell is based on the [Ammonite REPL](https://lihaoyi.github.io/Ammonite/) and
has all Oak dependencies embedded. Some useful predefined bindings can be imported
from `michid.crdt.Demo`:

    @ import michid.crdt.Demo._
    import michid.crdt.Demo._

The predefined `repository` instance is then available:

    @ repository.getDescriptorKeys
    res0: Array[String] = Array(
      "jcr.repository.name",
      "option.versioning.supported",
      ...

The `newSession` binding provides a convenient shortcut for opening new sessions
on that repository:

    @ val s = newSession
    s: javax.jcr.Session = session-8
    @ val r = s.getRootNode
    r: javax.jcr.Node = Node[NodeDelegate{tree=/: { jcr:primaryType = rep:root, mv = { ... }, set = { ... }, oak:index = { ... }, jcr:system = { ... }, count = { ... }, ...}}]

See the demo scripts in `src/main/resources` for further examples.

## Links
* [Conflict handling with Oak](https://www.slideshare.net/secret/2C1upVJ5j8bMBd)
* [Conflict-free Replicated Data Types](https://hal.inria.fr/file/index/docid/617341/filename/RR-7687.pdf)
* [Strong Eventual Consistency and Conflict-free Replicated Data Types](http://research.microsoft.com/apps/video/dl.aspx?id=153540)
* [Apache Jackrabbit Oak](http://jackrabbit.apache.org/oak/)
