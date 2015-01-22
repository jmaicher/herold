package server.authentication

case class User(id: Int)

class Authenticator {
  def authenticate(): Option[User] = {
    None
  }
}
