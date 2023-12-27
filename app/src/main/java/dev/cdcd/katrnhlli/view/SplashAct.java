package dev.cdcd.katrnhlli.view;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import dev.cdcd.katrnhlli.R;
import dev.cdcd.katrnhlli.libs.NetworkLibrary;

public class SplashAct extends AppCompatActivity {
    private NetworkLibrary networkReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(1);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_splash);

        networkReceiver = new NetworkLibrary();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (NetworkLibrary.isNetworkAvailable(this)) {
                Intent i = new Intent(this, GameAct.class);
                startActivity(i);
                finish();
            } else {
                Intent i = new Intent(this, NotifAct.class);
                startActivity(i);
                finish();
            }
        }, 1800);

    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }
}