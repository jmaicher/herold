package server

import java.math.BigInteger
import java.security.SecureRandom

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.LazyLogging
import org.glassfish.grizzly.http.CookiesBuilder.ServerCookiesBuilder
import org.glassfish.grizzly.http.server._
import org.glassfish.grizzly.http.util.HttpStatus
import org.glassfish.grizzly.http.{Cookie, HttpRequestPacket}
import org.glassfish.grizzly.websockets._
import server.message.{ReceiveMessage, Message, SendMessage}

object Server {
  def main(args: Array[String]): Unit = {

    val server = HttpServer.createSimpleServer(null, 8080)

    val userRegistry = new UserRegistry()
    userRegistry.addUser(new User(1, "David", "mail@dmaicher.de", "test"))
    userRegistry.addUser(new User(2, "Julian", "mail@jmaicher.de", "test"))

    server.getListener("grizzly").registerAddOn(new WebSocketAddOn())
    server.getServerConfiguration.addHttpHandler(new AuthenticationHandler(userRegistry), "/auth")
    WebSocketEngine.getEngine.register("", "/chat", new ChatApplication(userRegistry))

    server.start()
    Thread.sleep(Long.MaxValue)
  }
}

class User(var id: Int, var name: String, var email: String, var password: String) {
  override def toString = "#"+id+", "+email
}

class UserRegistry {
  private var emailToUser = Map[String, User]()
  private var accessTokenToUser = Map[String, User]()
  def addUser(u: User): Unit = emailToUser += u.email -> u
  def addAccessToken(u: User, t: String): Unit = accessTokenToUser += t -> u
  def getUserByEmail(e: String): Option[User]= emailToUser.get(e)
  def getUserByAccessToken(t: String): Option[User]= accessTokenToUser.get(t)
}

object AccessTokenGenerator {
  val random = new SecureRandom()
  def generate = new BigInteger(260, random).toString(32)
}

class AuthenticationHandler(userRegistry: UserRegistry) extends HttpHandler {
  override def service(request: Request, response: Response): Unit = {
    val email = request.getParameter("email")
    val password = request.getParameter("password")

    val user = userRegistry.getUserByEmail(email)
    if(user.isDefined && user.get.password.equals(password)){
      val newToken = AccessTokenGenerator.generate
      userRegistry.addAccessToken(user.get, newToken)
      response.setStatus(HttpStatus.OK_200)
      response.addCookie(new Cookie("Token", newToken))
    }
    else {
      response.setStatus(HttpStatus.FORBIDDEN_403)
    }
  }
}

class ChatWebSocket(private val user: User, handler: ProtocolHandler, requestPacket: HttpRequestPacket, listeners: WebSocketListener*) extends
  DefaultWebSocket(handler, requestPacket, listeners: _*)
{
  def getUser: User = user
}

class ChatApplication(userRegistry: UserRegistry) extends WebSocketApplication with LazyLogging {
  override def onConnect(socket: WebSocket): Unit = {
    logger.info("Connected ... " + socket.toString)
    socket match {
      case s: ChatWebSocket =>
        logger.info("Authenticated: " + socket.asInstanceOf[ChatWebSocket].getUser)
        super.onConnect(socket)
      case _ =>
        logger.info("NOT AUTHENTICATED!" + socket.toString)
        socket.close(403, "Invalid access token")
    }
  }

  override def createSocket(handler: ProtocolHandler, requestPacket: HttpRequestPacket, listeners: WebSocketListener*): WebSocket = {
    val header = requestPacket.getHeader("Cookie")
    if(header != null) {
      val cookies = new ServerCookiesBuilder(true, true).parse(header).build()
      val accessTokenCookie = cookies.findByName("Token")
      if(accessTokenCookie != null) {
        val accessToken = accessTokenCookie.getValue
        logger.debug("Found access token. Checking if valid... ")
        val user = userRegistry.getUserByAccessToken(accessToken)
        if(user.nonEmpty) {
          return new ChatWebSocket(user.get, handler, requestPacket, listeners: _*)
        }
      }
    }
    logger.debug("Did not find valid access token.")
    super.createSocket(handler, requestPacket, listeners: _*)
  }

  override def onClose(socket: WebSocket, frame: DataFrame): Unit = {
    super.onClose(socket, frame)
    logger.debug("Websocket closed: "+socket.toString)
  }

  override def onMessage(socket: WebSocket, raw: String): Unit = {
    logger.debug("Received: "+raw)
    try {
      ChatApplication.mapper.readValue[Message](raw) match {
        case m: SendMessage =>
          val rawMg = ChatApplication.mapper.writeValueAsString(
            new ReceiveMessage(socket.asInstanceOf[ChatWebSocket].getUser.id, m.body)
          )
          getWebSockets.toArray.filter(_ != socket).foreach(_.asInstanceOf[WebSocket].send(rawMg))
        case _ =>
      }
    }
    catch {
      case e: Exception => logger.error("Error handling message", e)
    }
  }

  override def onError(webSocket: WebSocket, t: Throwable): Boolean = {
    logger.error("Exception: "+t)
    super.onError(webSocket, t)
  }

  private object ChatApplication {
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.enableDefaultTyping()
  }
}