package client

import java.net.{InetAddress, Socket}
import java.util.Scanner

import server.messages.{ServerReply, ChatMessage, Message, AuthRequest}
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

    val authRequest = AuthRequest("1", "1")

    val receiver = new Receiver(socket, new Handler {
      override def handle(message: Message): Unit = {
        message match {
          case msg: ServerReply => {
            if(msg.uuid == authRequest.uuid) {
              if(msg.status == ServerReply.OK) {
                authenticated = true
                println("Authenticated successfully!")
              }
              else {
                println("Authentication failed!")
              }
            }
          }
        }
      }
    })
    receiver.listen()

    val sender = new Sender(socket)
    sender.send(authRequest)

    val in = new Scanner(System.in)

    while(true) {
      val body = in.nextLine()
      if(authenticated) {
        val message = ChatMessage("2", 1, 2, "user", body)
        sender.send(message)
      }
      else {
        println("Not authenticated yet!")
      }
    }
  }
}
