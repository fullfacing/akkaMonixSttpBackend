package backend

import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer
import akka.util.ByteString
import com.softwaremill.sttp._
import cats.implicits._
import options._
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import scala.collection.immutable.Seq
import scala.concurrent.Future

object Sttp {

  /* Converts Akka-HTTP headers to the STTP equivalent. */
  def toSttpHeaders(response: HttpResponse): Seq[(String, String)] = {
    val headCont   = HeaderNames.ContentType -> response.entity.contentType.toString()
    val contLength = response.entity.contentLengthOption.map(HeaderNames.ContentLength -> _.toString)
    val other      = response.headers.map(h => (h.name, h.value))
    headCont :: (contLength.toList ++ other)
  }

  /* Converts an Akka-HTTP response to a STTP equivalent. */
  def toSttpResponse[T](response: HttpResponse, sttpRequest: Request[T, Observable[ByteString]])
                         (implicit scheduler: Scheduler, mat: Materializer): Task[Response[T]] = {
    val statusCode   = response.status.intValue()
    val statusText   = response.status.reason()
    val respHeaders  = toSttpHeaders(response)
    val respMetadata = ResponseMetadata(respHeaders, statusCode, statusText)
    val decodedResp  = decodeAkkaResponse(response)

    val respCharset  = respHeaders.collectFirst {
      case (HeaderNames.ContentType, value) => encodingFromContentType(value)
    }.flatten

    val body = if (sttpRequest.options.parseResponseIf(respMetadata)) {
      toSttpResponseBody(sttpRequest.response, decodedResp, respCharset, respMetadata).map(_.asRight)
    } else {
      toSttpResponseBody(asByteArray, decodedResp, respCharset, respMetadata).map(_.asLeft)
    }

    body.map( t => Response(t, statusCode, statusText, respHeaders, Nil))
  }

  /* Converts an Akka response body to a STTP equivalent. */
  def toSttpResponseBody[T](sttpBody: ResponseAs[T, Observable[ByteString]],
                                     resp: HttpResponse,
                                     charset: Option[String],
                                     metadata: ResponseMetadata)
                             (implicit scheduler: Scheduler, mat: Materializer): Task[T] = {

    type R[A, B] = BasicResponseAs[A, B]

    def processBody(body: ResponseAs[T, Observable[ByteString]]): Future[T] = body match {
      case MappedResponseAs(raw: R[T, Observable[ByteString]], f) => processBody(raw).map(f(_, metadata))
      case IgnoreResponse                                         => discardEntity(resp.entity)
      case ResponseAsString(enc)                                  => entityToString(resp.entity, charset, enc)
      case ResponseAsByteArray                                    => entityToByteArray(resp.entity)
      case r @ ResponseAsStream()                                 => entityToObservable[T](resp.entity, r)
      case ResponseAsFile(file, overwrite)                        => entityToFile(resp.entity, file.toFile, overwrite).map(_ => file)
    }

    Task.deferFuture(processBody(sttpBody))
  }

}