package com.example.jun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.location.Address;
import android.location.Geocoder;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity
    implements OnMapReadyCallback {

    private GoogleMap mMap;

    EditText searchBox;
    TextView locationText;

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

        if (previous_marker != null){
            previous_marker_clear(); // 지역정보 마커 클리어
        }

        new NRPlaces.Builder()
                .listenre


    }

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