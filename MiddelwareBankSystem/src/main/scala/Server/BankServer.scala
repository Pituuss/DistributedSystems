package Server

import Server.Services.Bank._
import com.twitter.finagle.{ListeningServer, ThriftMux}
import db.SimpleDB
import exchange.service.thrift.Currency

class BankServer private(val currencies: List[Currency], bankCurrency: Currency) {
  
  import BankServer._
  
  val db = SimpleDB(bankCurrency)
  val exchangeClient = ExchangeClientService(exchangeServiceAddress, exchangeServicePort, db)
  exchangeClient.start(currencies, bankCurrency)
  private val registerService = Map(
    "register" → RegisterClientService(db)
  )
  private val accountManagerService = Map(
    "standard" → StandardManagerService(db),
    "premium" → PremiumManagerService(db)
  )
  
  val registerServer: ListeningServer = ThriftMux
    .server
    .serveIfaces(registerAddress, registerService)
  System.err.println(s"registerServer started at $registerAddress")
  
  val manageServer: ListeningServer = ThriftMux
    .server
    .serveIfaces(manageAddress, accountManagerService)
  
  System.err.println(s"manageServer started at $manageAddress")
  
  sys.addShutdownHook {
    System.err.println("*** shutting down Thrift server since JVM is shutting down")
    registerServer.close()
    manageServer.close()
    System.err.println("*** Thrift server shut down")
  }
}
object BankServer {
  def apply (): BankServer = new BankServer(List(Currency.Eur, Currency.Usd), Currency.Pln)
  
  private val registerAddress = "0.0.0.0:9099"
  private val manageAddress = "0.0.0.0:9091"
  
  private val exchangeServiceAddress = "localhost"
  private val exchangeServicePort = 50051
  
  def main (args: Array[String]): Unit = {
    BankServer()
  }
}
