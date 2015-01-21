package server

import java.net.{Socket, ServerSocket}
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
  val useReflection = false

  private val routing = Map(
    "message" -> new MessageController
  )

  override def handle(message: Message): Unit = {
    if(!useReflection) {
      val routingParts = message.action.split("/")
      if (routingParts.size == 2) {
        routing.get(routingParts(0)) match {
          case Some(controller) => {
            controller.send(message.params(0).toString)
          }
          case None => //BAM
        }
      }
    }
    else {
      methodReflCache.get(message.action) match {
        case Some(m) => m(message.params: _*)
        case None => {
          val routingParts = message.action.split("/")
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
                  val reflectMethod = im.reflectMethod(method)
                  methodReflCache += message.action -> reflectMethod
                  reflectMethod(message.params: _*)
                }
                case None => //BAM
              }
            }
            catch {
              case e: ScalaReflectionException => //BAM
            }
          }
          else {
            //BAM
          }
        }
      }
    }

    if(message.id.equals("1")) {
      time = System.currentTimeMillis()
    }
    else if(message.id.equals("10000")) {
      println((System.currentTimeMillis()-time)/1000f)
    }
  }
}
