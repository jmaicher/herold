package server.sender

import java.io.PrintStream
import java.net.Socket

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import server.messages.Message

class Sender(socket: Socket) {
  val out = new PrintStream(socket.getOutputStream)
  def send(message: Message): Unit = {

    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    out.println(mapper.writeValueAsString(message))
    out.flush()
  }
}
