package server.receiver

class ConsolePrintHandler extends Handler {
  override def handle(message: String): Unit = {
    println("Rcv: "+message)
  }
}
