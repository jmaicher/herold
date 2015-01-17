package server

import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

import server.receiver.{SendBackHandler, Receiver}
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
    new Server(2020, 2).run
  }
}

class Server(port: Int, poolSize: Int) extends Runnable {
  val serverSocket = new ServerSocket(port)
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)

  def run(): Unit = {
    try {
      while (true) {
        val socket = serverSocket.accept()
        pool.execute(new SocketHandler(socket))
      }
    } finally {
      pool.shutdown()
    }
  }
}

class SocketHandler(socket: Socket) extends Runnable {
  def run(): Unit = {
    val receiver = new Receiver(socket, new SendBackHandler(new Sender(socket)))
    receiver.listen()
  }
}
