package com.example.jun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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


    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchBox = findViewById(R.id.shop_editText_search);
        locationText = findViewById(R.id.shop_text_location);
    }

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


    // 장소 정보 호출 메소드
    public void showPlaceinformation(LatLng location)
    {
        mMap.clear(); // 지도 클리어


    }

    // onMapReady는 필수임
    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap;

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
}