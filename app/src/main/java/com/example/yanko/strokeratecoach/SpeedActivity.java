
/*
 * The code below was taken from StackOverflow user ISURU
 * (https://stackoverflow.com/users/3276345/isuru)
 * as an answer to a question
 * (https://stackoverflow.com/questions/15570542/determining-the-speed-of-a-vehicle-using-gps-in-android)
 * by StackOverflow user John Kulova
 * https://stackoverflow.com/users/1550867/john-kulova
 * The code is licensed under CC BY-SA 3.0 ( http://creativecommons.org/licenses/by-sa/3.0/ ).
 *
 * Minor modifications: 2017 Yanko Georgiev
 */


package com.example.yanko.strokeratecoach;

import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

//public class SpeedActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_speed);
//    }
//}

import java.util.Formatter;
import java.util.Locale;

import android.location.Location;
import android.location.LocationManager;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.example.yanko.strokeratecoach.Speed.CLocation;
import com.example.yanko.strokeratecoach.Speed.IBaseGpsListener;

public class SpeedActivity extends AppCompatActivity implements IBaseGpsListener {

    private static final int MY_LOCATION_PERMISSION = 22;
    ToneGenerator toneGen2;
    TextView txtCurrentSpeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed);
        txtCurrentSpeed = (TextView) this.findViewById(R.id.txtCurrentSpeed);

        PackageManager manager = getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION", "com.example.yanko.strokeratecoach");
        boolean hasPermission = (permission == manager.PERMISSION_GRANTED);
//
        if (!hasPermission) {
            Log.d("I CAN HAZ PERMISSION?", "NO!");
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_LOCATION_PERMISSION);
        }

        //TODO: Handle wait until user gives us permission
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 0, this);

        this.updateSpeed(null);

////        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

//        CheckBox chkUseMetricUntis = (CheckBox) this.findViewById(R.id.chkMetricUnits);
//        chkUseMetricUntis.setOnCheckedChangeListener(new OnCheckedChangeListener() {

//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                // TODO Auto-generated method stub
//                SpeedActivity.this.updateSpeed(null);
//            }
        toneGen2 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    }

    public void finish() {
        super.finish();
        System.exit(0);
    }

    private void updateSpeed(CLocation location) {
        // TODO Auto-generated method stub
        float nCurrentSpeed = 0;

        if (location != null) {
            location.setUseMetricunits(true);
            nCurrentSpeed = location.getSpeed();
        }

        Formatter fmt = new Formatter(new StringBuilder());

        //Get minutes:seconds per 500m
//        long secsPer500;
//
//        if (nCurrentSpeed != 0) {
//            secsPer500 = 500 / (long)nCurrentSpeed;
//        } else secsPer500 = 0;
//
//
//        String secsPer500String = String.format(Locale.US, "%02d min, %02d sec",
//                TimeUnit.SECONDS.toMinutes(secsPer500),
//                secsPer500 -
//                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(secsPer500)));

        //Format for m/s
        fmt.format(Locale.US, "%5.1f", nCurrentSpeed);
        String strCurrentSpeed = fmt.toString();
        strCurrentSpeed = strCurrentSpeed.replace(' ', '0');

        //Setup units
//        String strUnits = "miles/hour";
//        if (this.useMetricUnits()) {
//            strUnits = "meters/second";
//        }

        txtCurrentSpeed.setText(strCurrentSpeed);
    }

    private boolean useMetricUnits() {
        // TODO Auto-generated method stub
//        CheckBox chkUseMetricUnits = (CheckBox) this.findViewById(R.id.chkMetricUnits);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        Log.d("Location changed? ", "Yeeees");
        toneGen2.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
        if (location != null) {
            CLocation myLocation = new CLocation(location, this.useMetricUnits());
            this.updateSpeed(myLocation);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGpsStatusChanged(int event) {
        // TODO Auto-generated method stub

    }


}
