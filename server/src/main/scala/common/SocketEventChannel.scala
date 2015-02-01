package common

import java.io.PrintStream
import java.net.Socket
import java.nio.charset.CodingErrorAction

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.LazyLogging
import common.event.EventDispatcher
import common.event.socket.SocketEvent

import scala.io.{Codec, Source}

class SocketEventChannel(val socket: Socket) extends LazyLogging {
  private val out = new PrintStream(socket.getOutputStream)
  private var listenThread: Thread = null
  private var disconnectHandler: SocketDisconnectHandler = null

  def listen(dispatcher: EventDispatcher[SocketEvent]): Unit = {
    if(listenThread == null) {
      listenThread = new Thread(new Runnable {
        override def run(): Unit = {
          implicit val codec = Codec("UTF-8")
          codec.onMalformedInput(CodingErrorAction.REPLACE)
          codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
          val in = Source.fromInputStream(socket.getInputStream, "UTF-8").getLines()
          logger.debug("Listening for socket events")
          try{
            while(in.hasNext) {
              val raw = in.next()
              logger.debug("Received: "+raw)

              var event: SocketEvent = null
              try{
                event = SocketEventChannel.mapper.readValue[SocketEvent](raw)
              }
              catch {
                case e: IllegalArgumentException => logger.error("Error deserializing message!", e)
              }

              if(event != null) {
                dispatcher.dispatch(event)
              }
            }
          }
          catch {
            case e: Exception => logger.error("Exception while listening: "+e)
          }
          finally {
            try {
              socket.close()
            }
            finally {
              logger.debug("Socket disconnected. Stopped listening.")
              if(disconnectHandler != null) {
                disconnectHandler.onDisconnect()
              }
            }
          }
        }
      })
      listenThread.start()
    }
  }

  def send(event: SocketEvent): Unit = {
    val raw = SocketEventChannel.mapper.writeValueAsString(event)
    logger.debug("Sending: "+raw)
    out.println(raw)
    out.flush()
  }

  def setDisconnectHandler(h: SocketDisconnectHandler): Unit = {
    disconnectHandler = h
  }
}

protected object SocketEventChannel {
  protected val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.enableDefaultTyping()
}

trait SocketDisconnectHandler {
  def onDisconnect()
}