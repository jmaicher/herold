package server.receiver

import server.Message
import server.sender.Sender

import spray.json._
import server.HeroldJsonProtocol._

class SendBackHandler(sender: Sender) extends Handler{
  override def handle(json: String): Unit = {
    val message = json.parseJson.convertTo[Message]
    sender.send(message.toJson.compactPrint)
  }
}
