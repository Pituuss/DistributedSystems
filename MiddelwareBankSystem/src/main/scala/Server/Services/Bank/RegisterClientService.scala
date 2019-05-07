package Server.Services.Bank

import com.twitter.util.Future
import db.SimpleDB
import exchange.service.thrift._


class RegisterClientService (db: => SimpleDB) extends RegisterClient.MethodPerEndpoint {
  override def registerClient (person: PersonalData, initInput: Money): Future[RegisterUserReponse] = {
    println(s"*** REGISTER CLIENT $person $initInput")
    val currency = person.income.currency
    
    if (!db.exchange.contains(currency))
      return Future.exception(ErrorInOperation(s"Currency $currency is not being operated"))
    
    val accountType = if (person.income.amount * db.exchange(currency) > 5000) AccountType.Premium else AccountType.Standart
    val password = person.name + "ala123"
    
    db.addUser(person, initInput, accountType, Password(password)) match {
      case Left(reason) ⇒ Future.exception(ErrorInOperation(reason))
      case Right(a) ⇒ Future.value(a)
    }
  }
  
}
object RegisterClientService {
  def apply (db: => SimpleDB): RegisterClientService = new RegisterClientService(db)
}
