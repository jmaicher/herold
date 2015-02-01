package client

import java.net.{InetAddress, Socket}
import java.text.SimpleDateFormat
import java.util.{Date, Scanner}
import java.util.concurrent.{Executors, ExecutorService}

import common.SocketEventChannel
import common.event.socket._
import com.typesafe.scalalogging.LazyLogging
import common.event.{EventListener, EventDispatcher}
import server.event._

import scala.util.Random

object Client {
  def main(args: Array[String]) {
    val pool: ExecutorService = Executors.newFixedThreadPool(10000)
    /*
    val userLimit = 10
    for(i <- Range(0,userLimit)) {
      pool.execute(new NonInteractiveTestClient(i.toString, "", Random.nextInt(userLimit).toString))
    }
    */
    pool.execute(new InteractiveTestClient)
  }
}

abstract class Client extends Runnable with LazyLogging {
  logger.debug("connecting...")
  protected val eventDispatcher = new EventDispatcher[SocketEvent]
  protected val socketEventChannel = new SocketEventChannel(new Socket(System.getProperty("herold.host", "localhost"), 2020))
}

class NonInteractiveTestClient(val username: String, val password: String, val to: String) extends Client {
  override def run(): Unit = {
    eventDispatcher.register(classOf[SocketEvent], new EventListener[SocketEvent] {
      override def on(event: SocketEvent): Unit = event match {
        case e: AwaitingAuthSocketEvent => socketEventChannel.send(new AuthRequestSocketEvent(username, password))
        case e: AuthRequestSocketEvent => socketEventChannel.send(e)
        case e: AuthFailedSocketEvent =>
        case e: AuthSuccessSocketEvent => sendRandomMessageWithDelay()
        case e: ReceiveMessageSocketEvent => sendRandomMessageWithDelay()
        case e: SendMessageSocketEvent => socketEventChannel.send(e)
        case _ =>
      }
    })
    socketEventChannel.listen(eventDispatcher)
  }

  private def sendRandomMessageWithDelay(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        Thread.sleep(math.max(250,Random.nextInt(1000)))
        eventDispatcher.dispatch(new SendMessageSocketEvent(to, java.util.UUID.randomUUID.toString))
      }
    }).start()
  }
}

class InteractiveTestClient extends Client {
  private val scanner = new Scanner(System.in)

  override def run(): Unit = {
    eventDispatcher.register(classOf[AuthSocketEvent], new EventListener[AuthSocketEvent] {
      override def on(event: AuthSocketEvent): Unit = event match {
        case e: AwaitingAuthSocketEvent => requestAuthentication("Please authenticate...")
        case e: AuthRequestSocketEvent => socketEventChannel.send(e)
        case e: AuthFailedSocketEvent => requestAuthentication(e.msg)
        case e: AuthSuccessSocketEvent =>
          println("Successfully authenticated")
          startChat(e.username)
        case _ =>
      }
    })
    socketEventChannel.listen(eventDispatcher)
  }

  def requestAuthentication(msg: String): Unit = {
    println(msg)
    val username = getStringInput("Please enter username")
    val password = getStringInput("Please enter password")
    eventDispatcher.dispatch(new AuthRequestSocketEvent(username, password))
  }

  def startChat(username: String): Unit = {

    eventDispatcher.register(classOf[MessageSocketEvent], new EventListener[MessageSocketEvent] {
      def on(event: MessageSocketEvent): Unit = event match {
        case e: ReceiveMessageSocketEvent => printMessage(e.from, e.msg, e.date)
        case e: SendMessageSocketEvent => socketEventChannel.send(e)
        case _ =>
      }
    })

    println("Write a message to someone with @USERNAME:MESSAGE")

    new Thread(new Runnable {
      override def run(): Unit = {
        while(true) {
          val message = getStringInput()
          val parts = message.split(":", 2)
          if(parts.length == 2 && parts(0).startsWith("@") && !parts(1).trim.isEmpty) {
            val to = parts(0).replaceFirst("@", "")
            val msg = parts(1).trim
            if(to.equals(username)) {
              println("Does not make much sense to send a message to yourself? ;)")
            }
            else {
              printMessage(username, msg, new Date())
              eventDispatcher.dispatch(new SendMessageSocketEvent(to, msg))
            }
          }
          else {
            println("Unknown command!")
          }
        }
      }
    }).start()
  }

  def printMessage(name: String, msg: String, date: Date): Unit = {
    lazy val dateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss")
    println("["+dateFormat.format(date)+"] "+name+": "+msg)
  }

  def getStringInput(msg: String = ""): String = {
    var in = ""
    while(in.isEmpty) {
      if(!msg.isEmpty)
        println(msg)
      in = scanner.nextLine()
    }
    print("\033[1A")
    print("\033[2K")
    in
  }
}
