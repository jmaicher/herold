package client.herold.maicher.de.herold.event;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@type")
abstract public class SocketEvent {
    protected String uuid = java.util.UUID.randomUUID().toString();
}



class AuthFailedSocketEvent extends AuthSocketEvent {
   public String msg;
}

abstract class MessageSocketEvent extends SocketEvent {}
class SendMessageSocketEvent extends MessageSocketEvent {
    public String to;
    public String msg;
    public Date date = new Date();
}