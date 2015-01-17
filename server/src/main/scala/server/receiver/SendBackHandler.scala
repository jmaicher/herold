package server.receiver

import server.sender.Sender

class SendBackHandler(sender: Sender) extends Handler{
  override def handle(message: String): Unit = {
    println("Sending back: "+message)
    sender.send(message)
  }
}
