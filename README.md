[![CircleCI](https://circleci.com/gh/fullfacing/akkaMonixSttpBackend.svg?style=shield&circle-token=2547983c39c2197e6663282e9ae20f77eb97e03b)](https://circleci.com/gh/fullfacing/akkaMonixSttpBackend)
[![Maven Central](https://img.shields.io/maven-central/v/com.fullfacing/sttp-akka-monix_2.12.svg)](https://search.maven.org/search?q=a:sttp-akka-monix_2.12)

# akkaMonixSttpBackend
**Introduction:**<br/>
akkaMonixSttpBackend is a backend for [sttp](https://sttp.readthedocs.io/en/latest/index.html) using [Akka-HTTP](https://doc.akka.io/docs/akka-http/current/index.html) to handle requests, and with [Task](https://monix.io/docs/3x/eval/task.html) as the response Monad and [Observable](https://monix.io/docs/3x/reactive/observable.html) as the streaming type. It is a modification of the [Akka-HTTP backend](https://sttp.readthedocs.io/en/latest/backends/akkahttp.html) provided by sttp, with instances of Futures deferred to Tasks and Akka-Streams Sources converted to Observables.

The motivation behind creating this backend as opposed to using the existing [Monix wrapped async-http-client backend](https://sttp.readthedocs.io/en/latest/backends/asynchttpclient.html) is to give an alternative for projects that already have Akka-HTTP as a dependency, removing the need for the async-http-client dependency as well.

**Installation:**<br/>
Add the following sbt dependency:<br/>
`"com.fullfacing" %% "sttp-akka-monix" % "1.0.1"`

**Usage:**<br/>
Usage is identical to the [Akka-HTTP backend](https://sttp.readthedocs.io/en/latest/backends/akkahttp.html) with only the response type differing:
```scala
import akka.util.ByteString
import com.fullfacing.backend.AkkaMonixHttpBackend
import com.softwaremill.sttp.{Response, SttpBackend, _}
import monix.eval.Task
import monix.reactive.Observable

implicit val backend: SttpBackend[Task, Observable[ByteString]] = AkkaMonixHttpBackend()

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
