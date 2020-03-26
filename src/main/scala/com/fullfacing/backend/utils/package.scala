package com.fullfacing.backend

import java.io.{File, UnsupportedEncodingException}
import java.nio.ByteBuffer

import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.ContentTypes.`application/octet-stream`
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{HttpEncodings, `Content-Length`, `Content-Type`}
import akka.stream.scaladsl.{FileIO, Sink, StreamConverters}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import cats.implicits._
import monix.execution.Scheduler
import monix.reactive.Observable
import sttp.client.{BasicRequestBody, ByteArrayBody, ByteBufferBody, FileBody, InputStreamBody, StringBody}
import sttp.model.Part

import scala.concurrent.Future

package object utils {

  /* Converts an Akka-HTTP response entity into a byte array. */
  def entityToByteArray(entity: ResponseEntity)(implicit scheduler: Scheduler, mat: Materializer): Future[Array[Byte]] = {
    entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.toArray[Byte])
  }

  /* Converts an Akka-HTTP response entity into a String. */
  def entityToString(entity: ResponseEntity, charset: Option[String], encoding: String)
                    (implicit scheduler: Scheduler, mat: Materializer): Future[String] = {
    entityToByteArray(entity).map { byteArray =>
      new String(byteArray, charset.getOrElse(encoding))
    }
  }

  /* Converts an Akka-HTTP response entity into a file. */
  def entityToFile(entity: ResponseEntity, file: File)(implicit mat: Materializer): Future[IOResult] = {
    if (!file.exists())
      file.getParentFile.mkdirs() & file.createNewFile()

    entity.dataBytes.runWith(FileIO.toPath(file.toPath))
  }

  /* Converts an Akka-HTTP response entity into an Observable. */
  def entityToObservable(entity: ResponseEntity)(implicit mat: Materializer): Observable[ByteString] = {
    Observable.fromReactivePublisher(entity.dataBytes.runWith(Sink.asPublisher[ByteString](fanout = false)))
  }

  /* Discards an Akka-HTTP response entity. */
  def discardEntity(entity: ResponseEntity)(implicit mat: Materializer, scheduler: Scheduler): Future[Unit] = {
    entity.dataBytes.runWith(Sink.ignore).map(_ => ())
  }

  /* Parses the headers to a Content Type. */
  def createContentType(header: Option[String]): Either[Throwable, ContentType] = {
    header.map { contentType =>
      ContentType.parse(contentType).leftMap { errors =>
        new RuntimeException(s"Cannot parse content type: $errors")
      }
    }.getOrElse(`application/octet-stream`.asRight)
  }

  /* Creates a MultiPart Request. */
  def createMultiPartRequest(mps: Seq[Part[BasicRequestBody]], request: HttpRequest): Either[Throwable, HttpRequest] = {
    mps.map(convertMultiPart).toList.sequence.map { bodyPart =>
      FormData()
      request.withEntity(FormData(bodyPart: _*).toEntity())
    }
  }

  /* Converts a MultiPart to a MultiPartFormData Type. */
  def convertMultiPart(mp: Part[BasicRequestBody]): Either[Throwable, FormData.BodyPart] = {
    for {
      ct      <- createContentType(mp.contentType)
      headers <- ConvertToAkka.toAkkaHeaders(mp.headers.toList)
    } yield {
      val fileName  = mp.fileName.fold(Map.empty[String, String])(fn => Map("filename" -> fn))
      val bodyPart  = createBodyPartEntity(ct, mp.body)
      FormData.BodyPart(mp.name, bodyPart, fileName, headers)
    }
  }

  /* Creates a BodyPart entity. */
  def createBodyPartEntity(ct: ContentType, body: BasicRequestBody): BodyPartEntity = body match {
    case StringBody(b, encoding, _) => HttpEntity(contentTypeWithEncoding(ct, encoding), b.getBytes(encoding))
    case ByteArrayBody(b, _)        => HttpEntity(ct, b)
    case ByteBufferBody(b, _)       => HttpEntity(ct, ByteString(b))
    case isb: InputStreamBody       => HttpEntity.IndefiniteLength(ct, StreamConverters.fromInputStream(() => isb.b))
    case FileBody(b, _)             => HttpEntity.fromPath(ct, b.toPath)
  }

  /* Decodes an Akka-HTTP response. */
  def decodeAkkaResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip     => Gzip
      case HttpEncodings.deflate  => Deflate
      case HttpEncodings.identity => NoCoding
      case ce                     => throw new UnsupportedEncodingException(s"Unsupported encoding: $ce")
    }
    decoder.decodeMessage(response)
  }

  /* Sets the Content Type encoding. */
  def contentTypeWithEncoding(ct: String, enc: String): String =
    s"$ct; charset=$enc"

  /* Gets the encoding from the Content Type. */
  def encodingFromContentType(ct: String): Option[String] =
    ct.split(";").map(_.trim.toLowerCase).collectFirst {
      case s if s.startsWith("charset=") && s.substring(8).trim != "" => s.substring(8).trim
    }

  /* Concatenates the Byte Buffers. */
  def concatByteBuffers(bb1: ByteBuffer, bb2: ByteBuffer): ByteBuffer =
    ByteBuffer
      .allocate(bb1.array().length + bb2.array().length)
      .put(bb1)
      .put(bb2)

  /* Includes the encoding with the Content Type. */
  def contentTypeWithEncoding(ct: ContentType, encoding: String): ContentType = HttpCharsets
    .getForKey(encoding)
    .fold(ct)(hc => ContentType.apply(ct.mediaType, () => hc))

  /* Checks if it is the correct Content type. */
  def isContentType(headerKey: String): Boolean =
    headerKey.toLowerCase.contains(`Content-Type`.lowercaseName)

  /* Checks if it is the correct Content length. */
  def isContentLength(headerKey: String): Boolean =
    headerKey.toLowerCase.contains(`Content-Length`.lowercaseName)
}
