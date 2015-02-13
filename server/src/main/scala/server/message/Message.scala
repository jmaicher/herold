package server.message

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver
import com.fasterxml.jackson.databind.util.ClassUtil

class MessageTypeResolver extends TypeIdResolver {
  private val PACKAGE = classOf[Message].getPackage.getName
  var baseType: JavaType = null
  override def init(javaType: JavaType): Unit = baseType = javaType
  override def idFromBaseType(): String = idFromValueAndType(null, baseType.getRawClass)
  override def idFromValue(o: scala.Any): String = idFromValueAndType(o, o.getClass)
  override def getMechanism: Id = Id.CUSTOM
  override def typeFromId(s: String): JavaType = {
    val className = PACKAGE + "." + s
    try {
      val cls = ClassUtil.findClass(className)
      TypeFactory.defaultInstance().constructSpecializedType(baseType, cls)
    } catch {
      case e: ClassNotFoundException => throw new IllegalArgumentException("cannot find class '" + className + "'")
    }
  }
  override def idFromValueAndType(o: scala.Any, aClass: Class[_]): String = {
    val name = aClass.getName
    if (name.startsWith(PACKAGE)) {
      return name.substring(PACKAGE.length()+1)
    }
    throw new IllegalArgumentException("class " + name + " is not in the package " + PACKAGE)
  }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonTypeIdResolver(classOf[MessageTypeResolver])
abstract class Message {
  val uuid = java.util.UUID.randomUUID.toString
}

class SendMessage(var body: String) extends Message
class ReceiveMessage(var fromUserId: Int, var body: String) extends Message
