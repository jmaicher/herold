package server.receiver

import server.messages.Message

class ConsolePrintHandler extends Handler {
  override def handle(json: String): Unit = Message.deserialize(json) match {
    case Some(msg) => println("Rcv: "+ msg.serialize())
    case _ =>
  }
}
