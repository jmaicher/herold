package client

import java.net.{InetAddress, Socket}
import java.util.Scanner

import com.typesafe.scalalogging.LazyLogging
import server.messages.{ServerReply, ChatMessage, Message, AuthRequest}
import server.receiver.{Handler, Receiver}
import server.sender.Sender

object Client {
  def main(args: Array[String]) {
    new TestClient().run()
  }
}

class TestClient extends Runnable with LazyLogging {
  val socket = new Socket(InetAddress.getLocalHost, 2020)
  val sender = new Sender(socket)

  override def run(): Unit = {
    authenticate()
  }

  def authenticate(): Unit = {
    val in = new Scanner(System.in)

    println("Please enter user id")
    val id = in.nextInt()

    println("Please enter token")
    val token = in.nextLine()

    val authRequest = AuthRequest("1", id, token)

    val receiver = new Receiver(socket, new Handler {
      override def handle(message: Message): Unit = {
        logger.debug("received msg: "+message)
        message match {
          case msg: ServerReply => {
            if(msg.uuid == authRequest.uuid) {
              if(msg.status == ServerReply.OK) {
                logger.debug("Authenticated successfully!")
                startChat(authRequest.userId)
              }
              else {
                logger.warn("Authentication failed!")
              }
            }
          }
          case _ =>
        }
      }
    })
    receiver.listen()

    sender.send(authRequest)
  }

  def startChat(userId: Int): Unit = {

    val receiver = new Receiver(socket, new Handler {
      override def handle(message: Message): Unit = {
        logger.debug("received msg: "+message)
        message match {
          case msg: ChatMessage => {
            println("#"+msg.from+": "+msg.body)
          }
          case _ =>
        }
      }
    })
    receiver.listen()

    val in = new Scanner(System.in)

    println("Enter user id you want to chat with")
    val toUserId = in.nextInt()

    while(true) {
      val body = in.nextLine()
      val message = ChatMessage("2", userId.toInt, toUserId.toInt, "user", body)
      sender.send(message)
    }
  }
}
