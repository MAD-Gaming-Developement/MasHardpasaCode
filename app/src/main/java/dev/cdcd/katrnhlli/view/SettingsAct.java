package dev.cdcd.katrnhlli.view;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import dev.cdcd.katrnhlli.R;

public class SettingsAct extends AppCompatActivity {

    private static final String PREFS_NAME = "MGD_ABClientV1";
    private static final String KEY_ADS_PERMISSION = "adsPermission";
    private static final String KEY_LOCATION_PERMISSION = "locationPermission";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final int MY_LOCATION_PERMISSION_REQUEST = 188188;

    final SwitchCompat notifySwitch = findViewById(R.id.permissionNotifications);
    final SwitchCompat locationSwitch = findViewById(R.id.permissionLocation);
    final SwitchCompat adsSwitch = findViewById(R.id.permissionADs);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences prefs;

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        notifySwitch.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, false));
        locationSwitch.setChecked(prefs.getBoolean(KEY_LOCATION_PERMISSION, false));
        adsSwitch.setChecked(prefs.getBoolean(KEY_ADS_PERMISSION, false));

        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_LOCATION_PERMISSION, isChecked).apply();
            applyLocationPermission();
        });

        notifySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            if (isChecked) {
                // Enable post notifications
                enablePostNotifications();
            } else {
                // Disable post notifications
                disablePostNotifications();
            }
        });

        adsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_ADS_PERMISSION, isChecked).apply();
            if (isChecked) {
                // Enable post notifications
                enableADs();
            } else {
                // Disable post notifications
                disableADs();
            }

        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(KEY_ADS_PERMISSION, adsSwitch.isChecked())
                    .putBoolean(KEY_LOCATION_PERMISSION, locationSwitch.isChecked())
                    .putBoolean(KEY_NOTIFICATIONS, notifySwitch.isChecked())
                    .apply();

            applyLocationPermission();

            if (notifySwitch.isChecked()) {
                enablePostNotifications();
            } else {
                disablePostNotifications();
            }

            if (notifySwitch.isChecked()) {
                enableADs();
            } else {
                disableADs();
            }

            finish();
        });
    }


    private void applyLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        // Check if the user denied the location permission previously
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationPermissionRationaleDialog();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_LOCATION_PERMISSION_REQUEST);
        }
    }

    private void showLocationPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app requires location permission to function properly.")
                .setPositiveButton("Grant", (dialog, which) -> ActivityCompat.requestPermissions(SettingsAct.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_LOCATION_PERMISSION_REQUEST))
                .setNegativeButton("Deny", (dialog, which) -> Toast.makeText(SettingsAct.this, "Location permission denied", Toast.LENGTH_SHORT).show())
                .show();
    }

    private void enablePostNotifications() {
        // Add logic to enable post notifications (e.g., register with a push notification service)
        // This will depend on your specific implementation for handling notifications.
    }

    private void disablePostNotifications() {
        // Add logic to disable post notifications (e.g., unregister from the push notification service)
        // This will depend on your specific implementation for handling notifications.
    }

    private void enableADs() {
        // Enable Ads
        // Implement
    }

    private void disableADs() {
        //Disable Ads
        //Implement
    }


    @Deprecated
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_LOCATION_PERMISSION_REQUEST) {
            handleLocationPermissionResult(grantResults);
        }
    }

    private void handleLocationPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationSwitch.setChecked(true);
        } else {
            locationSwitch.setChecked(false);
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}