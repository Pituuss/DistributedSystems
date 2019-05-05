package Server


import java.util.logging.Logger

import exchange.service.protos.Exchange.ExchangeGrpc
import Server.Services.Exchange._
import io.grpc.{Server, ServerBuilder}
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender

import scala.concurrent.ExecutionContext

class ExchangeServer private(executionContext: ExecutionContext, state: ExchangeStateService) {
  private val server: Server = ServerBuilder.
    forPort(ExchangeServer.port)
    .addService(
      ExchangeGrpc.bindService(
        ExchangeRateService(state), executionContext)
    )
    .build
  
  private def start (): Unit = {
    server.start()
    ExchangeServer.logger.info("Server started, listening on " + ExchangeServer.port)
    
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      this.stop()
      System.err.println("*** server shut down")
    }
  }
  
  private def stop (): Unit = {
    server.shutdown()
  }
  
  private def blockUntilShutdown (): Unit = {
    server.awaitTermination()
  }
}

object ExchangeServer {
  private val logger = Logger.getLogger(classOf[ExchangeServer].getName)
  private val port = 50051
  
  def apply (executionContext: ExecutionContext, state: ExchangeStateService): ExchangeServer =
    new ExchangeServer(executionContext, state)
  
  def main (args: Array[String]): Unit = {
    BasicConfigurator.configure(new NullAppender())
    val server = ExchangeServer(ExecutionContext.global, ExchangeStateService())
    server.start()
    server.blockUntilShutdown()
  }
}
