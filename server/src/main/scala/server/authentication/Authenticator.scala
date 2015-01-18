package server.authentication

import server.messages.AuthRequest

class Authenticator {
  def authenticate(msg: AuthRequest): Boolean = {
    true
  }
}
