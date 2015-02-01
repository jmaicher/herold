package server

import java.net.{ServerSocket, Socket}
import java.util.concurrent.{ConcurrentLinkedQueue, ConcurrentHashMap, ExecutorService, Executors}
import _root_.server.event.server._
import common.{SocketDisconnectHandler, SocketEventChannel}
import common.event.socket._
import com.typesafe.scalalogging.LazyLogging
import common.event.{EventListener, EventDispatcher}

object Server {
  def main(args: Array[String]): Unit = {
    new Server(2020, 10000).run()
  }
}

class Server(val port: Int, val poolSize: Int) extends Runnable with LazyLogging {
  private val serverSocket = new ServerSocket(port)
  private val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)
  private val serverEventDispatcher = new EventDispatcher[ServerEvent]

  private val registry = new Registry(serverEventDispatcher)
  serverEventDispatcher.register(classOf[AuthRequestServerEvent], new Authenticator(registry, serverEventDispatcher))
  serverEventDispatcher.register(classOf[ServerEvent], registry)

  private val messageDeliverer = new MessageDeliverer(registry)
  serverEventDispatcher.register(classOf[ServerEvent], messageDeliverer)

  def run(): Unit = {
    try {
      logger.debug("Waiting for clients")
      while (true) {
        val socket = serverSocket.accept()
        pool.execute(new ClientSocket(socket, serverEventDispatcher))
        logger.debug("New client connected")
      }
    } finally {
      pool.shutdown()
    }
  }
}

class ClientSocket(val socket: Socket, val serverEventDispatcher: EventDispatcher[ServerEvent]) extends Runnable {
  private val socketEventDispatcher = new EventDispatcher[SocketEvent]
  private val socketEventChannel = new SocketEventChannel(socket)

  override def run(): Unit = awaitAuthentication()

  private def awaitAuthentication(): Unit = {
    socketEventDispatcher.register(classOf[AuthSocketEvent], new EventListener[AuthSocketEvent] {
      def on(e: AuthSocketEvent): Unit = e match {
        case e: AuthRequestSocketEvent =>
          serverEventDispatcher.dispatch(new AuthRequestServerEvent(e.username, e.password, socketEventDispatcher))
        case e: AuthSuccessSocketEvent =>
          authenticated(e.username)
          socketEventChannel.send(e)
        case e: AuthFailedSocketEvent => socketEventChannel.send(e)
        case _ =>
      }
    })

    socketEventChannel.listen(socketEventDispatcher)
    socketEventChannel.send(new AwaitingAuthSocketEvent)
  }

  private def authenticated(username: String): Unit = {
    socketEventDispatcher.register(classOf[MessageSocketEvent], new EventListener[MessageSocketEvent] {
      def on(e: MessageSocketEvent): Unit = e match {
        case e: SendMessageSocketEvent =>
          serverEventDispatcher.dispatch(new DeliverMessageServerEvent(username, e.to, e.msg, e.date, e.uuid))
        case e: ReceiveMessageSocketEvent => socketEventChannel.send(e)
        case e: MessageStatusSocketEvent => socketEventChannel.send(e)
        case _ =>
      }
    })
    socketEventChannel.setDisconnectHandler(new SocketDisconnectHandler {
      override def onDisconnect(): Unit = serverEventDispatcher.dispatch(new UserStatusOfflineServerEvent(username))
    })
  }
}

class Authenticator(val registry: Registry, val serverEventDispatcher: EventDispatcher[ServerEvent]) extends EventListener[AuthRequestServerEvent] with LazyLogging {

  private val credentials = Map[String, String](
    "david" -> "1234",
    "julian" -> "1234",
    "noelle" -> "1234"
  )

  def on(e: AuthRequestServerEvent): Unit = {
    /*
    credentials.get(e.username) match {
      case Some(p) => if(p.equals(e.password)) {
        //check if already online...
        if(!registry.get(e.username).isDefined) {
          logger.debug("Authentication succeeded for @"+e.username)
          e.socketEventDispatcher.dispatch(new AuthSuccessSocketEvent(e.username))
          serverEventDispatcher.dispatch(new AuthSuccessServerEvent(e.username, e.socketEventDispatcher))
        }
        else {
          logger.debug("Authentication failed for @"+e.username+": user already online")
          e.socketEventDispatcher.dispatch(new AuthFailedSocketEvent("User already signed in!"))
        }
        return
      }
      case _ =>
    }
    logger.debug("Authentication failed for @"+e.username+": wrong username/password")
    e.socketEventDispatcher.dispatch(new AuthFailedSocketEvent("Wrong username and/or password!"))
    */
    if(!registry.get(e.username).isDefined) {
      logger.debug("Authentication succeeded for @"+e.username)
      e.socketEventDispatcher.dispatch(new AuthSuccessSocketEvent(e.username))
      serverEventDispatcher.dispatch(new AuthSuccessServerEvent(e.username, e.socketEventDispatcher))
    }
    else {
      logger.debug("Authentication failed for @"+e.username+": user already online")
      e.socketEventDispatcher.dispatch(new AuthFailedSocketEvent("User already signed in!"))
    }
  }
}

class MessageDeliverer(registry: Registry) extends EventListener[ServerEvent] with LazyLogging {
  private val messageBuffer = new ConcurrentHashMap[String, ConcurrentLinkedQueue[DeliverMessageServerEvent]]
  def on(e: ServerEvent): Unit = e match {
    case e: DeliverMessageServerEvent => {
      //TODO: check if user exists before accepting and rejecting otherwise?
      if(!e.buffered) {
        registry.get(e.from) map { _.dispatch(new MessageStatusSocketEvent(e.refUUID, MessageStatusSocketEvent.ACCEPTED)) }
      }
      registry.get(e.to) match {
        case Some(socketEventDispatcher) => {
          logger.debug("User "+e.to+" is online. Delivering message: "+e.msg)
          //TODO: catch socket error and buffer?
          socketEventDispatcher.dispatch(new ReceiveMessageSocketEvent(e.from, e.msg, e.date))
          registry.get(e.from) map { _.dispatch(new MessageStatusSocketEvent(e.refUUID, MessageStatusSocketEvent.DELIVERED)) }
        }
        case None => {
          logger.debug("User "+e.to+" is offline. Buffering message: "+e.msg)
          var bufferedMessages = messageBuffer.get(e.to)
          if(bufferedMessages == null) {
            bufferedMessages = new ConcurrentLinkedQueue[DeliverMessageServerEvent]()
          }
          e.buffered = true
          bufferedMessages.add(e)
          messageBuffer.put(e.to, bufferedMessages)
        }
      }
    }
    case e: UserStatusOnlineServerEvent => {
      logger.debug("User "+e.username+" just came online")
      val bufferedMessages = messageBuffer.get(e.username)
      if(bufferedMessages == null || bufferedMessages.isEmpty) {
        logger.debug("No buffered messages found for "+e.username)
      }
      else {
        logger.debug("Delivering " + bufferedMessages.size() + " buffered message(s) to " + e.username)
        if (bufferedMessages != null) {
          while (!bufferedMessages.isEmpty) {
            on(bufferedMessages.poll())
          }
        }
      }
    }
    case _ =>
  }
}

class Registry(serverEventDispatcher: EventDispatcher[ServerEvent]) extends EventListener[ServerEvent] {
  private val users = new ConcurrentHashMap[String, EventDispatcher[SocketEvent]]()

  private def register(name: String, e: EventDispatcher[SocketEvent]) = users.put(name, e)

  private def unregister(name: String) = users.remove(name)

  def get(name: String): Option[EventDispatcher[SocketEvent]] = {
    users.get(name) match {
      case e: EventDispatcher[SocketEvent] => Some(e)
      case _ => None
    }
  }

  def on(e: ServerEvent): Unit = e match {
    case e: AuthSuccessServerEvent => {
      register(e.username, e.socketEventDispatcher)
      serverEventDispatcher.dispatch(new UserStatusOnlineServerEvent(e.username))
    }
    case e: UserStatusOfflineServerEvent => unregister(e.username)
    case _ =>
  }
}