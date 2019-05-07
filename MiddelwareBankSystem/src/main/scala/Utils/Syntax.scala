package Utils

import exchange.service.protos.Exchange.Currency

object Syntax {
  implicit class GrpcToThrift (currency: Currency) {
    def grpcToThrift: exchange.service.thrift.Currency = {
      currency match {
        case Currency.USD ⇒ exchange.service.thrift.Currency.Usd
        case Currency.GBP ⇒ exchange.service.thrift.Currency.Gbp
        case Currency.PLN ⇒ exchange.service.thrift.Currency.Pln
        case Currency.EUR ⇒ exchange.service.thrift.Currency.Eur
        case Currency.CHF ⇒ exchange.service.thrift.Currency.Chf
      }
    }
  }
  implicit class StringToGrpc (currencyName: String) {
    implicit def stringToGRPC: Currency = {
      currencyName match {
        case "USD" ⇒ Currency.USD
        case "GBP" ⇒ Currency.GBP
        case "PLN" ⇒ Currency.PLN
        case "EUR" ⇒ Currency.EUR
        case "CHF" ⇒ Currency.CHF
      }
    }
  }
  implicit class ThriftToGrpc (currency: exchange.service.thrift.Currency) {
    def thriftToGrpc: Currency = {
      currency match {
        case exchange.service.thrift.Currency.Usd ⇒ Currency.USD
        case exchange.service.thrift.Currency.Gbp ⇒ Currency.GBP
        case exchange.service.thrift.Currency.Pln ⇒ Currency.PLN
        case exchange.service.thrift.Currency.Eur ⇒ Currency.EUR
        case exchange.service.thrift.Currency.Chf ⇒ Currency.CHF
      }
    }
  }
}
