package server.search

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor, ask}
import akka.util.Timeout
import server.search.SessionManager.{BumpReq, KillReq, SpawnReq}
import server.search.DBSearchService.{FindBookReq, FindBookRes}
import server.search.SessionManager.{BumpReq, KillReq, SpawnReq}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.Success

class SearchService () extends Actor with ActorLogging {
  
  import SearchService._
  
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  val sessionManager: ActorRef = context.actorOf(SessionManager.props)
  
  context.actorOf(workerSupervisor("src/main/scala/database/db1.json", "searchWorker1"))
  context.actorOf(workerSupervisor("src/main/scala/database/db2.json", "searchWorker2"))
  
  override def receive: Receive = {
    case SearchReq(title) ⇒
      val snd = sender()
      val counter = Await.result((sessionManager ? SpawnReq(snd)).mapTo[ActorRef], 1 second)
      log.info(s"server.search.SearchService looking fo '$title'")
      context.children
        .map(worker ⇒ worker ? FindBookReq(title))
        .foreach(
          _ onComplete {
            case Success(FindBookRes(Some(book))) ⇒
              log.info("success")
              snd ! SearchResFull(Some(book))
              sessionManager ! KillReq(counter)
            case _ ⇒
              sessionManager ! BumpReq(counter)
          })
  }
  
  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ ⇒ Resume
  }
  
  override def preStart (): Unit = {log.info("SearchService Up")}
  
  override def postStop (): Unit = {
    context.children.foreach(child ⇒ child ! PoisonPill)
  }
}
object SearchService {
  implicit val timeout: Timeout = 500 millis
  
  def workerSupervisor (filename: String, name: String): Props = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      DBSearchService.props(filename),
      childName = name,
      minBackoff = 1 second,
      maxBackoff = 30 seconds,
      randomFactor = 0.2
    ))
  
  def apply (): SearchService = new SearchService()
  
  def props (): Props = Props(SearchService())
  
  final case class SearchReq (title: String)
  final case class SearchResFull (book: Option[Book])
  final case class SearchRes (title: Option[String])
}
