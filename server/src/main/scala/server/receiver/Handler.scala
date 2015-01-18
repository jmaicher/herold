package server.receiver

import server.messages.Message

trait Handler {
  def handle(message: Message)
}
