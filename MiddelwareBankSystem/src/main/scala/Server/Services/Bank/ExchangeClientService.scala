package Server.Services.Bank

import java.util.concurrent.TimeUnit

import db.SimpleDB
import exchange.service.protos.Exchange.{CurrencyRequest, ExchangeGrpc}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import Server.Syntax._
import exchange.service.thrift.Currency

import scala.collection.mutable

class ExchangeClientService private(
                                     private val channel: ManagedChannel,
                                     private val exchangeClient: ExchangeGrpc.ExchangeBlockingStub,
                                     private val db: SimpleDB
                                   ) {
  
  def shutdown (): Unit = {
    channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)
  }
  
  def start (requestedCurrencies: List[Currency], bankCurrency: Currency): Unit = {
    val request = new CurrencyRequest(requestedCurrencies.map(a ⇒ a.thriftToGrpc), bankCurrency.thriftToGrpc)
    val init = mutable.Map() ++ exchangeClient.getRates(request).rates.map { case (k, v) ⇒ (k.stringToGRPC.grpcToThrift, v) }
    
    db.updateExchange(init)
    
    new Thread(
      () ⇒
        exchangeClient.getRatesUpdates(request)
          .foreach(response ⇒ {
            val update = mutable.Map() ++ response.rates.map { case (k, v) ⇒ (k.stringToGRPC.grpcToThrift, v) }
            db.updateExchange(update)
          })
    ).start()
    
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC client since JVM is shutting down")
      shutdown()
      System.err.println("*** client shut down")
    }
  }
}

object ExchangeClientService {
  def apply (host: String, port: Int, db: ⇒ SimpleDB): ExchangeClientService = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()
    val exchangeRateClient = ExchangeGrpc.blockingStub(channel)
    new ExchangeClientService(channel, exchangeRateClient, db)
  }
}