package server.search

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import server.search.SearchService.SearchResFull
import server.search.SessionActor.Bump

class SessionManager extends Actor with ActorLogging {
  
  import SessionManager._
  
  override def receive: Receive = {
    case BumpReq(who: ActorRef) ⇒
      if (context.children.toList.contains(who))
        who ! Bump
    case KillReq(who: ActorRef) ⇒
      if (context.children.toList.contains(who))
        who ! PoisonPill
    case SpawnReq(inform) ⇒
      val ref = context.actorOf(SessionActor.props(inform))
      sender ! ref
  }
}
object SessionManager {
  final case class BumpReq (who: ActorRef)
  final case class SpawnReq (who: ActorRef)
  final case class KillReq (who: ActorRef)
  final case class Response (who: ActorRef, msg: SearchResFull)
  
  
  def props: Props = Props(new SessionManager)
}
