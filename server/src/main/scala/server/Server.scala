package server

import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

object Server {
  def main(args: Array[String]): Unit = {
    (new HeroldServer(2020, 2)).run
  }
}

class HeroldServer(port: Int, poolSize: Int) extends Runnable {
  val serverSocket = new ServerSocket(port)
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)

  def run(): Unit = {
    try {
      while (true) {
        val socket = serverSocket.accept()
        pool.execute(new Handler(socket))
      }
    } finally {
      pool.shutdown()
    }
  }
}

class Handler(socket: Socket) extends Runnable {
  def run(): Unit = {
    socket.getOutputStream.write("Hello client".getBytes())
    socket.getOutputStream.close()
  }
}
