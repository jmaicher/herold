package common.event

import com.typesafe.scalalogging.LazyLogging

class EventDispatcher[BaseEvent] extends LazyLogging {
  private var listeners = Map[Class[_ <: BaseEvent], List[EventListener[_ <: BaseEvent]]]()

  def register[E <: BaseEvent](c: Class[E], l: EventListener[E]): Unit = {
    listeners += (c -> {
      listeners.get(c) match {
        case Some(list) => if(list.contains(l)) list else list :+ l
        case None => List(l)
      }
    })
  }

  def unregister(l: EventListener[_ <: BaseEvent]): Unit = {
    listeners foreach { p =>
      if(p._2.contains(l)) {
        listeners += (p._1 -> p._2.diff(List(l)))
      }
    }
  }

  def dispatch[E <: BaseEvent](e: E): Unit = {
    logger.debug("Dispatching: "+e)
    var c: Class[BaseEvent] = e.getClass.asInstanceOf[Class[BaseEvent]]
    while(c != null) {
      listeners.get(c) map { _.foreach({
        _.asInstanceOf[EventListener[E]].on(e)
      })}
      c = c.getSuperclass.asInstanceOf[Class[BaseEvent]]
    }
  }
}

trait EventListener[BaseEvent] {
  def on(event: BaseEvent): Unit
}