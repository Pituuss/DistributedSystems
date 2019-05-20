package client

import akka.NotUsed
import akka.actor.ActorRef.noSender
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Framing, Sink}
import akka.stream.{ActorMaterializer, SourceRef, ThrottleMode}
import akka.util.{ByteString, Timeout}
import com.typesafe.config.{Config, ConfigFactory}
import server.orders.OrderService.{OrderReq, OrderRes}
import server.search.SearchService.{SearchReq, SearchRes, SearchResFull}
import server.stream.StreamWorker.{StreamReq, StreamRes}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn
import scala.util.Success

class Client extends Actor with ActorLogging {
  
  import Client._
  
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  
  private val search = "search (.+)".r
  private val order = "order (.+)".r
  private val stream = "stream (.+)".r
  
  val server: ActorRef = Await.result(context.actorSelection("akka.tcp://server@127.0.0.1:2552/user/server").resolveOne(5 seconds), 5 seconds)
  
  
  override def receive: Receive = {
    case command: String ⇒
      command match {
        case search(cmd) ⇒
          (server ? SearchReq(cmd)).mapTo[SearchRes] onComplete {
            case Success(res) ⇒ println(res)
            case _ ⇒ println("failed")
          }
        case order(cmd) ⇒
          (server ? OrderReq(cmd)).mapTo[OrderRes] onComplete {
            case Success(res) ⇒ println(res)
            case _ ⇒ println("failed")
          }
        case stream(cmd) ⇒
          (server ? StreamReq(cmd)).mapTo[StreamRes] onComplete {
            case Success(StreamRes(Some(res), who)) ⇒
              res.runWith(Sink.foreach(println)).map(_ ⇒ context.stop(who))
            case Success(StreamRes(None, who)) ⇒
              println("book not found")
              context.stop(who)
            case cause ⇒
              println(s"failed $cause")
          }
        case "bump" ⇒
          server ! " bump"
        case _ ⇒
          log.info("unknown message")
      }
  }
  
  override def preStart (): Unit = {
    self ! "bump"
  }
}
object Client {
  implicit val timeout: Timeout = 2 seconds
  
  val flow: Flow[ByteString, ByteString, NotUsed] =
    Framing
      .delimiter(
        ByteString(System.lineSeparator()),
        maximumFrameLength = 512,
        allowTruncation = true
      ).throttle(1, 1 second, 1, ThrottleMode.shaping)
  
  def main (args: Array[String]): Unit = {
    val config: Config = ConfigFactory.load("resources/client")
    val system: ActorSystem = ActorSystem("client", config)
    val client = system.actorOf(Client.props(), "client")
    Iterator.continually(StdIn.readLine()).takeWhile(_ != "q").foreach(client.tell(_, noSender))
  }
  
  def apply (): Client = new Client()
  
  def props (): Props = Props(Client())
}
