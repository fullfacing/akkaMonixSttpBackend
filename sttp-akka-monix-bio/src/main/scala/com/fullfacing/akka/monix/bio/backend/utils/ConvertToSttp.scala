package com.fullfacing.akka.monix.bio.backend.utils

import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer
import akka.util.ByteString
import com.fullfacing.akka.monix.core._
import monix.bio.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import sttp.client.{IgnoreResponse, MappedResponseAs, Request, Response, ResponseAs, ResponseAsByteArray, ResponseAsFile, ResponseAsFromMetadata, ResponseAsStream, ResponseMetadata}
import sttp.model.{Header, HeaderNames, StatusCode}

import scala.collection.immutable.Seq
import scala.concurrent.Future

object ConvertToSttp {

  /* Converts Akka-HTTP headers to the STTP equivalent. */
  def toSttpHeaders(response: HttpResponse): Seq[Header] = {
    val headCont   = HeaderNames.ContentType -> response.entity.contentType.toString()
    val contLength = response.entity.contentLengthOption.map(HeaderNames.ContentLength -> _.toString)
    val other      = response.headers.map(h => (h.name, h.value))
    val headerMap  = headCont :: (contLength.toList ++ other)

    headerMap.flatMap { case (k, v) => Header.safeApply(k, v).toOption }
  }

  /* Converts an Akka-HTTP response to a STTP equivalent. */
  def toSttpResponse[T](response: HttpResponse, sttpRequest: Request[T, Observable[ByteString]])
                       (implicit scheduler: Scheduler, mat: Materializer): Task[Response[T]] = {
    val statusCode   = StatusCode.notValidated(response.status.intValue())
    val statusText   = response.status.reason()
    val respHeaders  = toSttpHeaders(response)
    val respMetadata = ResponseMetadata(respHeaders, statusCode, statusText)
    val decodedResp  = decodeAkkaResponse(response)

    val body = toSttpResponseBody(sttpRequest.response, decodedResp, respMetadata)
    body.map(t => Response(t, statusCode, statusText, respHeaders, Nil))
  }

  /* Converts an Akka response body to a STTP equivalent. */
  def toSttpResponseBody[T](sttpBody: ResponseAs[T, Observable[ByteString]],
                            resp: HttpResponse,
                            metadata: ResponseMetadata)
                           (implicit scheduler: Scheduler, mat: Materializer): Task[T] = {

    type R[A] = ResponseAs[A, Observable[ByteString]]

    def processBody(body: ResponseAs[T, Observable[ByteString]]): Future[T] = body match {
      case MappedResponseAs(raw: R[T], f) => processBody(raw).map(f(_, metadata))
      case ResponseAsFromMetadata(f)      => processBody(f(metadata))
      case IgnoreResponse                 => discardEntity(resp.entity)
      case ResponseAsByteArray            => entityToByteArray(resp.entity)
      case r @ ResponseAsStream()         => Future.successful(r.responseIsStream(entityToObservable(resp.entity)))
      case ResponseAsFile(file)           => entityToFile(resp.entity, file.toFile).map(_ => file)
    }

    Task.deferFuture(processBody(sttpBody))
  }
}
