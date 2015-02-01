package client.herold.maicher.de.herold;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SocketEventChannelStarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, SocketEventChannel.class));
    }
}
