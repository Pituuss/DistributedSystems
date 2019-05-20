package server.stream

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.stream.scaladsl.{FileIO, Flow, Framing, StreamRefs}
import akka.stream.{ActorMaterializer, SourceRef, ThrottleMode}
import akka.util.{ByteString, Timeout}
import server.search.SearchService.{SearchReq, SearchResFull}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Success

class StreamWorker (val searchService: ActorRef) extends Actor with ActorLogging {
  
  import StreamWorker._
  
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  
  override def receive: Receive = {
    case StreamReq(title) ⇒
      val snd = sender()
      val fRes = searchService ? SearchReq(title)
      fRes onComplete {
        case Success(SearchResFull(Some(res))) ⇒
          val path = Paths.get(s"${res.path}")
          pipe(
            FileIO
              .fromPath(path)
              .via(separateLines)
              .via(toUtf8)
              .runWith(StreamRefs.sourceRef())
              .map(stream ⇒ StreamRes(Some(stream), self))
          ) to snd
        case _ ⇒
          snd ! StreamRes(None, self)
          context.stop(self)
      }
  }
  
  override def preStart (): Unit = {log.info("StreamWorker Up")}
}
object StreamWorker {
  implicit val timeout: Timeout = 1 second
  
  private val toUtf8: Flow[ByteString, String, NotUsed] = Flow[ByteString].map(_.utf8String)
  
  private val separateLines: Flow[ByteString, ByteString, NotUsed] =
    Framing
      .delimiter(
        ByteString(System.lineSeparator()),
        maximumFrameLength = 512,
        allowTruncation = true
      ).throttle(1, 1 second, 1, ThrottleMode.shaping)
  
  def apply (searchService: ActorRef): StreamWorker = new StreamWorker(searchService)
  
  def props (searchService: ActorRef): Props = Props(StreamWorker(searchService))
  
  case class StreamReq (title: String)
  
  case class StreamRes (stream: Option[SourceRef[String]], ref: ActorRef)
}
