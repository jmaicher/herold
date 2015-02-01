package client.herold.maicher.de.herold.event;

public class AuthRequestSocketEvent extends AuthSocketEvent {
    protected String username;
    protected String password;

    public AuthRequestSocketEvent() {

    }

    public AuthRequestSocketEvent(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
