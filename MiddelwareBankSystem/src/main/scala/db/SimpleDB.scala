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
    println(s"**** DB ADD USER $personalData $money")
    if (db.contains(personalData.id)) {
      println("***** EXCEPTION")
      "user already registered".asLeft
    }
    else {
      val account = Account(
        personalData,
        Money(money.amount * exchange(money.currency), bankCurrency),
        Password(md5Hash(passwd.password)),
        accountType
      )
      db.put(personalData.id, account)
      RegisterUserReponse(passwd, accountType).asRight
    }
  }
  
  def updateExchange (update: mutable.Map[Currency, Float]): Unit = {
    println(s"**** DB UPDATE EXCHANGE RATES $update")
    update.foreach { case (k, v) ⇒ exchange.put(k, v) }
  }
  
  def updateBalance (id: Id, money: Money): Either[String, String] = {
    println(s"**** DB UPDATE USER BALANCE $id $money")
    if (!db.contains(id)) {
      "user not found".asLeft
    } else {
      val account = db(id)
      val balance = Money(db(id).balance.amount + money.amount / exchange(money.currency), bankCurrency)
      val updatedAccount = Account(
        account.client,
        balance,
        account.passwd,
        account.accountType
      )
      db.update(id, updatedAccount)
      "ok".asRight
    }
    
  }
  
  def validateAuth (login: Login): Boolean = {
    db.contains(login.id) && db(login.id).passwd == login.passwd
  }
  
  def validateAuthPremium (login: Login): Boolean = {
    validateAuth(login) && db(login.id).accountType == AccountType.Premium
  }
  
  
  def getMoney (login: Login): Either[String, Money] = {
    db.get(login.id) match {
      case Some(money) ⇒ money.balance.asRight
      case None ⇒ "User not found".asLeft
    }
  }
}
object SimpleDB {
  def apply (bankCurrency: Currency): SimpleDB = new SimpleDB(mutable.Map[Id, Account](), mutable.Map(bankCurrency → 1f), bankCurrency)
  
  def md5Hash (text: String): String =
    java.security.MessageDigest.getInstance("MD5")
      .digest(text.getBytes())
      .map(0xFF & _)
      .map {"%02x".format(_)}
      .foldLeft("") {_ + _}
}
