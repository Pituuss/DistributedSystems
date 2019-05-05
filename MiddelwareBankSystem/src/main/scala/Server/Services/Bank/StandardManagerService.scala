package Server.Services.Bank

import com.twitter.util.Future
import db.SimpleDB
import exchange.service.thrift.{ErrorInOperation, Login, Money, StandardManager}

class StandardManagerService (db: ⇒ SimpleDB) extends StandardManager.MethodPerEndpoint {
  override def getBalance (login: Login): Future[Money] = {
    if (db.validateAuth(login)) {
      db.getMoney(login) match {
        case Right(money) ⇒ Future.value(money)
        case Left(reason) ⇒ Future.exception(ErrorInOperation(reason))
      }
    }
    else
      Future.exception(ErrorInOperation("Invalid username or password"))
  }
}
object StandardManagerService {
  def apply (db: ⇒ SimpleDB): StandardManagerService = new StandardManagerService(db)
}
