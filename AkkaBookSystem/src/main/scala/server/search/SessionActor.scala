package server.search

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import server.search.SearchService.SearchResFull

class SessionActor (inform: ActorRef) extends Actor with ActorLogging {
  
  import SessionActor._
  
  def rcv (count: Int): Receive = {
    case Bump ⇒
      context.become(rcv(count + 1))
      self ! Check
    
    case Check ⇒
      if (count > 1) {
        inform ! SearchResFull(None)
      }
  }
  
  override def receive: Receive = rcv(0)
}
object SessionActor {
  def apply (inform: ActorRef): SessionActor = new SessionActor(inform)
  
  def props (inform: ActorRef): Props = Props(SessionActor(inform))
  
  final case object Bump
  final case object Check
  
}