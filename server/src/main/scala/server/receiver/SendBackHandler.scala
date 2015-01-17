package server.receiver

import server.sender.Sender

class SendBackHandler(sender: Sender) extends Handler{
  override def handle(json: String): Unit = {
    sender.send(json)
  }
}
