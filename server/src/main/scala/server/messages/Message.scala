package server.messages

import java.util.concurrent.atomic.AtomicInteger

case class Message(action: String, params: List[Any]) {
  val id = Message.getId()
}

object Message {
  private val refCounter = new AtomicInteger()
  def getId(): String = {
    refCounter.incrementAndGet().toString
  }
}