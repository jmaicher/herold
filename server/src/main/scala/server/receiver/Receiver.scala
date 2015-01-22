package server.receiver

import java.net.Socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.LazyLogging
import server.messages.Message
import server.sender.Sender

import scala.io.BufferedSource

class Receiver[T<:Message](val socket: Socket, val handler: Handler) extends LazyLogging {
  def listen(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        val in = new BufferedSource(socket.getInputStream).getLines()
        while(in.hasNext) {
          val rawMessage = in.next()
          logger.debug(rawMessage)
          val mapper = new ObjectMapper with ScalaObjectMapper
          mapper.registerModule(DefaultScalaModule)
          val message = mapper.readValue[T](rawMessage)
          handler.handle(new Sender(socket), message)
        }
      }
    }).start()
  }
}
