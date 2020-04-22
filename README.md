[![CircleCI](https://circleci.com/gh/fullfacing/akkaMonixSttpBackend.svg?style=shield&circle-token=2547983c39c2197e6663282e9ae20f77eb97e03b)](https://circleci.com/gh/fullfacing/akkaMonixSttpBackend)
[![Maven Central](https://img.shields.io/maven-central/v/com.fullfacing/sttp-akka-monix-task_2.13.svg)](https://search.maven.org/search?q=a:sttp-akka-monix-task_2.13)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

# akkaMonixSttpBackend
**Introduction:**<br/>
akkaMonixSttpBackend is a backend for [sttp](https://sttp.readthedocs.io/en/latest/index.html) using [Akka-HTTP](https://doc.akka.io/docs/akka-http/current/index.html) to handle requests, and with [Task](https://monix.io/docs/3x/eval/task.html) as the response Monad and [Observable](https://monix.io/docs/3x/reactive/observable.html) as the streaming type. It is a modification of the [Akka-HTTP backend](https://sttp.readthedocs.io/en/latest/backends/akkahttp.html) provided by sttp, with instances of Futures deferred to Tasks and Akka-Streams Sources converted to Observables.

The motivation behind creating this backend as opposed to using the existing [Monix wrapped async-http-client backend](https://sttp.readthedocs.io/en/latest/backends/asynchttpclient.html) is to give an alternative for projects that already have Akka-HTTP as a dependency, removing the need for the async-http-client dependency as well.

**Installation:**<br/> Add the following sbt dependency:<br/>
`"com.fullfacing" %% "sttp-akka-monix-task" % "1.5.0"`<br/>

**Usage:**<br/>
Usage is identical to the [Akka-HTTP backend](https://sttp.readthedocs.io/en/latest/backends/akkahttp.html) with only the response type differing:
```scala
import akka.util.ByteString
import com.fullfacing.akka.monix.task.backend.AkkaMonixHttpBackend
import sttp.client.{Response, SttpBackend, _}
import monix.eval.Task
import monix.reactive.Observable

implicit val backend: SttpBackend[Task, Observable[ByteString], NothingT] = AkkaMonixHttpBackend()

// To set the request body as a stream:
val observable: Observable[ByteString] = ???

sttp
  .streamBody(observable)
  .post(uri"...")
  .send()

// To receive the response body as a stream:
val response: Task[Response[Observable[ByteString]]] =
  sttp
    .post(uri"...")
    .response(asStream[Observable[ByteString]])
    .send()
```
