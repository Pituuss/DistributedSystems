package server.orders

import java.io.{BufferedWriter, File, FileWriter}

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import server.search.DBSearchService

import scala.concurrent.duration._

class SaveOrderService (val filename: String) extends Actor with ActorLogging {
  
  import SaveOrderService._
  
  val file = new File(filename)
  val bufferedWriter = new BufferedWriter(new FileWriter(file, true))
  
  override def receive: Receive = {
    case SaveReq(title) ⇒
      log.info("saving order")
      bufferedWriter.write(title)
      bufferedWriter.newLine()
      bufferedWriter.flush()
  }
  
  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ ⇒ Resume
  }
  
  override def preStart (): Unit = {log.info("SaveOrderService Up")}
  
  override def postStop (): Unit = {
    bufferedWriter.close()
  }
}
object SaveOrderService {
  
  
  def apply (filename: String): SaveOrderService = new SaveOrderService(filename)
  
  def props (filename: String): Props = Props(SaveOrderService(filename))
  
  case class SaveReq (title: String)
}
