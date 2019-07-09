package backend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.{ConnectionPoolSettings, ParserSettings, RoutingSettings}
import akka.http.scaladsl.server.Route
import scala.concurrent.Future
import akka.stream.Materializer
import akka.http.scaladsl.server.RoutingLog
import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.ExceptionHandler
import monix.eval.Task

trait AkkaMonixHttpClientV2 {
  def singleRequest(request: HttpRequest, settings: ConnectionPoolSettings): Task[HttpResponse]
}

object AkkaMonixHttpClientV2 {
  def default(system: ActorSystem,
              connectionContext: Option[HttpsConnectionContext],
              customLog: Option[LoggingAdapter]): AkkaMonixHttpClientV2 = new AkkaMonixHttpClientV2 {

    private val http = Http()(system)

    override def singleRequest(request: HttpRequest, settings: ConnectionPoolSettings): Task[HttpResponse] = Task.deferFuture {
      http.singleRequest(
        request,
        connectionContext.getOrElse(http.defaultClientHttpsContext),
        settings,
        customLog.getOrElse(system.log)
      )
    }
  }

  def stubFromAsyncHandler(run: HttpRequest => Future[HttpResponse]): AkkaMonixHttpClientV2 =
    (request: HttpRequest, _: ConnectionPoolSettings) => Task.deferFuture(run(request))


  def stubFromRoute(route: Route)(
    implicit routingSettings: RoutingSettings,
    parserSettings: ParserSettings,
    materializer: Materializer,
    routingLog: RoutingLog,
    executionContext: ExecutionContextExecutor = null,
    rejectionHandler: RejectionHandler = RejectionHandler.default,
    exceptionHandler: ExceptionHandler = null
  ): AkkaMonixHttpClientV2 = stubFromAsyncHandler(Route.asyncHandler(route))
}