package db


import exchange.service.thrift.{Account, AccountType, Currency, Id, Login, Money, Password, PersonalData, RegisterUserReponse}

import scala.collection.mutable
import cats._
import cats.implicits._

class SimpleDB private(
                        val db: mutable.Map[Id, Account],
                        val exchange: mutable.Map[Currency, Float],
                        val bankCurrency: Currency
                      ) {
  
  import SimpleDB._
  
  def addUser (personalData: PersonalData, money: Money, accountType: AccountType, passwd: Password): Either[String, RegisterUserReponse] = {
    if (db.contains(personalData.id))
      "user already registered".asLeft
    else {
      val account = Account(personalData, money, Password(md5Hash(passwd._1)), accountType)
      db.put(personalData.id, account)
      RegisterUserReponse(passwd, accountType).asRight
    }
  }
  
  def updateExchange (update: mutable.Map[Currency, Float]): Unit = {
    update.foreach { case (k, v) ⇒ exchange.put(k, v) }
  }
  
  def updateBalance (id: Id, money: Money): Either[String, String] = {
    if (!db.contains(id)) {
      "user not found".asLeft
    } else {
      val account = db(id)
      print("updating balance")
      db.update(id, account)
      "ok".asRight
    }
    
  }
  
  def validateAuth (login: Login): Boolean = {
    db.contains(login.id) && db(login.id) == login.passwd
  }
  
  def getMoney (login: Login): Either[String, Money] = {
    db.get(login.id) match {
      case Some(money) ⇒ money.balance.asRight
      case None ⇒ "User not found".asLeft
    }
  }
}
object SimpleDB {
  def apply (bankCurrency: Currency): SimpleDB = new SimpleDB(mutable.Map[Id, Account](), mutable.Map[Currency, Float](), bankCurrency)
  
  def md5Hash (text: String): String =
    java.security.MessageDigest.getInstance("MD5")
      .digest(text.getBytes())
      .map(0xFF & _)
      .map {"%02x".format(_)}
      .foldLeft("") {_ + _}
}
