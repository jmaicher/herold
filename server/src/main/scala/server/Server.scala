package server

import java.lang.reflect.InvocationTargetException
import java.net.{Socket, ServerSocket}
import java.util.Date
import java.util.concurrent.{Executors, ExecutorService}
import server.controller.MessageController
import server.messages.Message
import server.receiver.{Handler, Receiver}
import server.sender.Sender
import scala.reflect.runtime.universe._

object Server {
  def main(args: Array[String]): Unit = {
    new Server(2020, 2).run()
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
    val receiver = new Receiver(socket, Router)
    receiver.listen()
  }
}

object Router extends Handler {
  private val mirror = runtimeMirror(getClass.getClassLoader)
  private var methodReflCache = Map[String, MethodMirror]()
  private var controllerReflCache = Map[String, InstanceMirror]()
  var time: Long = 0

  private val routing = Map(
    "message" -> new MessageController
  )

  override def handle(message: Message): Unit = {
    getReflectedMethod(message.action) match {
      case Some(m) => {
        try {
          m(message.params:_*)
        }
        catch {
          case e: IllegalArgumentException => //wrong arguments
          case e: InvocationTargetException => //error in business logic => 500
        }
      }
      case None => //?
    }

    if(message.id.equals("1")) {
      time = System.currentTimeMillis()
    }
    else if(message.id.equals("10000")) {
      println((System.currentTimeMillis()-time)/1000f)
    }
  }

  private def getReflectedMethod(action: String): Option[MethodMirror] = {
    methodReflCache.get(action) match {
      case Some(m) => Some(m)
      case None => {
        val routingParts = action.split("/")
        if (routingParts.size == 2) {
          try {
            routing.get(routingParts(0)) match {
              case Some(controller) => {
                val im: InstanceMirror = {
                  controllerReflCache.get(routingParts(0)) match {
                    case Some(cim) => cim
                    case None => {
                      val im = mirror.reflect(controller)
                      controllerReflCache += routingParts(0) -> im
                      im
                    }
                  }
                }

                val method = im.symbol.typeSignature.member(newTermName(routingParts(1))).asMethod
                val reflMethod = im.reflectMethod(method)
                methodReflCache += action -> reflMethod
                Some(reflMethod)
              }
              case None => None
            }
          }
          catch {
            case e: ScalaReflectionException => None
          }
        }
        else {
          None
        }
      }
    }
  }
}
