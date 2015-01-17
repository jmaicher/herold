package client

import java.net.{InetAddress, Socket}
import java.util.Scanner

import server.receiver.{ConsolePrintHandler, Receiver}
import server.sender.Sender

object Client {
  def main(args: Array[String]) {
    new TestClient().run()
  }
}

class TestClient extends Runnable {
  val socket = new Socket(InetAddress.getLocalHost, 2020)

  override def run(): Unit = {
    val receiver = new Receiver(socket, new ConsolePrintHandler)
    receiver.listen()

    val sender = new Sender(socket)
    val in = new Scanner(System.in)

    while(true) {
      val message = in.nextLine()
      sender.send(message)
    }
  }
}
