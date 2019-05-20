package server.orders

import akka.actor.SupervisorStrategy.{Restart, Resume}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.pattern._
import akka.util.Timeout
import server.orders.SaveOrderService.SaveReq
import server.search.SearchService.{SearchReq, SearchResFull}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Success

class OrderService (val searchService: ActorRef) extends Actor with ActorLogging {
  
  import OrderService._
  
  val dbWriter: ActorRef = context.actorOf(workerSupervisor("src/main/scala/database/orders.txt", "dbWriter"))
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  
  override def receive: Receive = {
    case OrderReq(title) ⇒
      val snd = sender()
      val fRes = searchService ? SearchReq(title)
      fRes onComplete {
        case Success(SearchResFull(Some(_))) ⇒
          snd ! OrderRes("done")
          dbWriter ! SaveReq(title)
        case _ ⇒
          snd ! OrderRes("Ordered Item not FOUND")
      }
  }
  
  override def preStart (): Unit = {log.info("OrderService Up")}
  
  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case ChildIsDead ⇒ Restart
    case _ ⇒ Resume
  }
}
object OrderService {
  implicit val timeout: Timeout = 1 second
  
  def workerSupervisor (filename: String, name: String): Props = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      SaveOrderService.props(filename),
      childName = name,
      minBackoff = 1 second,
      maxBackoff = 30 seconds,
      randomFactor = 0.2
    ))
  
  def apply (searchService: ActorRef): OrderService = new OrderService(searchService)
  
  def props (searchService: ActorRef): Props = Props(OrderService(searchService))
  
  case class OrderReq (title: String)
  case class OrderRes (title: String)
  
  final case object ChildIsDead extends Throwable
}
