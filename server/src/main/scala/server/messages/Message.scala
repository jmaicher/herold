package server.messages

import spray.json._

object MessageJsonProtocol extends DefaultJsonProtocol {
  implicit val authRequestFormat = jsonFormat3(AuthRequest.apply)
  implicit val chatMessageFormat = jsonFormat6(ChatMessage.apply)
}

import MessageJsonProtocol._

sealed trait Message {
  val id: String
  val uuid: String

  def serialize(): String
}

object Message {

  def deserialize(serialized: String): Option[Message] = {
    val jsonObj = serialized.parseJson.asJsObject

    jsonObj.getFields("id").headOption.flatMap(value => value match {
      case JsString(AuthRequest.id) => Some(jsonObj.convertTo[AuthRequest])
      case JsString(ChatMessage.id) => Some(jsonObj.convertTo[ChatMessage])
      case _ => None
    })
  }

}

case class AuthRequest(id: String, uuid: String, token: String)
  extends Message {
  override def serialize(): String = this.toJson.compactPrint
}

object AuthRequest {
  val id = "a"
  def apply(uuid: String, token: String): AuthRequest = apply(id, uuid, token)
}

case class ChatMessage(id: String, uuid: String, from: Int, to: Int, toType: String, body: String)
  extends Message {
  override def serialize(): String = this.toJson.compactPrint
}

object ChatMessage {
  val id = "m"
  def apply(uuid: String, from: Int, to: Int, toType: String, body: String): ChatMessage =
    apply(id, uuid, from, to, toType, body)

  val USER = "u"
  val GROUP = "g"
}


case class ServerReply(id: String, uuid: String, status: Int)

object ServerReply {
  val id = "r"
  def apply(uuid: String, status: Int): ServerReply = apply(id, uuid, status)

  val OK = 200
  val ACCEPTED = 202
  val BAD_REQUEST = 400
  val UNAUTHORIZED = 401
}
