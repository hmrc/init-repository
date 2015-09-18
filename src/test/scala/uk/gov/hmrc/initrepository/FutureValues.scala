package uk.gov.hmrc.initrepository

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

trait FutureValues {

  import scala.concurrent.duration._
  import scala.concurrent.{Await, Future}

  implicit val defaultTimeout = 5 seconds

  implicit def extractAwait[A](future: Future[A]) = awaitResult[A](future)

  def awaitResult[A](future: Future[A])(implicit timeout: Duration) = Await.result(future, timeout)

  implicit class FuturePimp[T](future: Future[T]) {
    def await: T = {
      Await.result(future, defaultTimeout)
    }

    def awaitSuccessOrThrow: Unit = {
      future.onComplete {
        case Success(value) => println(s"Got the callback, meaning = $value")
        case Failure(e) => throw e
      }
      Await.result(future, defaultTimeout)
    }
  }

}
