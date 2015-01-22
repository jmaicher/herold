package client

import java.net.{InetAddress, Socket}
import java.util.Scanner
import java.util.concurrent.{Executors, ExecutorService}

import com.typesafe.scalalogging.LazyLogging
import server.messages.{Message, Event, RpcRequest}
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

    val authRequest = RpcRequest("sessions/post", List(name))
    sender.send(authRequest)

    startChat(name)
  }

  def startChat(name: String): Unit = {

    val receiver = new Receiver[Event](socket, new Handler {
      override def handle(sender: Sender, message: Message): Unit = message match {
        case Event(name, args) => {
          logger.debug("received event: "+name)
          name match {
            case "message" => {
              println("#"+args(0)+": "+args(1))
            }
            case _ =>
          }
        }
        case _ =>
      }
    })
    receiver.listen()

    val in = new Scanner(System.in)

    println("Enter user name you want to chat with")
    val toUser = in.next()

    while(true) {
      val body = in.nextLine()
      val message = RpcRequest("message/send", List(name, toUser, body))
      sender.send(message)
    }
  }
}
