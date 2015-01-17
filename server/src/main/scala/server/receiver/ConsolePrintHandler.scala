package server.receiver

import server.Message

import spray.json._
import server.HeroldJsonProtocol._

class ConsolePrintHandler extends Handler {
  override def handle(json: String): Unit = {
    val message = json.parseJson.convertTo[Message]
    println("Rcv: "+message.body)
  }
}
