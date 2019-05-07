package Server.Services.Exchange

import java.util.concurrent.{Executors, TimeUnit}

import exchange.service.protos.Exchange.{CurrencyRateResponse, CurrencyRequest, ExchangeGrpc}
import io.grpc.stub.StreamObserver

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ExchangeRateService (state: ⇒ ExchangeStateService) extends ExchangeGrpc.Exchange {
  override def getRatesUpdates (
                                 request: CurrencyRequest,
                                 responseObserver: StreamObserver[CurrencyRateResponse]
                               ): Unit = {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val sessionKeys = request.currencyList
    var last = state.currencyToValue.toMap
    val feed: Runnable = () => {
      println(state.currencyToValue)
      val update = sessionKeys
        .filter(key ⇒
          state.currencyToValue(key) != last(key)
        )
        .map(key ⇒
          key.toString → (state.currencyToValue(key) / state.currencyToValue(request.bankCurrency))
        )
        .toMap
      last = state.currencyToValue.toMap
      responseObserver.onNext(CurrencyRateResponse(update))
    }
    scheduler.scheduleAtFixedRate(feed, 0l, 1000, TimeUnit.MILLISECONDS)
  }
  
  override def getRates (request: CurrencyRequest): Future[CurrencyRateResponse] = {
    val sessionKeys = request.currencyList
    Future(
      CurrencyRateResponse(sessionKeys.map(key ⇒
        key.toString → (state.currencyToValue(key) / state.currencyToValue(request.bankCurrency))).toMap)
    )
  }
}
object ExchangeRateService {
  def apply (state: ⇒ ExchangeStateService): ExchangeRateService = new ExchangeRateService(state)
}