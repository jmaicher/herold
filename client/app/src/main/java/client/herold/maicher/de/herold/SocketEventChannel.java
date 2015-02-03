package client.herold.maicher.de.herold;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
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
    private static final int RECONNECT_TIMEOUT = 500; //ms
    private static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.enableDefaultTyping();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    private Socket socket;
    private PrintStream out;

    @Override
    public void onCreate() {
        super.onCreate();
        connect();
        Log.d("SocketEventChannel", "onCreate");
    }

    private void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        Log.d("SocketEventChannel", "connecting");
                        socket = new Socket("192.168.0.100", 2020);
                        BufferedReader buffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        for (String line; (line = buffer.readLine()) != null; ) {
                            if(!line.isEmpty()) {
                                Log.d("SocketEventChannel", "Received: " + line);
                                SocketEvent event = null;
                                try {
                                    event = mapper.readValue(line, SocketEvent.class);
                                } catch (IllegalArgumentException e) {
                                    Log.e("SocketEventChannel", "Error deserializing event: " + e);
                                }

                                if (event != null) {
                                    onSocketEvent(event);
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e("SocketEventChannel", "Socket read error: " + e);
                    } finally {
                        socket = null;
                        out = null;
                        Log.d("SocketEventChannel", "Waiting "+RECONNECT_TIMEOUT+"ms and reconnecting.");
                        try{
                            Thread.sleep(RECONNECT_TIMEOUT);
                        }
                        catch(InterruptedException e) {}
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try{
                        if(socket != null) {
                            if(out == null) {
                                out = new PrintStream(socket.getOutputStream());
                            }
                            out.println("");
                            out.flush();
                            Log.d("SocketEventChannel", "Heartbeat ok");
                        }
                    }
                    catch (IOException e) {
                        Log.e("SocketEventChannel", "Socket write error: "+e);
                        out.close();
                    }

                    try {
                        Thread.sleep(2500);
                    }
                    catch (InterruptedException e) {
                        
                    }
                }
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
            out.close();
        }
    }

    private void sendString(String s) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SocketEventChannel", "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("SocketEventChannel", "onTaskRemoved");
        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);

        //Restart the service once it has been killed android
        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+RECONNECT_TIMEOUT, pi);
    }
}
