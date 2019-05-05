package Server.Services.Exchange

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import exchange.service.protos.Exchange.Currency

import scala.collection.mutable
import scala.util.Random

class ExchangeStateService private(val currencyToValue: mutable.Map[Currency, Float]) {
  self ⇒
  
  import ExchangeStateService._
  
  val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  val simulation: Runnable = () => {
    for (c <- Currency.values) {
      val value = self.currencyToValue(c)
      val upd = if (rand.nextFloat > .5) (value - rand.nextFloat * 3).abs else (value + rand.nextFloat * 3).abs
      self.currencyToValue.update(c, upd)
    }
  }
  scheduler.scheduleAtFixedRate(simulation, 0, 1000, TimeUnit.MILLISECONDS)
}
object ExchangeStateService {
  
  val rand: Random.type = Random
  
  def apply (): ExchangeStateService = new ExchangeStateService(mutable.Map.empty ++ Currency.values.map(k ⇒ k → (rand.nextFloat * 8)).toMap)
}
