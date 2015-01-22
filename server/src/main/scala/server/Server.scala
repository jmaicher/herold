package server

import java.net.{ServerSocket, Socket}
import java.util.concurrent.{ExecutorService, Executors}

import server.receiver.Receiver
import server.routing.Router


object Server {
  def main(args: Array[String]): Unit = {
    new Server(2020, 100).run()
  }
}

class Server(port: Int, poolSize: Int) extends Runnable {
  val serverSocket = new ServerSocket(port)
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)
  val router = new Router

  def run(): Unit = {
    try {
      while (true) {
        val socket = serverSocket.accept()
        pool.execute(new SocketHandler(socket, router))
      }
    } finally {
      pool.shutdown()
    }
  }
}

class SocketHandler(socket: Socket, router: Router) extends Runnable {
  def run(): Unit = {
    val receiver = new Receiver(socket, router)
    receiver.listen()
  }
}