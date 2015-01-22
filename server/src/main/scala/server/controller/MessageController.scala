package server.controller

import com.typesafe.scalalogging.LazyLogging
import server.Context
import server.messages.Event

class RpcController extends LazyLogging

class SessionsController extends RpcController {
  def post(context: Context, name: String) = {
    context.getRegistry.register(name, context.getClient)
  }
}

class MessageController extends RpcController {
  def send(context: Context, from: String, recipient: String, msg: String) = {
    context.getRegistry.getClient(recipient).map(sender => {
      val event = Event("message", List(from, msg))
      sender.send(event)
    })
  }
}
