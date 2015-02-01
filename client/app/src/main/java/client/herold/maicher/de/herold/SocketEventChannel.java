package client.herold.maicher.de.herold;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import client.herold.maicher.de.herold.event.AuthRequestSocketEvent;
import client.herold.maicher.de.herold.event.AwaitingAuthSocketEvent;
import client.herold.maicher.de.herold.event.ReceiveMessageSocketEvent;
import client.herold.maicher.de.herold.event.SocketEvent;

public class SocketEventChannel extends Service {

    public static final String INTENT_SEND_EVENT = "de.maicher.herold.intent.send_event";
    public static final String INTENT_RECEIVED_EVENT = "de.maicher.herold.intent.received_event";
    private static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.enableDefaultTyping();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    private Socket socket;
    private PrintStream out;

    public SocketEventChannel() {
        connect();
        Log.d("SocketEventChannel", "Instantiated...");
    }

    private void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d("SocketEventChannel", "connecting");
                    socket = new Socket("10.0.2.1", 2020);
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    for (String line; (line = buffer.readLine()) != null;) {
                        Log.d("SocketEventChannel", "Received: "+line);
                        SocketEvent event = null;
                        try {
                            event = mapper.readValue(line, SocketEvent.class);
                        }
                        catch (IllegalArgumentException e) {
                            Log.e("SocketEventChannel", "Error deserializing event: "+e);
                        }

                        if(event != null) {
                            onSocketEvent(event);
                        }
                    }
                }
                catch (IOException e) {
                    Log.e("SocketEventChannel", "Socket read error: " + e);
                }
                finally {
                    try{
                        socket.close();
                    }
                    catch (IOException e) { }
                    socket = null;
                }
                Log.d("SocketEventChannel", "Stopped.");
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onSocketEvent(SocketEvent event) {
        if(event instanceof AwaitingAuthSocketEvent) {
            sendSocketEvent(new AuthRequestSocketEvent("david", "foo"));
        }
        else if(event instanceof ReceiveMessageSocketEvent) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle("New message from "+((ReceiveMessageSocketEvent) event).getFrom())
                            .setContentText(((ReceiveMessageSocketEvent) event).getMsg());

            NotificationManager mNotifyMgr =  (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(1, mBuilder.build());
        }
    }

    private void sendSocketEvent(SocketEvent event) {
        try{
            if(out == null) {
                out = new PrintStream(socket.getOutputStream());
            }
            out.println(mapper.writeValueAsString(event));
            out.flush();
        }
        catch (IOException e) {
            Log.e("SocketEventChannel", "Socket write error: "+e);
            try{
                socket.close();
                out.close();
            }
            catch (IOException e2) { }
        }
    }
}
