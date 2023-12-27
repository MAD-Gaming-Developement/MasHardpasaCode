package dev.cdcd.katrnhlli.libs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.widget.Toast;

public class NetworkLibrary extends BroadcastReceiver {

    private static final String EXPECTED_ACTION = "dev.cdcd.katrnhlli.ACTION_NETWORK_STATUS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && EXPECTED_ACTION.equals(intent.getAction())) {
            if (isNetworkAvailable(context)) {
                Toast.makeText(context, "Established connection to MGD Servers", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Network is not available. MGD Servers not reachable.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Log or handle the case where the received Intent has an unexpected action
            Log.e("NetworkLibrary", "Received an Intent with unexpected action");
        }
    }

    public static  boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities networkCapabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            return networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }

        return false;
    }

}
