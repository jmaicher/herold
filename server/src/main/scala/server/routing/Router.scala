package server.routing

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import server.sender.Sender
import server.{Context, Registry}
import server.controller.{SessionsController, MessageController}
import server.messages.{Message, RpcRequest}
import server.receiver.Handler
import scala.reflect.runtime.universe._

class Router(val registry: Registry) extends Handler {
  private val mirror = runtimeMirror(getClass.getClassLoader)
  private val methodReflCache = new ConcurrentHashMap[String, MethodMirror]()
  private val controllerReflCache = new ConcurrentHashMap[String, InstanceMirror]()

  private val routing = Map(
    "message" -> new MessageController,
    "sessions" -> new SessionsController
  )

  override def handle(sender: Sender, message: Message): Unit = message match {
    case RpcRequest(method, args) => {
      getReflectedMethod(method) match {
        case Some(m) => {
          try {
            implicit val context = new Context(sender, registry)
            m(context::args: _*)
          }
          catch {
            case e: IllegalArgumentException => //wrong arguments
            case e: InvocationTargetException => //error in business logic => 500
          }
        }
        case None => //?
      }
    }
  }

  private def getReflectedMethod(action: String): Option[MethodMirror] = {
    methodReflCache.get(action) match {
      case m: MethodMirror => Some(m)
      case null => {
        val routingParts = action.split("/")
        if (routingParts.size == 2) {
          try {
            routing.get(routingParts(0)) match {
              case Some(controller) => {
                val im: InstanceMirror = {
                  controllerReflCache.get(routingParts(0)) match {
                    case cim: InstanceMirror => cim
                    case null => {
                      val im = mirror.reflect(controller)
                      controllerReflCache.put(routingParts(0), im)
                      im
                    }
                  }
                }

                val method = im.symbol.typeSignature.member(newTermName(routingParts(1))).asMethod
                val reflMethod = im.reflectMethod(method)
                methodReflCache.put(action, reflMethod)
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

