package client

import java.net.{InetAddress, Socket}
import java.util.{Scanner}
import java.util.concurrent.{Executors, ExecutorService}

import com.typesafe.scalalogging.LazyLogging
import server.messages.{Message}
import server.receiver.{Handler, Receiver}
import server.sender.Sender

object Client {
  def main(args: Array[String]) {
    val pool: ExecutorService = Executors.newFixedThreadPool(10)
    pool.execute(new TestClient())
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

    println("Please enter user name")
    val name = in.next()

    //val authRequest = AuthRequest("1", id, token)

    val receiver = new Receiver(socket, new Handler {
      override def handle(message: Message): Unit = {

      }
    })
    receiver.listen()

    sender.send(Message("message/send", List("dmaicher", "jmaicher", "Ping")))
  }

  /*
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
  */
}
