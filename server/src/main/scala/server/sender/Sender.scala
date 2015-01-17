package server.sender

import java.io.PrintStream
import java.net.Socket

class Sender(socket: Socket) {
  val out = new PrintStream(socket.getOutputStream())
  def send(message: String): Unit = {
    out.println(message)
    out.flush()
  }
}
