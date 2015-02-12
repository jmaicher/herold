package server

import com.typesafe.scalalogging.LazyLogging
import org.glassfish.grizzly.http.Cookie
import org.glassfish.grizzly.http.server._
import org.glassfish.grizzly.http.util.HttpStatus
import org.glassfish.grizzly.websockets._

object Server {
  def main(args: Array[String]): Unit = {

    val server = HttpServer.createSimpleServer(null, 8080)

    val authenticator = new UserRegistry()
    authenticator.addUser(new User("David", "mail@dmaicher.de", "test"))

    server.getListener("grizzly").registerAddOn(new WebSocketAddOn())
    server.getServerConfiguration.addHttpHandler(new AuthenticationHandler(authenticator), "/auth")
    WebSocketEngine.getEngine.register("", "/chat", new ChatApplication(authenticator))

    server.start()
    Thread.sleep(Long.MaxValue)
  }
}

class User(var name: String, var email: String, var password: String)

class UserRegistry {
  private var emailToUser = Map[String, User]()
  private var accessTokenToUser = Map[String, User]()

  def addUser(u: User): Unit = {
    emailToUser += u.email -> u
  }

  def addAccessToken(u: User, t: String): Unit = {
    accessTokenToUser += t -> u
  }

  def getUserByEmail(e: String): Option[User]= {
    emailToUser.get(e)
  }

  def getUserByAccessToken(t: String): Option[User]= {
    accessTokenToUser.get(t)
  }
}

class AuthenticationHandler(userRegistry: UserRegistry) extends HttpHandler {
  override def service(request: Request, response: Response): Unit = {

    val email = request.getParameter("email")
    val password = request.getParameter("password")

    val user = userRegistry.getUserByEmail(email)
    if(user.isDefined && user.get.password.equals(password)){
      val newToken = java.util.UUID.randomUUID.toString
      userRegistry.addAccessToken(user.get, newToken)
      response.setStatus(HttpStatus.OK_200)
      response.addCookie(new Cookie("Token", newToken))
    }
    else {
      response.setStatus(HttpStatus.FORBIDDEN_403)
    }
  }
}

class ChatApplication(userRegistry: UserRegistry) extends WebSocketApplication with LazyLogging {
  override def onConnect(socket: WebSocket): Unit = {
    logger.info("Connected ... " + socket.toString)
    val defSocket = socket.asInstanceOf[DefaultWebSocket]
    val accessToken = defSocket.getUpgradeRequest.getCookies find (_.getName.equals("Token"))
    if(accessToken.isEmpty || userRegistry.getUserByAccessToken(accessToken.get.getValue).isEmpty) {
      logger.info("NOT AUTHENTICATED!" + socket.toString)
      socket.close(403, "Access denied")
    }
    else {
      logger.info("Authenticated!" + socket.toString)
    }
    super.onConnect(socket)
  }

  override def onClose(socket: WebSocket, frame: DataFrame): Unit = {
    super.onClose(socket, frame)
    logger.info(String.format("Session %s closed",socket.toString))
  }

  override def onMessage(socket: WebSocket, message: String): Unit = {
    println(message)
  }
}

/*

class Server(val port: Int, val poolSize: Int) extends LazyLogging {
  private val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)
  private val serverEventDispatcher = new EventDispatcher[ServerEvent]

  private val registry = new Registry(serverEventDispatcher)
  private val authenticator = new Authenticator(registry, serverEventDispatcher)

  private val messageDeliverer = new MessageDeliverer(registry)
  serverEventDispatcher.register(classOf[ServerEvent], messageDeliverer)

  def run(): Unit = {
    try {
      logger.debug("Waiting for clients")
      while (true) {
        //val clientSocket = new ClientSocket(serverSocket.accept(), serverEventDispatcher)
        //pool.execute(clientSocket)
        //serverEventDispatcher.dispatch(new ClientConnectedServerEvent(clientSocket))
      }
    } finally {
      pool.shutdown()
    }
  }
}

class ClientSocket(val socket: Socket, val serverEventDispatcher: EventDispatcher[ServerEvent]) extends Runnable {
  private val socketEventDispatcher = new EventDispatcher[SocketEvent]
  private val socketEventChannel = new SocketEventChannel(socket)
  socketEventChannel.listen(socketEventDispatcher)
  socketEventChannel.setDisconnectHandler(new SocketDisconnectHandler {
    override def onDisconnect(): Unit = serverEventDispatcher.dispatch(new ClientDisconnectedServerEvent(ClientSocket.this))
  })

  override def run(): Unit = awaitAuthentication()

  private def awaitAuthentication(): Unit = {
    socketEventDispatcher.register(classOf[AuthRequestSocketEvent], new EventListener[AuthRequestSocketEvent] {
      def on(e: AuthRequestSocketEvent): Unit = {
        serverEventDispatcher.dispatch(new AuthenticationServerEvent(e.username, e.password, ClientSocket.this))
      }
    })
  }

  def authenticate(username: String): Unit = {
    socketEventDispatcher.register(classOf[SendMessageSocketEvent], new EventListener[SendMessageSocketEvent] {
      def on(e: SendMessageSocketEvent): Unit = {
        serverEventDispatcher.dispatch(new DeliverMessageServerEvent(username, e.to, e.msg, e.date, e.uuid))
      }
    })
  }

  def send(e: SocketEvent): Unit = {
    socketEventChannel.send(e)
  }
}

class Authenticator(val registry: Registry, val serverEventDispatcher: EventDispatcher[ServerEvent]) extends LazyLogging {

  serverEventDispatcher.register(classOf[AuthenticationServerEvent], new EventListener[AuthenticationServerEvent] {
    def on(e: AuthenticationServerEvent): Unit = {
      //TODO: use some storage & actually check credentials
      if(!registry.get(e.username).isDefined) {
        logger.debug("Authentication succeeded for @"+e.username)
        e.clientSocket.authenticate(e.username)
        e.clientSocket.send(new AuthSuccessSocketEvent(e.username))
        serverEventDispatcher.dispatch(new AuthenticatedServerEvent(e.username, e.clientSocket))
      }
      else {
        logger.debug("Authentication failed for @"+e.username+": user already online")
        e.clientSocket.send(new AuthFailedSocketEvent("User already signed in!"))
      }
    }
  })

  serverEventDispatcher.register(classOf[ClientConnectedServerEvent], new EventListener[ClientConnectedServerEvent] {
    def on(e: ClientConnectedServerEvent): Unit = {
      e.clientSocket.send(new AwaitingAuthSocketEvent)
    }
  })
}

class MessageDeliverer(registry: Registry) extends EventListener[ServerEvent] with LazyLogging {
  private val messageBuffer = new ConcurrentHashMap[String, ConcurrentLinkedQueue[DeliverMessageServerEvent]]
  def on(e: ServerEvent): Unit = e match {
    case e: DeliverMessageServerEvent => {
      //TODO: check if user exists before accepting and rejecting otherwise?
      if(!e.buffered) {
        registry.get(e.from) map { _.send(new MessageStatusSocketEvent(e.refUUID, MessageStatusSocketEvent.ACCEPTED)) }
      }
      registry.get(e.to) match {
        case Some(clientSocket) => {
          logger.debug("User "+e.to+" is online. Delivering message "+e.refUUID)
          //TODO: catch socket error and buffer?
          clientSocket.send(new ReceiveMessageSocketEvent(e.from, e.msg, e.date))
          registry.get(e.from) map { _.send(new MessageStatusSocketEvent(e.refUUID, MessageStatusSocketEvent.DELIVERED)) }
        }
        case None => {
          logger.debug("User "+e.to+" is offline. Buffering message "+e.refUUID)
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

class Registry(serverEventDispatcher: EventDispatcher[ServerEvent]) {
  private val nameToSocket = new ConcurrentHashMap[String, ClientSocket]()
  private val socketToName = new ConcurrentHashMap[ClientSocket, String]()

  def get(name: String): Option[ClientSocket] = {
    nameToSocket.get(name) match {
      case s: ClientSocket => Some(s)
      case _ => None
    }
  }

  serverEventDispatcher.register(classOf[AuthenticatedServerEvent], new EventListener[AuthenticatedServerEvent] {
    override def on(e: AuthenticatedServerEvent): Unit = {
      nameToSocket.put(e.username, e.clientSocket)
      socketToName.put(e.clientSocket, e.username)
      serverEventDispatcher.dispatch(new UserStatusOnlineServerEvent(e.username))
    }
  })

  serverEventDispatcher.register(classOf[ClientDisconnectedServerEvent], new EventListener[ClientDisconnectedServerEvent] {
    override def on(e: ClientDisconnectedServerEvent): Unit = {
      val username = socketToName.remove(e.clientSocket)
      if(username != null) {
        nameToSocket.remove(username)
        serverEventDispatcher.dispatch(new UserStatusOfflineServerEvent(username))
      }
    }
  })
}
*/
