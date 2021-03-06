package com.fullfacing.akka.monix.bio.backend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.util.ByteString
import com.fullfacing.akka.monix.bio.backend.utils.{BioMonadAsyncError, ConvertToSttp}
import com.fullfacing.akka.monix.core.{ConvertToAkka, ProxySettings}
import monix.bio.{IO, Task}
import monix.execution.Scheduler
import monix.reactive.Observable
import sttp.client.monad.MonadError
import sttp.client.ws.WebSocketResponse
import sttp.client.{FollowRedirectsBackend, NothingT, Request, Response, SttpBackend, SttpBackendOptions}

import scala.concurrent.Future

class AkkaMonixBioHttpBackend(actorSystem: ActorSystem,
                              ec: Scheduler,
                              terminateActorSystemOnClose: Boolean,
                              options: SttpBackendOptions,
                              client: AkkaMonixBioHttpClient,
                              customConnectionPoolSettings: Option[ConnectionPoolSettings])
  extends SttpBackend[Task, Observable[ByteString], NothingT] {

  /* Initiates the Akka Actor system. */
  implicit val system: ActorSystem = actorSystem

  override def send[T](sttpRequest: Request[T, Observable[ByteString]]): Task[Response[T]] = {
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

    IO.fromEither(request)
      .flatMap(client.singleRequest(_, updatedSettings))
      .flatMap(ConvertToSttp.toSttpResponse(_, sttpRequest))
  }

  def responseMonad: MonadError[Task] = BioMonadAsyncError

  override def close(): Task[Unit] = Task.deferFuture {
    if (terminateActorSystemOnClose) actorSystem.terminate else Future.unit
  }.void

  override def openWebsocket[T, WS_RESULT](request: Request[T, Observable[ByteString]],
                                           handler: NothingT[WS_RESULT]): Task[WebSocketResponse[WS_RESULT]] = handler
}

object AkkaMonixBioHttpBackend {

  /* Creates a new Actor system and Akka-HTTP client by default. */
  def apply(options: SttpBackendOptions = SttpBackendOptions.Default,
            customHttpsContext: Option[HttpsConnectionContext] = None,
            customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
            customLog: Option[LoggingAdapter] = None)
           (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString], NothingT] = {

    val actorSystem = ActorSystem("sttp")
    val client = AkkaMonixBioHttpClient.default(actorSystem, customHttpsContext, customLog)

    val akkaMonixHttpBackend = new AkkaMonixBioHttpBackend(
      actorSystem                  = actorSystem,
      ec                           = ec,
      terminateActorSystemOnClose  = false,
      options                      = options,
      client                       = client,
      customConnectionPoolSettings = customConnectionPoolSettings
    )

    new FollowRedirectsBackend[Task, Observable[ByteString], NothingT](akkaMonixHttpBackend)
  }

  /* This constructor allows for a specified Actor system. */
  def usingActorSystem(actorSystem: ActorSystem,
                       options: SttpBackendOptions = SttpBackendOptions.Default,
                       customHttpsContext: Option[HttpsConnectionContext] = None,
                       customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
                       customLog: Option[LoggingAdapter] = None)
                      (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString], NothingT] = {
    val client = AkkaMonixBioHttpClient.default(actorSystem, customHttpsContext, customLog)
    usingClient(actorSystem, options, customConnectionPoolSettings, client)
  }

  /* This constructor allows for a specified Actor system. */
  def usingClient(actorSystem: ActorSystem,
                  options: SttpBackendOptions = SttpBackendOptions.Default,
                  poolSettings: Option[ConnectionPoolSettings] = None,
                  client: AkkaMonixBioHttpClient)
                 (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString], NothingT] = {

    val akkaMonixHttpBackend = new AkkaMonixBioHttpBackend(
      actorSystem                  = actorSystem,
      ec                           = ec,
      terminateActorSystemOnClose  = false,
      options                      = options,
      client                       = client,
      customConnectionPoolSettings = poolSettings
    )

    new FollowRedirectsBackend[Task, Observable[ByteString], NothingT](akkaMonixHttpBackend)
  }
}
