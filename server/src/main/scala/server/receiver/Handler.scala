package server.receiver

trait Handler {
  def handle(message: String)
}
