package com.fullfacing.akka.monix.task.backend.utils

import cats.implicits._
import monix.eval.Task
import sttp.monad.{Canceler, MonadAsyncError}

import scala.util.{Failure, Success}

object TaskMonadAsyncError extends MonadAsyncError[Task] {
  override def unit[T](t: T): Task[T] = Task.now(t)

  override def map[T, T2](fa: Task[T])(f: (T) => T2): Task[T2] = fa.map(f)

  override def flatMap[T, T2](fa: Task[T])(f: (T) => Task[T2]): Task[T2] =
    fa.flatMap(f)

  override def async[T](register: (Either[Throwable, T] => Unit) => Canceler): Task[T] =
    Task.async { cb =>
      register {
        case Left(t)  => cb(Failure(t))
        case Right(t) => cb(Success(t))
      }
    }

  override def error[T](t: Throwable): Task[T] = Task.raiseError(t)

  override protected def handleWrappedError[T](rt: Task[T])(h: PartialFunction[Throwable, Task[T]]): Task[T] =
    rt.onErrorRecoverWith(h)

  def ensure[T](f: Task[T], e: => Task[Unit]): Task[T] = {
    f.flatTap(_ => e)
  }
}
