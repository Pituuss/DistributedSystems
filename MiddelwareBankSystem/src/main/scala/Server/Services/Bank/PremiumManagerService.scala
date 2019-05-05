package Server.Services.Bank

import com.twitter.util.Future
import db.SimpleDB
import exchange.service.thrift._

import cats._
import cats.implicits._

class PremiumManagerService (db: ⇒ SimpleDB) extends PremiumManager.MethodPerEndpoint {
  
  override def getBalance (login: Login): Future[Money] =
    super.getBalance(login)
  
  override def takeLoan (loanRequest: LoanRequest): Future[LoanResponse] = {
    if (!db.validateAuth(loanRequest.login))
      Future.exception(ErrorInOperation("invalid login or password"))
    
    val multiplier = 3
    val foreignCost = Money(loanRequest.money.amount * multiplier * loanRequest.time, loanRequest.money.currency)
    val nativeCost = Money(foreignCost.amount * db.exchange(loanRequest.money.currency), db.bankCurrency)
    db.updateBalance(loanRequest.login.id, loanRequest.money)
    Future.value(LoanResponse(accepted = true, nativeCost.some, foreignCost.some))
  }
}
object PremiumManagerService {
  def apply (db: ⇒ SimpleDB): PremiumManagerService = new PremiumManagerService(db)
}
