package server.search

import java.io.FileNotFoundException

import akka.actor.SupervisorStrategy.{Restart, Resume}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.{BufferedSource, Source}
import net.liftweb.json._

class DBSearchService private(val filename: String) extends Actor with ActorLogging {
  
  import DBSearchService._
  
  implicit val formats: DefaultFormats.type = DefaultFormats
  val fileSource: BufferedSource = Source.fromFile(filename)
  
  def rcv (fileSource: BufferedSource): Receive = {
    case FindBookReq(title) ⇒
      log.info(s"DBSearch looking for $title")
      val books = parse(fileSource.mkString)
        .extract[List[Book]]
        .filter {
          case Book(t, _, _) ⇒ t.equals(title)
        }
      if (books.isEmpty) {
        sender() ! Future.failed(RecordNotFoundException)
      }
      else {
        sender() ! FindBookRes(books.headOption)
      }
      context.become(rcv(Source.fromFile(filename)), discardOld = true)
  }
  
  override def receive: Receive = rcv(Source.fromFile(filename))
  
  override def preStart (): Unit = {log.info("DBSearchService Up")}
  
  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: FileNotFoundException ⇒ Restart
    case _ ⇒ Resume
  }
}
object DBSearchService {
  def apply (filename: String): DBSearchService = new DBSearchService(filename)
  
  def props (filename: String): Props = Props(DBSearchService(filename))
  
  final case class FindBookReq (title: String)
  final case class FindBookRes (books: Option[Book])
  
  final case object RecordNotFoundException extends Throwable
}
