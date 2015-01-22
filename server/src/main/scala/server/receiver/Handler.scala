package server.receiver

import server.messages.Message
import server.sender.Sender

trait Handler {
  def handle(sender: Sender, message: Message)
}
