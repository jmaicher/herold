package server

import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

import akka.actor.{ActorLogging, Props, ActorSystem, Actor}
import com.typesafe.scalalogging.{LazyLogging, Logger}
import server.authentication.Authenticator
import akka.actor.{Props, ActorSystem, Actor}
import server.messages.Message
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

  override def handle(json: String): Unit = Message.deserialize(json) match {
    case Some(msg) => println("Rcv: "+ msg.serialize())
    case _ =>
  }

  def authenticated: Receive = {
    case m: String => {
      logger.debug("received: "+m)
      _sender.send(m)
    }
  }

  //default receive when not authenticated yet
  def receive = {
    case m: String => {
      logger.debug("received: "+m)
      if (authenticator.authenticate(m)) {
        logger.debug("authentication successful")
        become(authenticated)
        _sender.send(m) // TODO: success message?
      } else {
        logger.debug("authentication failed")
        _sender.send(m) // TODO: error message?
      }
    }
    case _ =>
  }
}
