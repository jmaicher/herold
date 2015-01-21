package server.controller

import com.typesafe.scalalogging.LazyLogging

class MessageController extends LazyLogging {
  def send(msg: String): String = {
    msg
  }
}
