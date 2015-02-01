package server.event.server

import java.util.Date
import server.ClientSocket

abstract class ServerEvent

class ClientConnectedServerEvent(val clientSocket: ClientSocket) extends ServerEvent
class ClientDisconnectedServerEvent(val clientSocket: ClientSocket) extends ServerEvent

class AuthenticationServerEvent(val username: String, val password: String, val clientSocket: ClientSocket) extends ServerEvent
class AuthenticatedServerEvent(val username: String, val clientSocket: ClientSocket) extends ServerEvent

abstract class UserStatusServerEvent(val username: String) extends ServerEvent
class UserStatusOnlineServerEvent(username: String) extends UserStatusServerEvent(username)
class UserStatusOfflineServerEvent(username: String) extends UserStatusServerEvent(username)

class DeliverMessageServerEvent(val from: String, val to: String, val msg: String, val date: Date, val refUUID: String, var buffered:Boolean = false) extends ServerEvent