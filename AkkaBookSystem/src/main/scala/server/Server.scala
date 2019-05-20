package server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import server.orders.OrderService
import server.orders.OrderService.OrderReq
import server.search.SearchService
import server.search.SearchService.{SearchReq, SearchRes, SearchResFull}
import server.stream.StreamService
import server.stream.StreamWorker.StreamReq

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class Server extends Actor with ActorLogging {
  
  import Server._
  
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  
  private val search = context.actorOf(SearchService.props(), "searchService")
  private val order = context.actorOf(OrderService.props(search), "orderService")
  private val stream = context.actorOf(StreamService.props(search), "streamService")
  
  override def receive: Receive = {
    case searchReq: SearchReq ⇒
      log.info("received search request")
      val snd = sender()
      pipe((search ? searchReq).map {
        case res: SearchResFull ⇒
          SearchRes(
            for {
              book ← res.book
              price = book.price
            } yield price
          )
      }) to snd
    
    case orderReq: OrderReq ⇒
      log.info("received order request")
      val snd = sender()
      pipe(order ? orderReq) to snd
    
    case streamReq: StreamReq ⇒
      log.info("received stream request")
      val snd = sender()
      pipe(stream ? streamReq) to snd
    
    case _ ⇒ log.info("some msg")
  }
}
object Server {
  implicit val timeout: Timeout = 1 second
  
  def main (args: Array[String]): Unit = {
    val config = ConfigFactory.load("resources/server")
    val system: ActorSystem = ActorSystem("server", config)
    val server = system.actorOf(Server.props(), "server")
    println(server.path)
  }
  
  def apply (): Server = new Server()
  
  def props (): Props = Props(Server())
}
