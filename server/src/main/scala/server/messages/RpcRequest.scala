package server.messages

import java.util.concurrent.atomic.AtomicInteger

abstract class Message {
  val id = Message.getId()
}

object Message {
  private val refCounter = new AtomicInteger()
  def getId(): String = {
    refCounter.incrementAndGet().toString
  }
}

abstract class IncomingMessage extends Message
case class RpcRequest(method: String, args: List[Any]) extends IncomingMessage

abstract class OutgoingMessage extends Message
case class Event(name: String, args: List[Any]) extends OutgoingMessage
