package com.example.jun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

    //
    private GoogleMap mMap;       //화면이동, 마커달기 등등으로 쓰이는 구글맵 변수.
    private CameraPosition mCameraPosition;    //1번 게시물에서 사용했었던, 좌표로이동 + 3d 효과까지 줄 수 있는 변수

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;     // 구글 Places API에 접근해서 지역정보를 얻는 변수
    private PlaceDetectionClient mPlaceDetectionClient; //구글 Places API에 접근해서 <현재 위치>를 얻는 변수

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;  // 구글 Places API에 접근해서, <융합된 주위정보>를 얻는 변수

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(35.05148245, 126.72306776); //인터넷 연결안됬을 때, 연결된 default 시작장소의 좌표값 <수정해서 사용>
    private static final int DEFAULT_ZOOM = 15;                // 줌의 정도 상수
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1; // 위치정보사용에 대한 동의 상수로, requestCode랑 비교해서 같으면 ok
    private boolean mLocationPermissionGranted;                // 불린형으로, 위치정보사용 동의시 true대입

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;      // mFusedLocationProviderClient에 의해 Places API에 연결되어 현재위치의 주위Location정보를 담는 Location변수

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    // 액티비티가 잠시 중지되어서 다시 켜질 때를 위해, 값을 저장할라고 호출되는 onSaveInstanceState()에 사용되는 키값으로, 파라미터로 넘어오는 번들객체에다가
    // 번들의 밸류인 현재좌표, 현재위치정보를 담을 때 사용될 번들의 키값 상수들이다.


    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;    // [메뉴]버튼을 눌렀을 때, 현재위치에서 좋아할만한 장소를 띄울 <최대>갯수로서, 아래 있는 각 장소에 대한 이름/주소/속성/좌표의 최대 갯수가 되기도 한다. 만약 내가 좋아할만한 장소가 int count랑 비교해서 5개가 안되면 count 만큼 띄워지도록 비교하는 문장이 있다.
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs; // 좋아할만한 장소에 대한 값들

    //액티비티가 중지되었을 때, 맵 만 살아 있으면, 맵변수에서 좌표정보/위치 주변정보를 번들객체에 담아둔다. -> onCreate()호출시 xml화면 보여주기 전에 이 값들을 회수할 것이다.
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

        // 구글 맵 fragment에 띄우기
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchBox = findViewById(R.id.shop_editText_search);
        locationText = findViewById(R.id.shop_text_location);
    }

    // onMapReady는 필수임
    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap; // 화면이동, 마커달기 등등으로 쓰이는 구글맵 변수.

        mMap.addMarker(new MarkerOptions()
                .title("나주농협 송현지점(하행)")
                .position(new LatLng(35.050917, 126.723083)));


        // mMap 마커클릭시 이벤트 리스너
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

                //클릭리스너를 달면, 클릭-> 정보창 안떴는데, 클릭리스너안에 showInfoWindow()를 호출하면 정보창도 같이 뜬다!!
                marker.showInfoWindow();

                return true;
            }
        });

        //Map에다가 setInfoWindowAdapter()를 달아준다. 그 정보창 어댑터는 2가지 인터페이스를 구현화 하는데,  getInfoWindow()와 getInfoContents()가 있다.
        // 우리는 layout 폴더에 추가해준 [custom_info_contents.xml]를 화면으로써 띄우는 getInfoContents()를 사용하고, getInfoWindow에는 return null처리 한다.
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



        getLocationPermission(); // 맵이 준비 되었을 때, 1번째로 호출 하는 함수

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // 8. 핸드폰의 현재위치를 맵의 포지션에 지정하는 매쏘드
        //getDeviceLocation();

        LatLng SEOUL = new LatLng(37.56, 126.97);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(SEOUL);
        markerOptions.title("서울");
        markerOptions.snippet("한국의 수도");
        mMap.addMarker(markerOptions);


        // 기존에 사용하던 다음 2줄은 문제가 있습니다.
        // CameraUpdateFactory.zoomTo가 오동작하네요.
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SEOUL, 10));

    }

    ////////////////////  권한 확인 확인 /////////////////////////

    // 퍼미션 확인하는 메소드
    private void getLocationPermission() 
    {
        // 허가 되어 있는 경우 true 할당
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED)
        {
            mLocationPermissionGranted = true;
        }
        // 위치사용 허가 안되어 있을 경우 허가를 물어보는 창
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // 퍼미션 요청 창 -> 클릭 시 메소드 실행됨
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

    //////////////////////////////////////////////////////////

    public void mOnClick(View v){

        // 검색창에서 텍스트를 가져온다.
        String searchText = searchBox.getText().toString();
        String TAG = "TAG"; String TAG2 = "TAG2"; String TAG3 = "TAG3";
        Log.d(TAG,searchText);
        Geocoder geocoder = new Geocoder(getBaseContext());
        List<Address> addresses = null;

        try {
            Log.d(TAG2, searchText);
            // 현재는 영어 검색만 되도록 해둠(v0 버전)
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

    // 구글맵 주소 검색 메서도
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



    // 자신의 위치로 가는 버튼 표시
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) { // 권한 허용 O
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else { // 권한 허용 X
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    // 메뉴바
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cur_place_menu, menu);
        return true;
    }
    //2-2메뉴추가
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //21. 메뉴xml을 깃허브에서 복사해온 뒤에, 메뉴달아주는 코드.
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


            //필터 넣으려해봤지만 안됨.
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
                            if (task.isSuccessful() && task.getResult() != null) {
                                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();

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

                            } else {
                            }
                        }
                    });
        } else {
            // The user has not granted permission.

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
        //메뉴가 눌러졌을 때, 5개 좋아할만한 장소를 띄우고, 클릭리스너를 처리하는 곳.
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
}

