package com.fullfacing.akka.monix.core

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import cats.implicits._
import monix.execution.Scheduler
import monix.reactive.Observable
import sttp.client3.{ByteArrayBody, ByteBufferBody, FileBody, InputStreamBody, MultipartBody, NoBody, RequestBody, StreamBody, StringBody}
import sttp.model.{Header, Method}

import scala.collection.immutable

object ConvertToAkka {

  /* Converts a HTTP method from STTP to the Akka-HTTP equivalent. */
  def toAkkaMethod(method: Method): HttpMethod = method match {
    case Method.GET     => HttpMethods.GET
    case Method.HEAD    => HttpMethods.HEAD
    case Method.POST    => HttpMethods.POST
    case Method.PUT     => HttpMethods.PUT
    case Method.DELETE  => HttpMethods.DELETE
    case Method.OPTIONS => HttpMethods.OPTIONS
    case Method.PATCH   => HttpMethods.PATCH
    case Method.CONNECT => HttpMethods.CONNECT
    case Method.TRACE   => HttpMethods.TRACE
    case _              => HttpMethod.custom(method.method)
  }

  /* Converts STTP headers to Akka-HTTP equivalents. */
  def toAkkaHeaders(headers: List[Header]): Either[Throwable, immutable.Seq[HttpHeader]] = {
    val parsingResults = headers.collect {
      case Header(n, v) if !isContentType(n) && !isContentLength(n) => HttpHeader.parse(n, v)
    }

    val errors = parsingResults.collect { case ParsingResult.Error(e) => e }

    if (errors.isEmpty) {
      parsingResults.collect { case ParsingResult.Ok(httpHeader, _) => httpHeader }.asRight
    } else {
      new RuntimeException(s"Cannot parse headers: $errors").asLeft
    }
  }

  /* Converts a STTP request body into an Akka-HTTP request with the same body. */
  def toAkkaRequestBody[R](body: RequestBody[R], headers: Seq[Header], request: HttpRequest)
                          (implicit scheduler: Scheduler): Either[Throwable, HttpRequest] = {

    val header = headers.collectFirst { case Header(k, v) if isContentType(k) => v }

    createContentType(header).flatMap { ct =>
      body match {
        case NoBody                => request.asRight
        case StringBody(b, enc, _) => request.withEntity(contentTypeWithEncoding(ct, enc), b.getBytes(enc)).asRight
        case ByteArrayBody(b, _)   => request.withEntity(HttpEntity(ct, b)).asRight
        case ByteBufferBody(b, _)  => request.withEntity(HttpEntity(ct, ByteString(b))).asRight
        case InputStreamBody(b, _) => request.withEntity(HttpEntity(ct, StreamConverters.fromInputStream(() => b))).asRight
        case FileBody(b, _)        => request.withEntity(ct, b.toPath).asRight
        case StreamBody(s)         => request.withEntity(HttpEntity(ct, Source.fromPublisher(s.asInstanceOf[Observable[ByteString]].toReactivePublisher))).asRight
        case MultipartBody(ps)     => createMultiPartRequest(ps, request)
      }
    }
  }

}
