package server.event.server

import java.util.Date
import common.event.EventDispatcher
import common.event.socket.SocketEvent

abstract class ServerEvent

class AuthRequestServerEvent(val username: String, val password: String, val socketEventDispatcher: EventDispatcher[SocketEvent]) extends ServerEvent
class AuthSuccessServerEvent(val username: String, val socketEventDispatcher: EventDispatcher[SocketEvent]) extends ServerEvent

abstract class UserStatusServerEvent(val username: String) extends ServerEvent
class UserStatusOnlineServerEvent(username: String) extends UserStatusServerEvent(username)
class UserStatusOfflineServerEvent(username: String) extends UserStatusServerEvent(username)

class DeliverMessageServerEvent(val from: String, val to: String, val msg: String, val date: Date, val refUUID: String, var buffered:Boolean = false) extends ServerEvent