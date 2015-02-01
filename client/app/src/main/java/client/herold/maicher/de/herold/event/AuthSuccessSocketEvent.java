package client.herold.maicher.de.herold.event;

public class AuthSuccessSocketEvent extends AuthSocketEvent {
    protected String username;

    public AuthSuccessSocketEvent() {

    }

    public AuthSuccessSocketEvent(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
