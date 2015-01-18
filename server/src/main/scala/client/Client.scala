package client

import java.net.{InetAddress, Socket}
import java.util.Scanner

import server.messages.{Message, AuthRequest}
import server.receiver.{Handler, Receiver}
import server.sender.Sender

object Client {
  def main(args: Array[String]) {
    new TestClient().run()
  }
}

class TestClient extends Runnable {
  val socket = new Socket(InetAddress.getLocalHost, 2020)
  var authenticated = false

  override def run(): Unit = {
    val receiver = new Receiver(socket, new Handler {
      override def handle(message: Message): Unit = {
        println(message)
      }
    })
    receiver.listen()

    val sender = new Sender(socket)
    val in = new Scanner(System.in)

    while(true) {
      val body = in.nextLine()
      val message = AuthRequest("1", "1")
      sender.send(message)
    }
  }
}
