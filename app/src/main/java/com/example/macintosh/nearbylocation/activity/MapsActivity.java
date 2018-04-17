package com.example.macintosh.nearbylocation.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.macintosh.nearbylocation.Common;
import com.example.macintosh.nearbylocation.R;
import com.example.macintosh.nearbylocation.model.nearby.MyPlace;
import com.example.macintosh.nearbylocation.model.nearby.Results;
import com.example.macintosh.nearbylocation.remote.IGoogleApiService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationManager mLocationManager;
    private Marker mMarker;
    private LocationRequest mLocationRequest;

    private static final int LOCATION_PERMISSION = 100;
    IGoogleApiService mService;

    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Initial Service
        mService = Common.getGoogleApiService();

        //Request Runtime Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        findViewById(R.id.btnNearby).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nearbyPlace("hospital");
            }
        });
    }

    private void nearbyPlace(final String placeType) {
        mMap.clear();
        String url = getUrl(latitude, longitude, placeType);

        mService.getNearbyPlaces(url).enqueue(new Callback<MyPlace>() {
            @Override
            public void onResponse(Call<MyPlace> call, Response<MyPlace> response) {
                if (response.isSuccessful()) {
                    for (int i = 0; i < response.body().getResults().length; i++) {
                        MarkerOptions markerOptions = new MarkerOptions();
                        Results googlePlace = response.body().getResults()[i];
                        double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                        double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());
                        String placeName = googlePlace.getName();
                        String vicinity = googlePlace.getVicinity();
                        LatLng latLng = new LatLng(lat, lng);
                        markerOptions.position(latLng);
                        markerOptions.title(vicinity);
//                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round));

                        mMap.addMarker(markerOptions);
                    }

                    LatLng latLng = new LatLng(latitude, longitude);
                    //Move Camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                }
            }

            @Override
            public void onFailure(Call<MyPlace> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private String getUrl(double latitude, double longitude, String placeType) {
        StringBuilder googlePlaceUrl = new StringBuilder(Common.GOOGLE_API_URL);
        googlePlaceUrl.append("place/nearbysearch/json?");
        googlePlaceUrl.append("location=" + latitude + "," + longitude);
        googlePlaceUrl.append("&radius=" + 1000);
        googlePlaceUrl.append("&type=" + placeType);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key=" + getResources().getString(R.string.browser_key));

        Log.d("getUrl", googlePlaceUrl.toString());
        return googlePlaceUrl.toString();
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, LOCATION_PERMISSION);
                return true;
            }else{
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, LOCATION_PERMISSION);
                return false;
            }
        }else {
            return true;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Initial Google Play Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }else{
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        getLastLocation();
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    mLastLocation = locationResult.getLastLocation();
                    latitude = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();
                }
            }, Looper.myLooper());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void getLastLocation() {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (mLastLocation == null) {
                        mLastLocation = location;


                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        //Move Camera
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /*
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mMarker != null) {
            mMarker.remove();
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                            mMap.setMyLocationEnabled(true);
                        }
                    }
                }else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
