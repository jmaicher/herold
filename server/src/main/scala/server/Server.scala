package server

import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

import akka.actor.{Props, ActorSystem, Actor}
import server.receiver.{Handler, SendBackHandler, Receiver}
import server.sender.Sender
import spray.json.{DefaultJsonProtocol}

case class Message(id: String, body: String)

object Message {
  val id = "h/m"
  def apply(body: String): Message = apply(id, body)
}

object HeroldJsonProtocol extends DefaultJsonProtocol {
  implicit val messageFormat = jsonFormat2(Message.apply)
}

object Server {
  def main(args: Array[String]): Unit = {
    new Server(2020, 2).run()
  }
}

class Server(port: Int, poolSize: Int) extends Runnable {
  val serverSocket = new ServerSocket(port)
  val system = ActorSystem()

  def run(): Unit = {
    try {
      while (true) {
          system.actorOf(Props(classOf[SocketActor], serverSocket.accept()))
      }
    } finally {
      system.shutdown()
    }
  }
}

class SocketActor(val socket: Socket) extends Actor with Handler {

  val receiver = new Receiver(socket, this)
  receiver.listen()

  override def handle(message: String): Unit = {
    println("received: "+message)
  }

  def receive = {
    case _ => println("received some message")
  }
}
