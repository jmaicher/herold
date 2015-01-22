package server.controller

import com.typesafe.scalalogging.LazyLogging

class MessageController extends LazyLogging {
  def send(msg: String, i: Int, f: Double, l: Int, li: List[Any], m: Map[Any, Any]): String = {
    throw new IllegalArgumentException
  }
}
