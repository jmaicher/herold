package client.herold.maicher.de.herold;

import android.app.Application;
import android.content.Intent;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        sendBroadcast(new Intent("de.maicher.herold.intent.start_socket_event_channel"));
    }
}
