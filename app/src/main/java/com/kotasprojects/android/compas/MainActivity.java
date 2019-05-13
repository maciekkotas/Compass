package com.kotasprojects.android.compas;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public LocationManager locationManager;
    public LocationListener locationListener;
    float currentDegree = 0f;
    float bearing = 0f;
    float position = 0f;
    private TextView longitudeTv, degreeTv, distanceTV, desLatTv, desLngTv, latitudeTv;
    private ImageView compassIv, arrowIv, desIv;
    private FloatingActionButton fab;
    private String latString, lngString;
    private Double lat, lng;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        distanceTV = findViewById(R.id.distanceTv);
        longitudeTv = findViewById(R.id.longitudeTv);
        latitudeTv = findViewById(R.id.latitudeTv);
        degreeTv = findViewById(R.id.degreeTv);
        compassIv = findViewById(R.id.compassIv);
        arrowIv = findViewById(R.id.arrowIv);
        desLatTv = findViewById(R.id.desLatTv);
        desLngTv = findViewById(R.id.desLngTv);
        desIv = findViewById(R.id.desIv);
        fab = findViewById(R.id.floatingActionButton);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            getPosition();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ContentResolver contentResolver = getBaseContext()
                            .getContentResolver();
                    boolean gpsStatus = Settings.Secure
                            .isLocationProviderEnabled(contentResolver,
                                    LocationManager.GPS_PROVIDER);
                    if (gpsStatus) {
                        getPosition();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.localisation_denied), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    /**
     * distance between to points
     **/
    public void getPosition() {

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (loc != null) {
            setLocalisation(loc);
        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                setLocalisation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);

        if (loc == null) {
            Toast.makeText(this, getString(R.string.gps_lost), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float degrees = Math.round(event.values[0]);
        degreeTv.setText(String.valueOf((int) degrees) + cardinalDirection((int) degrees));
        RotateAnimation rotateAnimation = new RotateAnimation(currentDegree, -degrees, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(210);
        rotateAnimation.setFillAfter(true);
        RotateAnimation arrowAnimation = new RotateAnimation(position, -degrees, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        arrowAnimation.setDuration(210);
        arrowAnimation.setFillAfter(true);
        compassIv.startAnimation(rotateAnimation);
        arrowIv.startAnimation(arrowAnimation);
        currentDegree = -degrees;
        position = -degrees + bearing;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void addDestination(Double destLat, Double destLng) {
        distanceTV.setText(String.valueOf((int) Compass.distance(lat, lng, destLat, destLng)) + "m");
        bearing = (float) Compass.bearing(lat, lng, destLat, destLng);
        position = bearing;
        if (bearing != 0) {
            arrowIv.setVisibility(View.VISIBLE);
        }
    }

    public void setLocalisation(Location location) {
        lat = location.getLatitude();
        lng = location.getLongitude();
        latitudeTv.setText(String.valueOf(lat));
        longitudeTv.setText(String.valueOf(lng));
    }

    public void createDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.destination_dialog, null);
        dialog.setContentView(dialogView);
        dialog.setTitle("Your destination");
        final EditText latitude = dialogView.findViewById(R.id.edittext_dialog_lat);
        final EditText longitude = dialogView.findViewById(R.id.edittext_dialog_lng);
        Button positiveBtn = dialogView.findViewById(R.id.button_dialog_positive);
        Button negativeBtn = dialogView.findViewById(R.id.button_dialog_negative);
        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean canCloseone = false;
                Boolean canCloseTwo = false;
                latString = latitude.getText().toString();
                lngString = longitude.getText().toString();
                if (latString.length() != 0 && lngString.length() != 0) {
                    showDestination(View.VISIBLE);
                    double destLat = Double.parseDouble(latString);
                    double destLng = Double.parseDouble(lngString);
                    if (destLat >= -90.0 && destLat <= 90.0) {
                        desLatTv.setText(latString);
                        canCloseone = true;
                    } else {
                        latitude.setError(getString(R.string.lat_range));
                    }
                    if (destLng >= -180.0 && destLng <= 180.0) {
                        desLngTv.setText(lngString);
                        canCloseTwo = true;
                    } else {
                        longitude.setError(getString(R.string.lng_range));
                    }
                    addDestination(destLat, destLng);
                    if (canCloseone && canCloseTwo) {
                        dialog.dismiss();
                    }
                }
            }

        });
        negativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                latString = null;
                lngString = null;
                showDestination(View.GONE);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void showDestination(int visibility) {
        desIv.setVisibility(visibility);
        arrowIv.setVisibility(visibility);
        desLatTv.setVisibility(visibility);
        desLngTv.setVisibility(visibility);
        distanceTV.setVisibility(visibility);

    }

    public String cardinalDirection(int degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(degrees / 45) % 8];
    }
}
