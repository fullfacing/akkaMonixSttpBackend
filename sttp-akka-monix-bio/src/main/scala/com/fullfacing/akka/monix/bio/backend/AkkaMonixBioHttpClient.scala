package com.fullfacing.akka.monix.bio.backend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, RoutingLog}
import akka.http.scaladsl.settings.{ConnectionPoolSettings, ParserSettings, RoutingSettings}
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.Materializer
import monix.bio.Task

import scala.concurrent.{ExecutionContextExecutor, Future}

trait AkkaMonixBioHttpClient {
  def singleRequest(request: HttpRequest, settings: ConnectionPoolSettings): Task[HttpResponse]
}

object AkkaMonixBioHttpClient {
  def default(system: ActorSystem,
              connectionContext: Option[HttpsConnectionContext],
              customLog: Option[LoggingAdapter]): AkkaMonixBioHttpClient = new AkkaMonixBioHttpClient {

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

  def stubFromAsyncHandler(run: HttpRequest => Future[HttpResponse]): AkkaMonixBioHttpClient =
    (request: HttpRequest, _: ConnectionPoolSettings) => Task.deferFuture(run(request))


  def stubFromRoute(route: Route)(
    implicit routingSettings: RoutingSettings,
    parserSettings: ParserSettings,
    materializer: Materializer,
    routingLog: RoutingLog,
    executionContext: ExecutionContextExecutor = null,
    rejectionHandler: RejectionHandler = RejectionHandler.default,
    exceptionHandler: ExceptionHandler = null
  ): AkkaMonixBioHttpClient = stubFromAsyncHandler(Route.asyncHandler(route))
}