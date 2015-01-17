package server.receiver

import java.net.Socket

import scala.io.BufferedSource

class Receiver(val socket: Socket, val handler: Handler) {
  def listen(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        val in = new BufferedSource(socket.getInputStream()).getLines()
        while(in.hasNext) {
          handler.handle(in.next())
        }
      }
    }).start()
  }
}
