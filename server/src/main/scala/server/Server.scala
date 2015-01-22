package server

import java.net.{ServerSocket, Socket}
import java.util.concurrent.{ConcurrentHashMap, ExecutorService, Executors}

import server.receiver.Receiver
import server.routing.Router
import server.sender.Sender


object Server {
  def main(args: Array[String]): Unit = {
    new Server(2020, 100).run()
  }
}

class Server(port: Int, poolSize: Int) extends Runnable {
  val serverSocket = new ServerSocket(port)
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)
  val router = new Router(new Registry)

  def run(): Unit = {
    try {
      while (true) {
        val socket = serverSocket.accept()
        pool.execute(new ClientSocket(socket, router))
      }
    } finally {
      pool.shutdown()
    }
  }
}

class ClientSocket(socket: Socket, router: Router) extends Runnable {
  def run(): Unit = {
    val receiver = new Receiver(socket, router)
    receiver.listen()
  }
}

class Registry {
  private val userMap = new ConcurrentHashMap[String, Sender]()
  def register(name: String, sender: Sender) = userMap.put(name, sender)
  def unregister(name: String) = userMap.remove(name)
  def getClient(name: String): Option[Sender] = {
    userMap.get(name) match {
      case s: Sender => Some(s)
      case _ => None
    }
  }
}

class Context(private val registry: Registry) {
  def getRegistry = registry
}