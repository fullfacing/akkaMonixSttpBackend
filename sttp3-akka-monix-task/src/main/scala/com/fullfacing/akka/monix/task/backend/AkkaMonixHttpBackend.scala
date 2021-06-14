package com.fullfacing.akka.monix.task.backend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.Materializer
import akka.util.ByteString
import com.fullfacing.akka.monix.core.{ConvertToAkka, ProxySettings}
import com.fullfacing.akka.monix.task.backend.utils.{BodyFromAkka, ConvertToSttp, TaskMonadAsyncError}
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import sttp.capabilities
import sttp.client3.{FollowRedirectsBackend, Request, Response, SttpBackend, SttpBackendOptions}
import sttp.monad.MonadError

import scala.concurrent.Future

class AkkaMonixHttpBackend(actorSystem: ActorSystem,
                           ec: Scheduler,
                           terminateActorSystemOnClose: Boolean,
                           options: SttpBackendOptions,
                           client: AkkaMonixHttpClient,
                           customConnectionPoolSettings: Option[ConnectionPoolSettings])
  extends SttpBackend[Task, Observable[ByteString]] {

  /* Initiates the Akka Actor system. */
  implicit val system: ActorSystem = actorSystem

  private lazy val bodyFromAkka = new BodyFromAkka()(ec, implicitly[Materializer], responseMonad)

  def send[T, R >: Observable[ByteString] with capabilities.Effect[Task]](sttpRequest: Request[T, R]): Task[Response[T]] = {
    implicit val sch: Scheduler = ec

    val settingsWithProxy = ProxySettings.includeProxy(
      ProxySettings.connectionSettings(actorSystem, options, customConnectionPoolSettings), options
    )
    val updatedSettings = settingsWithProxy.withUpdatedConnectionSettings(_.withIdleTimeout(sttpRequest.options.readTimeout))
    val requestMethod   = ConvertToAkka.toAkkaMethod(sttpRequest.method)
    val partialRequest  = HttpRequest(uri = sttpRequest.uri.toString, method = requestMethod)

    val request = for {
      headers <- ConvertToAkka.toAkkaHeaders(sttpRequest.headers.toList)
      body    <- ConvertToAkka.toAkkaRequestBody(sttpRequest.body, sttpRequest.headers, partialRequest)
    } yield body.withHeaders(headers)

    Task.fromEither(request)
      .flatMap(client.singleRequest(_, updatedSettings))
      .flatMap(ConvertToSttp.toSttpResponse[T, R](_, sttpRequest)(bodyFromAkka))
  }

  def responseMonad: MonadError[Task] = TaskMonadAsyncError

  override def close(): Task[Unit] = Task.deferFuture {
    if (terminateActorSystemOnClose) actorSystem.terminate else Future.unit
  }.void
}

object AkkaMonixHttpBackend {

  /* Creates a new Actor system and Akka-HTTP client by default. */
  def apply(options: SttpBackendOptions = SttpBackendOptions.Default,
            customHttpsContext: Option[HttpsConnectionContext] = None,
            customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
            customLog: Option[LoggingAdapter] = None)
           (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString]] = {

    val actorSystem = ActorSystem("sttp")
    val client = AkkaMonixHttpClient.default(actorSystem, customHttpsContext, customLog)

    val akkaMonixHttpBackend = new AkkaMonixHttpBackend(
      actorSystem                  = actorSystem,
      ec                           = ec,
      terminateActorSystemOnClose  = false,
      options                      = options,
      client                       = client,
      customConnectionPoolSettings = customConnectionPoolSettings
    )

    new FollowRedirectsBackend[Task, Observable[ByteString]](akkaMonixHttpBackend)
  }

  /* This constructor allows for a specified Actor system. */
  def usingActorSystem(actorSystem: ActorSystem,
                       options: SttpBackendOptions = SttpBackendOptions.Default,
                       customHttpsContext: Option[HttpsConnectionContext] = None,
                       customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
                       customLog: Option[LoggingAdapter] = None)
                      (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString]] = {
    val client = AkkaMonixHttpClient.default(actorSystem, customHttpsContext, customLog)
    usingClient(actorSystem, options, customConnectionPoolSettings, client)
  }

  /* This constructor allows for a specified Actor system. */
  def usingClient(actorSystem: ActorSystem,
                  options: SttpBackendOptions = SttpBackendOptions.Default,
                  poolSettings: Option[ConnectionPoolSettings] = None,
                  client: AkkaMonixHttpClient)
                 (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString]] = {

    val akkaMonixHttpBackend = new AkkaMonixHttpBackend(
      actorSystem                  = actorSystem,
      ec                           = ec,
      terminateActorSystemOnClose  = false,
      options                      = options,
      client                       = client,
      customConnectionPoolSettings = poolSettings
    )

    new FollowRedirectsBackend[Task, Observable[ByteString]](akkaMonixHttpBackend)
  }
}
