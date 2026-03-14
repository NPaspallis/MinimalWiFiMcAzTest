package com.example.minimalwifimcaztest;

import static android.widget.Toast.LENGTH_SHORT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;

    private WifiRttManager mWifiRttManager;
    private MyRangingResultCallback myRangingResultCallback;

    private TextView textView;
    private String message = "WIFI MANAGER TEST\n\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.textView = findViewById(R.id.textView);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiScanReceiver = new WifiScanReceiver();
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        myRangingResultCallback = new MyRangingResultCallback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        textView.setText(this.message);
    }

    private void print(String message) {
        this.message += message + "\n";
        textView.setText(this.message);
    }

    public void startExperiment(View view) {
        this.message = "WIFI MANAGER TEST\n\n";
        textView.setText(this.message);
        print("*** EXPERIMENT STARTED ***");
        checkForPermissions();
        print("RTT available? " + mWifiRttManager.isAvailable());
        print("Starting WiFi scan...");

        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
    }

    public void endExperiment() {
        unregisterReceiver(mWifiScanReceiver);
        print("*** EXPERIMENT ENDED ***");
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            // count how man y have support for MC and how many for az
            int mc = 0, az = 0;
            List<ScanResult> mcOrAzScanResults = new Vector<>();
            for (ScanResult result : scanResults) {
                if (result.is80211mcResponder()) {
                    mc++;
                    mcOrAzScanResults.add(result);
                } else if(result.is80211azNtbResponder()) {
                    az++;
                    mcOrAzScanResults.add(result);
                }
            }
            print("Scan results: " + scanResults.size() + " of which " + mc + " support MC and " + az + " support AZ");
            if(mc>0 || az>0) {
                print("Starting RTT scan...");
                // create a list with a single AP -- choose an AZ access point if available, otherwise an MC
                final RangingRequest rangingRequest = new RangingRequest.Builder().addAccessPoints(mcOrAzScanResults).build();
                mWifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), myRangingResultCallback);
            } else {
                print("No responder found");
                endExperiment();
            }
        }
    }

    private class MyRangingResultCallback extends RangingResultCallback {

        @Override
        public void onRangingFailure(int code) {
            print("Ranging failure - code: " + code);
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            // handle RangingResults
            print("Ranging results: " + list.size());
            for(int i = 0; i < list.size(); i++) {
                RangingResult rangingResult = list.get(i);
                print("Result " + (i+1) +" - MC: " + rangingResult.is80211mcMeasurement() + ", AZ: " + rangingResult.is80211azNtbMeasurement()
                        + ", AP: " + rangingResult.getMacAddress() + ", distance (mm): " + rangingResult.getDistanceMm());
            }
            endExperiment();
        }
    }

    public static final int REQUEST_CODE_1 = 1001;
    public static final int REQUEST_CODE_2 = 1002;

    private void checkForPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied: " + Manifest.permission.ACCESS_FINE_LOCATION, LENGTH_SHORT).show();
            requestPermissions(
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES },
                    REQUEST_CODE_1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied: " + Manifest.permission.NEARBY_WIFI_DEVICES, LENGTH_SHORT).show();
            requestPermissions(
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES},
                    REQUEST_CODE_2);
        }
    }
}