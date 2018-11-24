package com.playgilround.schedule.client.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.playgilround.schedule.client.R;
import com.playgilround.schedule.client.activity.MainActivity;

import java.io.IOException;
import java.util.List;

/**
 * 18-07-05
 * 위치 설정 다이얼로그
 */
public class InputLocationDialog extends Activity implements View.OnClickListener, OnMapReadyCallback,
        MaterialSearchBar.OnSearchActionListener {
    String resLocation;
    static final String TAG = InputLocationDialog.class.getSimpleName();

    double latitude;
    double longitude;

    double scheLatitude; //위도
    double scheLongitude; //경도
    String scheLocation;

    ProgressDialog progress;

    private MaterialSearchBar searchBar;
    private boolean isInit = true;
    private boolean isSearch = true;
    private boolean isLocation = false; //최초만.

    Double resLatitude;
    Double resLongitude;
    private Geocoder geocoder;
    private GoogleMap mMap;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_input_location);

        Intent intent = getIntent();
        scheLatitude = intent.getDoubleExtra("latitude", 0);
        scheLongitude = intent.getDoubleExtra("longitude", 0);
        scheLocation = intent.getStringExtra("location");

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            //Gps 제공자의 정보가 바뀌면 콜백하도록 리스너 등록하기
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, mLocationListener);

            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 1, mLocationListener);
            Log.d(TAG, "Request Location Updates");
            progress = new ProgressDialog(this);
            progress.setCanceledOnTouchOutside(false);
            progress.setTitle("위치");

            if (scheLatitude == 0.0 && scheLongitude == 0.0) {
                progress.setMessage("계신 곳에 위치를 탐색 중입니다.");
            } else {
                progress.setMessage("설정 된 위치로 표시됩니다.");
            }
            progress.show();

        } catch (SecurityException e) {
            e.printStackTrace();
        }

        searchBar = findViewById(R.id.searchBar);
        searchBar.setHint("위치 검색");
        searchBar.setSpeechMode(false);

        searchBar.setOnSearchActionListener(this);
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Material Text ->" + getClass().getSimpleName() + "text changed ->" + searchBar.getText());
                isInit = true;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        findViewById(R.id.tvConfirm).setOnClickListener(this);
        findViewById(R.id.tvCancel).setOnClickListener(this);
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        switch (buttonCode) {
            case MaterialSearchBar.BUTTON_NAVIGATION:
                Log.d(TAG, "Button Navigation MaterialSearchBar");
                break;

            case MaterialSearchBar.BUTTON_SPEECH:
                Log.d(TAG, "Button speech MaterialSearchBar..");
                break;
        }
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
        String s = enabled ? "enabled" : "disabled";
    }


    /**
     * Address[addressLines=[0:"Gongdeog-Yeog,
     * Seoul,
     * South Korea"],
     * feature=Gongdeog-Yeog,
     * admin=Seoul,
     * sub-admin=null,
     * locality=null,
     * thoroughfare=null,
     * postalCode=null,
     * countryCode=KR,
     * countryName=South Korea,
     * hasLatitude=true,
     * latitude=37.54322,
     * hasLongitude=true,
     * longitude=126.95157599999999,
     * phone=null,
     * url=null,
     * extras=null]
     * @param text
     */
    @Override
    public void onSearchConfirmed(CharSequence text) {

        if (isSearch) {
            if (isInit) {
                Log.d(TAG, "Confirmed -> " + text.toString());
                resLocation = text.toString();
                List<Address> addressList = null;

                try {
                    //GeoCoding
                    addressList = geocoder.getFromLocationName(resLocation, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Check Size -> " + addressList.size());

                if (addressList.size() != 0) {

//
                    String title = addressList.get(0).getFeatureName();
                    String snippet = addressList.get(0).getCountryName();

                    resLatitude =  addressList.get(0).getLatitude();
                    resLongitude = addressList.get(0).getLongitude();

                    // 좌표(위도, 경도) 생성
                    LatLng point = new LatLng(resLatitude, resLongitude);
                    // 마커 생성
                    MarkerOptions mOptions2 = new MarkerOptions();
                    mOptions2.title(title);
                    mOptions2.snippet(snippet);
                    mOptions2.position(point);
                    mOptions2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                    // 마커 추가
                    mMap.addMarker(mOptions2);
                    // 해당 좌표로 화면 줌
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));

                } else if (addressList.size() == 0) {
                    Toast.makeText(getApplicationContext(), "그런 장소는 없습니다.", Toast.LENGTH_LONG).show();
                }

                isSearch = false;
            }
        } else {
            Log.d(TAG, "Already to INputLocation.");
            isSearch = true;
        }
    }


    @Override
    public void onMapReady(final GoogleMap map) {

        mMap = map;
//        LatLng SEOUL = new LatLng(37.56, 126.97);
        geocoder = new Geocoder(this);
        progress.cancel();

        Log.d(TAG, "Result Get Ready ->" + scheLatitude + "--" + scheLongitude);
        MarkerOptions markerOptions = new MarkerOptions();

        if (scheLatitude != 0.0 && scheLongitude != 0.0) {
            Log.d(TAG, "설정된 장소가 이미 지정된경우" + scheLatitude + "//" + scheLongitude + "지금 내 위치 --> " + latitude + "//" + longitude);
            //설정된 장소가 이미 지정된경우
            LatLng destMap = new LatLng(scheLatitude, scheLongitude);
            markerOptions.position(destMap);

            // 반경 500M원
            CircleOptions circle500M = new CircleOptions().center(destMap) //원점
                    .radius(500)      //반지름 단위 : m
                    .strokeWidth(0f)  //선너비 0f : 선없음
                    .fillColor(Color.parseColor("#880000ff")); //배경색

            markerOptions.title("도착지");
            markerOptions.snippet(scheLocation);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));


            resLocation = scheLocation;
            resLatitude = scheLatitude;
            resLongitude = scheLongitude;



            //내 위치가 표시될 마커 생성
            MarkerOptions currentMarker = new MarkerOptions();
            currentMarker.position(new LatLng(latitude, longitude));
            currentMarker.title("내 위치");

            map.addMarker(markerOptions);
            map.addCircle(circle500M);
            map.addMarker(currentMarker);

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(destMap, 15));
            map.animateCamera(CameraUpdateFactory.zoomTo(15));
        } else {
            Log.d(TAG, "Result Map Ready ->" + latitude + "--" + longitude);
            LatLng destMap = new LatLng(latitude, longitude);

            markerOptions.position(destMap);

            // 반경 500M원
            CircleOptions circle500M = new CircleOptions().center(destMap) //원점
                    .radius(500)      //반지름 단위 : m
                    .strokeWidth(0f)  //선너비 0f : 선없음
                    .fillColor(Color.parseColor("#880000ff")); //배경색

            markerOptions.title("내 위치");
            markerOptions.snippet("내 위치");
            map.addMarker(markerOptions);
            map.addCircle(circle500M);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(destMap, 15));
            map.animateCamera(CameraUpdateFactory.zoomTo(15));
        }



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvCancel:
                finish();
                break;
            case R.id.tvConfirm:
                Log.d(TAG, "Location Result ->" + resLocation);
                Intent intent = new Intent();
                intent.putExtra("location",resLocation);
                intent.putExtra("latitude", resLatitude);
                intent.putExtra("longitude", resLongitude);
                setResult(RESULT_OK, intent);
                finish();
                break;
        }
    }


    public void finishLocation() {
        FragmentManager fragmentManager = getFragmentManager();
        /*MapFragment mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);*/
        MapFragment mapFragment = (MapFragment) fragmentManager
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        isLocation = true;
    }
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //위치값 갱신 시
            Log.d("test", "onLocationChanged, location:" + location);

            latitude = location.getLatitude();   //위도
            longitude = location.getLongitude(); //경도
            double altitude = location.getAltitude();   //고도
            float accuracy = location.getAccuracy();    //정확도
            String provider = location.getProvider();   //위치제공자
            //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
            //Network 위치제공자에 의한 위치변화
            //Network 위치는 Gps에 비해 정확도가 많이 떨어진다.
            Log.d(TAG, "위치정보 : " + provider + "\n위도 : " + longitude + "\n경도 : " + latitude
                    + "\n고도 : " + altitude + "\n정확도 : "  + accuracy);

            if (!isLocation) {
                finishLocation();
            }
        }
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.d("test", "onStatusChanged, provider:" + s + ", status:" + i + " ,Bundle:" + bundle);

        }

        @Override
        public void onProviderEnabled(String s) {
            Log.d("test", "onProviderEnabled, provider:" + s);

        }

        @Override
        public void onProviderDisabled(String s) {
            Log.d("test", "onProviderDisabled, provider:" + s);

        }
    };
}
