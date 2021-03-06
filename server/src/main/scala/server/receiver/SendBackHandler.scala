package server.receiver

import server.messages.Message
import server.sender.Sender

class SendBackHandler(sender: Sender) extends Handler {
  override def handle(sender: Sender, message: Message): Unit = {
    sender.send(message)
  }
}
