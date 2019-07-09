package backend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.HttpsConnectionContext
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.softwaremill.sttp._
import com.softwaremill.sttp.impl.monix.TaskMonadAsyncError
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable

class AkkaMonixHttpBackendV2(actorSystem: ActorSystem,
                                      ec: Scheduler,
                                      terminateActorSystemOnClose: Boolean,
                                      options: SttpBackendOptions,
                                      client: AkkaMonixHttpClientV2,
                                      customConnectionPoolSettings: Option[ConnectionPoolSettings]
                            ) extends SttpBackend[Task, Observable[ByteString]] {

  /* Initiates the Akka Actor system. */
  implicit val system: ActorSystem = actorSystem
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override def send[T](sttpRequest: Request[T, Observable[ByteString]]): Task[Response[T]] = {
    implicit val ec: Scheduler = this.ec

    val settingsWithProxy = ProxySettings.includeProxy(
      ProxySettings.connectionSettings(actorSystem, options, customConnectionPoolSettings), options
    )
    val updatedSettings = settingsWithProxy.withUpdatedConnectionSettings(_.withIdleTimeout(sttpRequest.options.readTimeout))
    val requestMethod   = Akka.toAkkaMethod(sttpRequest.method)
    val partialRequest  = HttpRequest(uri = sttpRequest.uri.toString, method = requestMethod)

    val request = for {
      headers <- Akka.toAkkaHeaders(sttpRequest.headers)
      body    <- Akka.toAkkaRequestBody(sttpRequest.body, sttpRequest.headers, partialRequest)
    } yield body.withHeaders(headers)

    Task.fromEither(request)
      .flatMap(req  => client.singleRequest(req, updatedSettings))
      .flatMap(resp => Sttp.toSttpResponse(resp, sttpRequest))
  }

  def responseMonad: MonadError[Task] = TaskMonadAsyncError

  override def close(): Unit = if (terminateActorSystemOnClose) { actorSystem.terminate() }
}

object AkkaMonixHttpBackendV2 {

  /* Creates a new Actor system and Akka-HTTP client by default. */
  def apply(options: SttpBackendOptions = SttpBackendOptions.Default,
            customHttpsContext: Option[HttpsConnectionContext] = None,
            customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
            customLog: Option[LoggingAdapter] = None)
           (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString]] = {

    val akkaMonixHttpBackendV2 = new AkkaMonixHttpBackendV2(
      ActorSystem("sttp"),
      ec,
      terminateActorSystemOnClose = false,
      options,
      AkkaMonixHttpClientV2.default(ActorSystem("sttp"), customHttpsContext, customLog),
      customConnectionPoolSettings)

    new FollowRedirectsBackend(akkaMonixHttpBackendV2)
  }

  /* This constructor allows for a specified Actor system. */
  def usingActorSystem(actorSystem: ActorSystem,
                       options: SttpBackendOptions = SttpBackendOptions.Default,
                       customHttpsContext: Option[HttpsConnectionContext] = None,
                       customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
                       customLog: Option[LoggingAdapter] = None)
                      (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString]] = {

    usingClient(actorSystem, options, customConnectionPoolSettings,
      AkkaMonixHttpClientV2.default(actorSystem, customHttpsContext, customLog))
  }

  /* This constructor allows for a specified Actor system. */
  def usingClient(actorSystem: ActorSystem,
                  options: SttpBackendOptions = SttpBackendOptions.Default,
                  customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
                  client: AkkaMonixHttpClientV2)
                 (implicit ec: Scheduler = monix.execution.Scheduler.global): SttpBackend[Task, Observable[ByteString]] = {

    val akkaMonixHttpBackendV2 = new AkkaMonixHttpBackendV2(
      actorSystem,
      ec,
      terminateActorSystemOnClose = false,
      options,
      client,
      customConnectionPoolSettings)

    new FollowRedirectsBackend(akkaMonixHttpBackendV2)
  }
}
