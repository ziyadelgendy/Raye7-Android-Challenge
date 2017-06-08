package com.example.ziyadelgendy.raye7challenge;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 99;
    public static GoogleMap mMap;
    private Marker myMarker;
    private Location myLocation;
    private Location destination;
    private GoogleApiClient mGoogleApiClient;
    private LocationManager locationManager;
    private android.location.LocationListener locationListener;
    private Button searchButton;
    private Button changeType;
    private Button locToSrc;
    private EditText searchText;
    private TextView distTime;
    private float dist = 0;
    private boolean ontrip = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchText = (EditText) findViewById(R.id.searchText);
        distTime = (TextView) findViewById(R.id.distTime);
        searchButton = (Button) findViewById(R.id.searchButton);
        changeType = (Button) findViewById(R.id.changeType);
        locToSrc = (Button) findViewById(R.id.locToSrc);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                initCamera(location);
                myLocation = location;
                if (destination != null) {
                    LatLng my = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                    LatLng des = new LatLng(destination.getLatitude(), destination.getLongitude());
                    String url = getMapsApiDirectionsUrl(my, des);
                    ReadTask downloadTask = new ReadTask();
                    // Start downloading json data from Google Directions API
                    downloadTask.execute(url);
                    dist = distance((float) myLocation.getLatitude(), (float) myLocation.getLongitude(), (float) destination.getLatitude(), (float) destination.getLongitude());
                   checkDist(dist);
                    if(dist==0){
                        toast();
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }

        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET}, 99);
            }
            return;
        } else {
            locationManager.requestLocationUpdates("gps", 999999999, 30, locationListener);
        }
    }

   //this method is called automatic
    // it initialise new GoogleApiClient and connect it then initiate listeners and checks for permissions
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

        initListeners();


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    checks if the permission to access the user's current location is enabled
            return;
        }
        mMap.setMyLocationEnabled(true);
    }


    public void onChangeType(View view) {
        if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

    }
    //this method is called when the user clicks on current location button
    // it checks if the user is on trip or not and if yes it change the trip source
    //to the user's current location
    public void onLocToSrc(View view) {
        if (!(ontrip)) {
            Toast.makeText(this, "You are not on a trip right now", Toast.LENGTH_SHORT).show();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            myLocation = LocationServices
                    .FusedLocationApi
                    .getLastLocation(mGoogleApiClient);

            LatLng my=new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
            LatLng des=new LatLng(destination.getLatitude(),destination.getLongitude());
            String url = getMapsApiDirectionsUrl(my,des);
            ReadTask downloadTask = new ReadTask();
            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
            dist=distance((float)myLocation.getLatitude(),(float)myLocation.getLongitude(),(float)destination.getLatitude(),(float)destination.getLongitude());
            checkDist(dist);
        }
    }

    // this method takes the destination address and
    // calls getMapsDirectionsUrl then encode this url to get the directions
    //then calculate the distance and checks for it
    public void onStartTrip(View view) {
        if(destination==null){
            Toast.makeText(this, "Please choose a destination", Toast.LENGTH_SHORT).show();
        }else{
            LatLng my=new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
            LatLng des=new LatLng(destination.getLatitude(),destination.getLongitude());
            String url = getMapsApiDirectionsUrl(my,des);
            ReadTask downloadTask = new ReadTask();
            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
            mMap.setTrafficEnabled(true);
            dist=distance((float)myLocation.getLatitude(),(float)myLocation.getLongitude(),(float)destination.getLatitude(),(float)destination.getLongitude());
           checkDist(dist);
        }
    }

    //this method is responsible for getting the location of the chosen location
    public void onSearch(View view) {
        mMap.clear();
        String addressName = searchText.getText().toString();

        if (addressName != null && !(addressName.equals(""))) {
            Geocoder geocoder = new Geocoder(this);

            try {
                List<android.location.Address> addressList = geocoder.getFromLocationName(addressName, 1);
                android.location.Address address = addressList.get(0);
                LatLng latlng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.addMarker(new MarkerOptions().position(latlng).title("Marker at your chosen location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
                destination=new Location("Test");
                destination.setLongitude(address.getLongitude());
                destination.setLatitude(address.getLatitude());
            } catch (IOException e) {
                e.printStackTrace();
            }
            ontrip=false;
            distTime.setText("");
        }else{
            Toast.makeText(this, "Please enter your destination", Toast.LENGTH_SHORT).show();
        }
    }

    // this method initiate the listeners on the map
    private void initListeners() {
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    void toast(){
        Toast.makeText(this, "You reached your destination", Toast.LENGTH_SHORT).show();
    }

    //this method takes a location and animate the camera to make it's center the location parameter
    // and do some adjustments on the camera properties
    private void initCamera(Location location) {
        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(location.getLatitude(),
                        location.getLongitude()))
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();
        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), null);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
       // mMap.addMarker(new MarkerOptions().position(latlng).title("Marker at your current location"));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,15));
    }

    // this method is called when the connection is completed
    //it checks for the needed permissions then get the user's current location
    // and call initiateCamera on that location
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }


        myLocation = LocationServices
                .FusedLocationApi
                .getLastLocation(mGoogleApiClient);


        if (myLocation == null) {
            LocationRequest locationRequest = LocationRequest.create().setNumUpdates(1).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(0);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    myLocation = location;
                }
            });
        }
        initCamera(myLocation);
    }

    // If connection lost this method will be called
    // It shows toasts to notify the user that the connection is lost and what was the reason
    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed. Please check your network.", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        destination=new Location("Test");
        mMap.clear();
        MarkerOptions options = new MarkerOptions().position(latLng).title("Marker at your desired destination");
        options.icon(BitmapDescriptorFactory.defaultMarker());
        mMap.addMarker(options);
        destination.setLatitude(latLng.latitude);
        destination.setLongitude(latLng.longitude);
        ontrip=false;
        distTime.setText("");
    }

    //Convert LatLng to address
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this);

        String address = "";
        try {
            address = geocoder
                    .getFromLocation(latLng.latitude, latLng.longitude, 1)
                    .get(0).getAddressLine(0);
        } catch (IOException e) {
        }

        return address;
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    //Checks for Raye7 to access the GPS of the device
    //If permission was not accepted the application will terminate
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 99:
                if (grantResults.length > 0 && grantResults[0] == getPackageManager().PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                   // locationManager.requestLocationUpdates("gps",999999999,30,locationListener);
                }
        }
    }

    //Get URL which hold the directions from source point to destination point
    private String  getMapsApiDirectionsUrl(LatLng origin,LatLng dest) {
        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        return url;

    }
    // Calculate distance in Km
    private static float distance(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);
        return dist/1000;
    }

    private void checkDist(float distance){
        if(dist==0){
            destination=null;
            distTime.setText("");
            ontrip=false;
        }else {
            distTime.setText(dist + "Km left");
            ontrip = true;
        }
    }

}
