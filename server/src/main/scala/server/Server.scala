package server

import java.net.{Socket, ServerSocket}

import akka.actor._
import com.typesafe.scalalogging.{LazyLogging}
import server.authentication.{User, Authenticator}
import server.messages.{ServerReply, ChatMessage, AuthRequest, Message}
import server.receiver.{Handler, Receiver}
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
  val registry = new Registry
  val broker = system.actorOf(Props(classOf[MessageBroker], registry))

  def run(): Unit = {
    logger.info("server is running")
    try {
      while (true) {
          system.actorOf(Props(classOf[SocketActor], serverSocket.accept(), authenticator, registry, broker))
      }
    } finally {
      system.shutdown()
    }
  }
}

class Registry {
  private val userMap = new scala.collection.mutable.LinkedHashMap[Int, ActorRef]

  def register(id: Int, actor: ActorRef) = {
    userMap.synchronized {
      userMap += id -> actor
    }
  }

  def unregister(id: Int) = {
    userMap.synchronized {
      userMap -= id
    }
  }

  def getActorFor(id: Int): Option[ActorRef] = {
    userMap.synchronized {
      userMap.get(id)
    }
  }
}

class MessageBroker(val registry: Registry) extends Actor with LazyLogging {
  def receive = {
    case msg: ChatMessage => registry.getActorFor(msg.to) match {
      case Some(actor) => actor forward msg
      case _ => {
        sender() ! ServerReply(msg.uuid, ServerReply.ACCEPTED)
        logger.debug("Cannot deliver message, recipient $msg.to not connected")
      }
    }
    case _ =>
  }
}

class SocketActor(val socket: Socket, val authenticator: Authenticator, val registry: Registry, val broker: ActorRef)
  extends Actor with Handler with LazyLogging {

  import context._
  private val _sender = new Sender(socket)
  private val receiver = new Receiver(socket, this)
  receiver.listen()

  override def handle(message: Message): Unit = {
    self ! message
  }

  def authenticated(user: User): Receive = {
    case msg: ChatMessage => msg.to match {
      case user.id => {
        _sender.send(msg)
        sender() ! ServerReply(msg.uuid, ServerReply.OK)
      }
      case _ => broker ! msg
    }
    case reply: ServerReply => {
      _sender.send(reply)
    }
    case _ =>
  }

  //default receive when not authenticated yet
  def receive = {
    case authReq: AuthRequest => authenticator.authenticate(authReq) match {
      case Some(user) => {
        become(authenticated(user))
        registry.register(user.id, self)
        logger.debug("authentication successful")
        val reply = ServerReply(authReq.uuid, ServerReply.OK)
        _sender.send(reply)
      }
      case _ => {
        logger.debug("authentication failed")
        val reply = ServerReply(authReq.uuid, ServerReply.BAD_REQUEST)
        _sender.send(reply)
      }
    }
    case _ =>
  }
}
