package server.stream

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import server.stream.StreamWorker.StreamReq
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class StreamService private(val searchService: ActorRef) extends Actor with ActorLogging {
  
  import StreamService._
  
  implicit val executionContext: ExecutionContextExecutor = context.system.dispatcher
  
  override def receive: Receive = {
    case request: StreamReq ⇒
      val worker = context.actorOf(StreamWorker.props(searchService))
      pipe(worker ? request) to sender()
  }
  
  override def preStart (): Unit = {log.info("StreamService Up")}
  
  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ ⇒ Resume
  }
}
object StreamService {
  implicit val timeout: Timeout = 1 second
  
  def apply (searchService: ActorRef): StreamService = new StreamService(searchService)
  
  def props (searchService: ActorRef): Props = Props(StreamService(searchService))
}
