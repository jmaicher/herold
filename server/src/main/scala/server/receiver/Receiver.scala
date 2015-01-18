package server.receiver

import java.net.Socket

import com.typesafe.scalalogging.LazyLogging
import server.messages.Message

import scala.io.BufferedSource

class Receiver(val socket: Socket, val handler: Handler) extends LazyLogging {
  def listen(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        val in = new BufferedSource(socket.getInputStream).getLines()
        while(in.hasNext) {
          val rawMessage = in.next()
          Message.deserialize(rawMessage) match {
            case Some(msg) => handler.handle(msg)
            case None => logger.warn("Unable to deserialize message: "+rawMessage)
          }
        }
      }
    }).start()
  }
}
