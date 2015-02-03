package common

import java.io.{IOException, OutputStreamWriter, PrintStream}
import java.net.{SocketTimeoutException, Socket}
import java.nio.charset.CodingErrorAction

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.LazyLogging
import common.event.EventDispatcher
import common.event.socket.{AckSocketEvent, SocketEvent}

import scala.io.{Codec, Source}

class SocketEventChannel(val socket: Socket) extends LazyLogging {
  private val out = new OutputStreamWriter(socket.getOutputStream, "UTF-8")
  private var listenThread: Thread = null
  private var disconnectHandler: SocketDisconnectHandler = null
  private implicit val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
  //socket.setSoTimeout(5000)

  def listen(dispatcher: EventDispatcher[SocketEvent]): Unit = {
    if(listenThread == null) {
      listenThread = new Thread(new Runnable {
        override def run(): Unit = {
          val in = Source.fromInputStream(socket.getInputStream, "UTF-8").getLines()
          logger.debug("Listening for socket events")
          try{
            while(in.hasNext) {
              val raw = in.next()
              //empty means heartbeat from client
              if(!raw.isEmpty) {
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
          }
          catch {
            case e: IOException => logger.error("IOException while listening: "+e)
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
    val raw = SocketEventChannel.mapper.writeValueAsString(event)+"\n"
    logger.debug("Sending: "+raw)
    out.write(raw, 0, raw.length)
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