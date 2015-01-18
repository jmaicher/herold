package server

import java.net.{Socket, ServerSocket}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, Executors, ExecutorService}

import akka.actor.{ActorLogging, Props, ActorSystem, Actor}
import com.typesafe.scalalogging.{LazyLogging, Logger}
import server.authentication.Authenticator
import akka.actor.{Props, ActorSystem, Actor}
import server.messages.{ServerReply, ChatMessage, AuthRequest, Message}
import server.receiver.{Handler, SendBackHandler, Receiver}
import server.sender.Sender

object Server {
  def main(args: Array[String]): Unit = {
    new Server(2020, 2).run()
  }
}

class Server(port: Int, poolSize: Int) extends Runnable with LazyLogging {
  val serverSocket = new ServerSocket(port)
  val system = ActorSystem()
  val authenticator = new Authenticator

  def run(): Unit = {
    logger.info("server is running")
    try {
      while (true) {
          system.actorOf(Props(classOf[SocketActor], serverSocket.accept(), authenticator))
      }
    } finally {
      system.shutdown()
    }
  }
}

class SocketActor(val socket: Socket, val authenticator: Authenticator) extends Actor with Handler with LazyLogging {
  import context._
  private val _sender = new Sender(socket)
  private val receiver = new Receiver(socket, this)
  receiver.listen()

  override def handle(message: Message): Unit = {
    self ! message
  }

  def authenticated: Receive = {
    case msg: ChatMessage => {
      logger.debug("received: "+msg)
      _sender.send(msg)
    }
    case msg: Message => {
      logger.debug("received unhandled message: " + msg)
      val reply = ServerReply(msg.uuid, ServerReply.NOT_FOUND)
      _sender.send(reply)
    }
    case _ =>
  }

  //default receive when not authenticated yet
  def receive = {
    case msg: AuthRequest => {
      logger.debug("received: "+msg)
      if (authenticator.authenticate(msg)) {
        become(authenticated)
        logger.debug("authentication successful")
        val reply = ServerReply(msg.uuid, ServerReply.OK)
        _sender.send(reply)
      } else {
        logger.debug("authentication failed")
        val reply = ServerReply(msg.uuid, ServerReply.BAD_REQUEST)
        _sender.send(reply)
      }
    }
    case msg: Message => {
      logger.debug("received unhandled message: " + msg)
      val reply = ServerReply(msg.uuid, ServerReply.NOT_FOUND)
      _sender.send(reply)
    }
    case _ =>
  }
}
