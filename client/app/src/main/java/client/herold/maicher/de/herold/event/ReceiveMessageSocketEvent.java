package client.herold.maicher.de.herold.event;

import java.util.Date;

public class ReceiveMessageSocketEvent extends SocketEvent {
    private String from;
    private String msg;
    private Date date;

    public ReceiveMessageSocketEvent() {
        date = new Date();
    }

    public ReceiveMessageSocketEvent(String from, String msg) {
        this.from = from;
        this.msg = msg;
        date = new Date();
    }

    public String getFrom() {
        return from;
    }

    public String getMsg() {
        return msg;
    }
}
