package Server.Services.Bank

import com.twitter.util.Future
import db.SimpleDB
import exchange.service.thrift._


class RegisterUserService (db: => SimpleDB) extends RegisterUser.MethodPerEndpoint {
  
  import RegisterUserService._
  
  override def register (person: PersonalData, initInput: Money): Future[RegisterUserReponse] = {
    val currency = person.income.currency
    if (!db.exchange.contains(currency)) return Future.exception(ErrorInOperation(s"Currency $currency is not being operated"))
    val accountType = if (person.income.amount * db.exchange(currency) > 5000) AccountType.Premium else AccountType.Standart
    val password = person.name + "ala123"
    
    db.addUser(person, initInput, accountType, Password(password)) match {
      case Left(reason) ⇒ Future.exception(ErrorInOperation(reason))
      case Right(a) ⇒ Future.value(a)
    }
  }
}
object RegisterUserService {
  def apply (db: => SimpleDB): RegisterUserService = new RegisterUserService(db)
  
  
}
