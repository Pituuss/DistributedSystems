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
    "register" → RegisterUserService(db)
  )
  private val accountManagerService = Map(
    "standard" → StandardManagerService(db),
    "premium" → PremiumManagerService(db)
  )
  
  val registerServer: ListeningServer = ThriftMux
    .server
    .serveIfaces(address, registerService)
  
  val manageServer: ListeningServer = ThriftMux
    .server
    .serveIfaces(address, accountManagerService)
  
  System.err.println(s"Server started at $address")
  
  sys.addShutdownHook {
    System.err.println("*** shutting down Thrift server since JVM is shutting down")
    registerServer.close()
    manageServer.close()
    System.err.println("*** Thrift server shut down")
  }
}
object BankServer {
  def apply (): BankServer = new BankServer(List(Currency.Eur, Currency.Gbp), Currency.Pln)
  
  private val address = "0.0.0.0:9001"
  
  private val exchangeServiceAddress = "localhost"
  private val exchangeServicePort = 50051
  
  def main (args: Array[String]): Unit = {
    BankServer()
  }
}
