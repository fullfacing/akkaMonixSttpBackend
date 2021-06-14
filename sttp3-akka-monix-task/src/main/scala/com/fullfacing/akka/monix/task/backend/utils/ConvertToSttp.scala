package com.fullfacing.akka.monix.task.backend.utils

import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer
import akka.util.ByteString
import com.fullfacing.akka.monix.core._
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import sttp.client3.{Request, Response}
import sttp.model.{Header, HeaderNames, ResponseMetadata, StatusCode}

import scala.collection.immutable.Seq

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
  def toSttpResponse[T, R >: Observable[ByteString]](response: HttpResponse,
                                                     sttpRequest: Request[T, R])
                                                    (bodyFromAkka: BodyFromAkka)
                                                    (implicit scheduler: Scheduler, mat: Materializer): Task[Response[T]] = {
    val statusCode   = StatusCode(response.status.intValue())
    val statusText   = response.status.reason()
    val respHeaders  = toSttpHeaders(response)
    val respMetadata = ResponseMetadata(statusCode, statusText, respHeaders)
    val decodedResp  = decodeAkkaResponse(response)

    val body = bodyFromAkka(sttpRequest.response, respMetadata, Left(decodedResp))
    body.map(t => Response(t, statusCode, statusText, respHeaders))
  }
}
