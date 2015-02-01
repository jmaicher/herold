package common.event.socket

import java.util.Date

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@type")
abstract class SocketEvent {
  val uuid = java.util.UUID.randomUUID.toString
}

abstract class AuthSocketEvent extends SocketEvent
class AwaitingAuthSocketEvent extends AuthSocketEvent
class AuthRequestSocketEvent(var username: String, var password: String) extends AuthSocketEvent
class AuthSuccessSocketEvent(var username: String) extends AuthSocketEvent
class AuthFailedSocketEvent(var msg: String) extends AuthSocketEvent

abstract class MessageSocketEvent extends SocketEvent
class SendMessageSocketEvent(var to: String, var msg: String) extends MessageSocketEvent {
  var date: Date = new Date()
}
class MessageStatusSocketEvent(var refUUID: String, var status: String) extends MessageSocketEvent
object MessageStatusSocketEvent {
  val ACCEPTED = "accepted"
  val DELIVERED = "delivered"
  val REJECTED = "rejected"
}
class ReceiveMessageSocketEvent(var from: String, var msg: String, var date: Date) extends MessageSocketEvent

