package Server.Services.Bank

import com.twitter.util.Future
import db.SimpleDB
import exchange.service.thrift._

import cats._
import cats.implicits._

class PremiumManagerService (db: ⇒ SimpleDB) extends PremiumManager.MethodPerEndpoint {
  
  override def getBalance (login: Login): Future[Money] = {
    println(s"*** PREMIUM MANAGER GET BALANCE $login")
    if (db.validateAuthPremium(login)) {
      db.getMoney(login) match {
        case Right(money) ⇒ Future.value(money)
        case Left(reason) ⇒ Future.exception(ErrorInOperation(reason))
      }
    }
    else
      Future.exception(ErrorInOperation("Invalid username or password or you are to poor"))
  }
  
  override def takeLoan (loanRequest: LoanRequest): Future[LoanResponse] = {
    println(s"*** TAKE LOAN $loanRequest")
    val login = loanRequest.login
    val exchange = db.exchange
    if (db.validateAuthPremium(login)) {
      val multiplier = 3
      val foreignCost = Money(loanRequest.money.amount * multiplier * loanRequest.time, loanRequest.money.currency)
      val nativeCost = Money(foreignCost.amount * exchange(loanRequest.money.currency), db.bankCurrency)
      db.updateBalance(login.id, loanRequest.money)
      Future.value(LoanResponse(accepted = true, nativeCost.some, foreignCost.some))
    }
    else {
      Future.exception(ErrorInOperation("Invalid login or password or you are too poor"))
    }
  }
}
object PremiumManagerService {
  def apply (db: ⇒ SimpleDB): PremiumManagerService = new PremiumManagerService(db)
}
