import common.event.{EventListener, EventDispatcher}
import org.scalatest._
import org.scalamock.scalatest._

class EventDispatcherTestSpec extends FlatSpec with Matchers with MockFactory {

  "EventDispatcher" should "dispatch to listener" in {
    val ed = new EventDispatcher[TestEvent]
    val listener = mock[EventListener[TestEvent]]
    ed.register(classOf[TestEvent], listener)

    val e1 = new TestEvent
    listener.on _ expects(e1)
    ed.dispatch(e1)

    val e2 = new MoreSpecificTestEvent
    listener.on _ expects(e2)
    ed.dispatch(e2)

    val e3 = new MostSpecificTestEvent
    listener.on _ expects(e3)
    ed.dispatch(e3)

    ed.unregister(listener)
    listener.on _ expects(*) never()
    ed.dispatch(e1)
  }
}

class TestEvent
class MoreSpecificTestEvent extends TestEvent
class MostSpecificTestEvent extends MoreSpecificTestEvent