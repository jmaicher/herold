package server.sender

import java.io.PrintStream
import java.net.Socket

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.LazyLogging
import server.messages.Message

class Sender(socket: Socket) extends LazyLogging {
  val out = new PrintStream(socket.getOutputStream)
  def send(message: Message): Unit = {
    logger.debug(message.toString)
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    out.println(mapper.writeValueAsString(message))
    out.flush()
  }
}
