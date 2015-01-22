package client

import java.net.{InetAddress, Socket}
import java.util.Date
import java.util.concurrent.{Executors, ExecutorService}

import com.typesafe.scalalogging.LazyLogging
import server.messages.{Message}
import server.receiver.{Handler, Receiver}
import server.sender.Sender

object Client {
  def main(args: Array[String]) {
    val pool: ExecutorService = Executors.newFixedThreadPool(10)
    for(i <- Range(0,10)) {
      pool.execute(new TestClient())
    }
  }
}

class TestClient extends Runnable with LazyLogging {
  val socket = new Socket(InetAddress.getLocalHost, 2020)
  val sender = new Sender(socket)

  override def run(): Unit = {
    authenticate()
  }

  def authenticate(): Unit = {
    /*
    val in = new Scanner(System.in)

    println("Please enter user id")
    val id = in.nextInt()

    println("Please enter token")
    val token = in.nextLine()

    val authRequest = AuthRequest("1", id, token)
    */

    val receiver = new Receiver(socket, new Handler {
      override def handle(message: Message): Unit = {

      }
    })
    receiver.listen()

    for(i <- Range(0,10000)) {
      sender.send(Message("message/send", List("This is an awesome message!", 1, 2.0, 1, List("bla"), Map("foo" -> "bar"))))
    }
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
