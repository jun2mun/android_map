package com.example.jun;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity
    implements OnMapReadyCallback {

    EditText searchBox;
    TextView locationText;

    private String TAG = "Tag";

    //
    private GoogleMap mMap;       //????????????, ???????????? ???????????? ????????? ????????? ??????.
    private CameraPosition mCameraPosition;    //1??? ??????????????? ???????????????, ??????????????? + 3d ???????????? ??? ??? ?????? ??????

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;     // ?????? Places API??? ???????????? ??????????????? ?????? ??????
    private PlaceDetectionClient mPlaceDetectionClient; //?????? Places API??? ???????????? <?????? ??????>??? ?????? ??????

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;  // ?????? Places API??? ????????????, <????????? ????????????>??? ?????? ??????

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(35.05148245, 126.72306776); //????????? ??????????????? ???, ????????? default ??????????????? ????????? <???????????? ??????>
    private static final int DEFAULT_ZOOM = 15;                // ?????? ?????? ??????
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1; // ????????????????????? ?????? ?????? ?????????, requestCode??? ???????????? ????????? ok
    private boolean mLocationPermissionGranted;                // ???????????????, ?????????????????? ????????? true??????

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;      // mFusedLocationProviderClient??? ?????? Places API??? ???????????? ??????????????? ??????Location????????? ?????? Location??????

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    // ??????????????? ?????? ??????????????? ?????? ?????? ?????? ??????, ?????? ??????????????? ???????????? onSaveInstanceState()??? ???????????? ????????????, ??????????????? ???????????? ?????????????????????
    // ????????? ????????? ????????????, ????????????????????? ?????? ??? ????????? ????????? ?????? ???????????????.

    private final LatLng defaultLocation = new LatLng(37.56, 126.97);

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;    // [??????]????????? ????????? ???, ?????????????????? ??????????????? ????????? ?????? <??????>????????????, ?????? ?????? ??? ????????? ?????? ??????/??????/??????/????????? ?????? ????????? ????????? ??????. ?????? ?????? ??????????????? ????????? int count??? ???????????? 5?????? ????????? count ?????? ??????????????? ???????????? ????????? ??????.
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs; // ??????????????? ????????? ?????? ??????

    //??????????????? ??????????????? ???, ??? ??? ?????? ?????????, ??????????????? ????????????/?????? ??????????????? ??????????????? ????????????. -> onCreate()????????? xml?????? ???????????? ?????? ??? ????????? ????????? ?????????.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);
        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // ?????? ??? fragment??? ?????????
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchBox = findViewById(R.id.shop_editText_search);
        locationText = findViewById(R.id.shop_text_location);
    }

    // onMapReady??? ?????????
    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap; // ????????????, ???????????? ???????????? ????????? ????????? ??????.

        mMap.addMarker(new MarkerOptions()
                .title("???????????? ????????????(??????)")
                .position(new LatLng(35.050917, 126.723083)));


        // mMap ??????????????? ????????? ?????????
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener(){
            @Override
            public boolean onMarkerClick(Marker marker){
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(marker.getPosition())
                        .zoom(17)
                        .bearing(90)
                        .tilt(30)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                //?????????????????? ??????, ??????-> ????????? ????????????, ????????????????????? showInfoWindow()??? ???????????? ???????????? ?????? ??????!!
                marker.showInfoWindow();

                return true;
            }
        });

        //Map????????? setInfoWindowAdapter()??? ????????????. ??? ????????? ???????????? 2?????? ?????????????????? ????????? ?????????,  getInfoWindow()??? getInfoContents()??? ??????.
        // ????????? layout ????????? ???????????? [custom_info_contents.xml]??? ??????????????? ????????? getInfoContents()??? ????????????, getInfoWindow?????? return null?????? ??????.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }


            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });



        getLocationPermission(); // ?????? ?????? ????????? ???, 1????????? ?????? ?????? ??????

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // 8. ???????????? ??????????????? ?????? ???????????? ???????????? ?????????
        getDeviceLocation();

    }


    ////////////////////  ?????? ?????? ?????? /////////////////////////
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    // ????????? ???????????? ????????? (
    private void getLocationPermission() {
        // ?????? ?????? ?????? ?????? true ??????
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED)
        {
            mLocationPermissionGranted = true;
        }
        // ???????????? ?????? ????????? ?????? ?????? ????????? ???????????? ???
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // ????????? ?????? ??? -> ?????? ??? ????????? ?????????
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    // ????????? ????????? ?????? ?????? ??????
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) { // ?????? ?????? O
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else { // ?????? ?????? X
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    // ?????????
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cur_place_menu, menu);
        return true;
    }
    //2-2????????????
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //21. ??????xml??? ??????????????? ???????????? ??????, ?????????????????? ??????.
        if (item.getItemId() == R.id.option_get_place) {
            showCurrentPlace();
        }
        return true;
    }

    private void showCurrentPlace() {
        if (mMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {


            //?????? ????????????????????? ??????.
/*            ArrayList<String> filters = new ArrayList<>();
            filters.add(Place.TYPE_ATM + "");
            filters.add(Place.TYPE_BANK + "");
            filters.add(Place.TYPE_BUS_STATION + "");
            filters.add("restaurant");
            filters.add("establishment");
            filters.add(Place.TYPE_STORE + "");

            PlaceFilter placeFilter = new PlaceFilter(false, filters);*/


            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission") final Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener
                    (new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                            String Tag = "TAG";
                            Log.d(Tag, String.valueOf(task.isSuccessful()));

                            if (task.isSuccessful() && task.getResult() != null) {
                                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();

                                Log.d(Tag, String.valueOf(likelyPlaces));

                                // Set the count, handling cases where less than 5 entries are returned.
                                int count;
                                if (likelyPlaces.getCount() < M_MAX_ENTRIES) {
                                    count = likelyPlaces.getCount();
                                } else {
                                    count = M_MAX_ENTRIES;
                                }

                                int i = 0;
                                mLikelyPlaceNames = new String[count];
                                mLikelyPlaceAddresses = new String[count];
                                mLikelyPlaceAttributions = new String[count];
                                mLikelyPlaceLatLngs = new LatLng[count];

                                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                                    // Build a list of likely places to show the user.
                                    mLikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
                                    mLikelyPlaceAddresses[i] = (String) placeLikelihood.getPlace()
                                            .getAddress();
                                    mLikelyPlaceAttributions[i] = (String) placeLikelihood.getPlace()
                                            .getAttributions();
                                    mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                                    i++;
                                    if (i > (count - 1)) {
                                        break;
                                    }
                                }

                                // Release the place likelihood buffer, to avoid memory leaks.
                                likelyPlaces.release();

                                // Show a dialog offering the user the list of likely places, and add a
                                // marker at the selected place.
                                openPlacesDialog();

                            }
                            else {
                                Log.e(TAG, "Exception: %s", task.getException());
                            }
                        }
                    });
        } else {
            // The user has not granted permission.
            String Tag = "TAG";
            Log.d(Tag,"error");
            // Add a default marker, because the user hasn't selected a place.
            mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));

            // Prompt the user for permission.
            getLocationPermission();
        }
    }

    private void openPlacesDialog() {
        //????????? ???????????? ???, 5??? ??????????????? ????????? ?????????, ?????????????????? ???????????? ???.
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item.
                LatLng markerLatLng = mLikelyPlaceLatLngs[which];
                String markerSnippet = mLikelyPlaceAddresses[which];
                if (mLikelyPlaceAttributions[which] != null) {
                    markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window
                // showing information about that place.
                mMap.addMarker(new MarkerOptions()
                        .title(mLikelyPlaceNames[which])
                        .position(markerLatLng)
                        .snippet(markerSnippet));



/*                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(markerLatLng)      // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .bearing(90)                // Sets the orientation of the camera to east
                        .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/

                // Position the map's camera at the location of the marker.
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                        DEFAULT_ZOOM));




            }
        };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                //.setTitle(R.string.pick_place)
                .setTitle("test")
                .setItems(mLikelyPlaceNames, listener)
                .show();
    }


    /// custom
    public void mOnClick(View v){

        // ??????????????? ???????????? ????????????.
        String searchText = searchBox.getText().toString();
        String TAG = "TAG"; String TAG2 = "TAG2"; String TAG3 = "TAG3";
        Log.d(TAG,searchText);
        Geocoder geocoder = new Geocoder(getBaseContext());
        List<Address> addresses = null;

        try {
            Log.d(TAG2, searchText);
            // ????????? ?????? ????????? ????????? ??????(v0 ??????)
            addresses = geocoder.getFromLocationName(
                    String.valueOf(searchText),1);

            if (addresses != null && !addresses.equals(" ")){
                search(addresses);
            }
        } catch (IOException e) {
            Log.d(TAG2,"err");
            e.printStackTrace();
        }
    };

    // ????????? ?????? ?????? ?????????
    protected void search(List<Address> addresses){
        Address address = addresses.get(0);
        LatLng latLng = new LatLng(address.getLatitude(),address.getLongitude());

        String addressText = String.format(
                "%s, %s",
                address.getMaxAddressLineIndex() > 0 ? address
                        .getAddressLine(0) : " ", address.getFeatureName());

        locationText.setVisibility(View.VISIBLE);
        locationText.setText("Latitude" + address.getLatitude() + "Longitude" + address.getLongitude() + "\n" + addressText);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(addressText);

        mMap.clear();
        mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

    }


    ////////////////////////////////////////////////////////////////////////////

    
}

