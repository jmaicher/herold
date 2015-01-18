package server.authentication

import server.messages.AuthRequest

case class User(id: Int)

class Authenticator {
  def authenticate(authReq: AuthRequest): Option[User] = {
    Some(User(authReq.userId))
  }
}
