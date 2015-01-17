package server.receiver

import server.sender.Sender

import server.HeroldJsonProtocol._
import spray.json.pimpAny

class SendBackHandler(sender: Sender) extends Handler{
  override def handle(message: String): Unit = {
    val msg = server.Message(message).toJson.compactPrint
    sender.send(msg)
  }
}
