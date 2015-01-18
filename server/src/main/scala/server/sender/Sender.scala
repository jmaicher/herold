package server.sender

import java.io.PrintStream
import java.net.Socket

import server.messages.Message

class Sender(socket: Socket) {
  val out = new PrintStream(socket.getOutputStream)
  def send(message: Message): Unit = {
    out.println(message.serialize())
    out.flush()
  }
}
