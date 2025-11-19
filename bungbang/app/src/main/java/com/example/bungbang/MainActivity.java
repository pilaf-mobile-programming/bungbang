package com.example.bungbang;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.overlay.Marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private FusedLocationSource locationSource;
    private NaverMap naverMap;

    private View detailPanel;
    private ImageButton btnClose;
    private TextView tvTitle, tvType, tvHours, tvPrice, tvDesc;

    // 체크박스
    private CheckBox cbBung, cbTako, cbHodduk, cbFlower;

    // 타입별 마커 리스트
    private final List<Marker> bungMarkers = new ArrayList<>();
    private final List<Marker> takoMarkers = new ArrayList<>();
    private final List<Marker> hoddukMarkers = new ArrayList<>();
    private final List<Marker> flowerMarkers = new ArrayList<>();


    enum StoreType {
        BUNGBANG, TAKOYAKI, HODDUK, FLOWERBANG
    }

    static class BungStore {
        String name;
        double lat;
        double lng;
        String description;
        String openHours;
        String priceInfo;
        StoreType type;

        BungStore(String name,
                  double lat,
                  double lng,
                  String description,
                  String openHours,
                  String priceInfo,
                  StoreType type) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.description = description;
            this.openHours = openHours;
            this.priceInfo = priceInfo;
            this.type = type;
        }
    }

    // --- 붕어빵 ---
    private final List<BungStore> bungStores = Arrays.asList(
            new BungStore("숭실대 정문 붕어빵",
                    37.496311, 126.957459, "슈크림 / 팥", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.BUNGBANG),
            new BungStore("중앙도서관 앞 붕어빵",
                    37.495912, 126.956950, "도서관 앞 인기 간식", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.BUNGBANG)
    );

    // --- 타코야끼 ---
    private final List<BungStore> takoyakiStores = Arrays.asList(
            new BungStore("숭실대 후문 타코야끼",
                    37.495480, 126.958700, "따끈한 타코야끼", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.TAKOYAKI),
            new BungStore("형남공학관 옆 타코야끼",
                    37.495850, 126.957900, "공대생 단골", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.TAKOYAKI)
    );

    // --- 호떡 ---
    private final List<BungStore> hotteokStores = Arrays.asList(
            new BungStore("학생회관 앞 호떡",
                    37.496000, 126.957900, "겨울철 필수 간식", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.HODDUK),
            new BungStore("조만식기념관 뒤 호떡",
                    37.496450, 126.956800, "바삭바삭 호떡", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.HODDUK)
    );

    // --- 국화빵 ---
    private final List<BungStore> kukhwaStores = Arrays.asList(
            new BungStore("문화관 앞 국화빵",
                    37.495650, 126.956500, "달콤한 국화빵", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.FLOWERBANG),
            new BungStore("공학관 계단 옆 국화빵",
                    37.495200, 126.957200, "학생들 휴식 스팟", "영업시간 : 14:00 ~ 22:00", "가격: 3개 2,000원", StoreType.FLOWERBANG)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cbBung = findViewById(R.id.cb_bung);
        cbTako = findViewById(R.id.cb_tako);
        cbHodduk = findViewById(R.id.cb_hodduk);
        cbFlower = findViewById(R.id.cb_flower);

// 체크 상태 바뀔 때마다 마커 가시성 갱신
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            updateMarkerVisibility();
        };

        cbBung.setOnCheckedChangeListener(listener);
        cbTako.setOnCheckedChangeListener(listener);
        cbHodduk.setOnCheckedChangeListener(listener);
        cbFlower.setOnCheckedChangeListener(listener);


        detailPanel = findViewById(R.id.detail_panel);
        btnClose = findViewById(R.id.btn_close);
        tvTitle = findViewById(R.id.tv_title);
        tvType = findViewById(R.id.tv_type);
        tvHours = findViewById(R.id.tv_hours);
        tvPrice = findViewById(R.id.tv_price);
        tvDesc = findViewById(R.id.tv_desc);

        // 닫기 버튼: 패널 숨기기
        btnClose.setOnClickListener(v -> hideDetailPanel());


        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;

        // 카테고리별로 마커 찍기
        addMarkers(bungStores, naverMap);
        addMarkers(takoyakiStores, naverMap);
        addMarkers(hotteokStores, naverMap);
        addMarkers(kukhwaStores, naverMap);

        // 카메라는 붕어빵 첫 가게 기준
        BungStore first = bungStores.get(0);
        LatLng target = new LatLng(first.lat, first.lng);
        naverMap.moveCamera(CameraUpdate.scrollTo(target));
        naverMap.moveCamera(CameraUpdate.zoomTo(15.0));

        // 초기 필터 상태 반영
        updateMarkerVisibility();
    }

    private void addMarkers(List<BungStore> stores, NaverMap map) {
        for (BungStore store : stores) {
            Marker marker = new Marker();
            marker.setPosition(new LatLng(store.lat, store.lng));
            marker.setCaptionText(store.name);

            // 이미지 아이콘 설정
            marker.setIcon(getMarkerIcon(store.type));

            // 이 마커에 이 가게 정보를 태그로 붙여둠
            marker.setTag(store);

            // 클릭 리스너
            marker.setOnClickListener(overlay -> {
                BungStore s = (BungStore) marker.getTag();
                if (s != null) {
                    showDetailPanel(s);
                }
                return true; // 클릭 이벤트 여기서 소비
            });

            // 타입에 따라 리스트에 담기
            switch (store.type) {
                case BUNGBANG:
                    bungMarkers.add(marker);
                    break;
                case TAKOYAKI:
                    takoMarkers.add(marker);
                    break;
                case HODDUK:
                    hoddukMarkers.add(marker);
                    break;
                case FLOWERBANG:
                    flowerMarkers.add(marker);
                    break;
            }

            marker.setMap(map);
        }
    }

    private void updateMarkerVisibility() {
        if (naverMap == null) return;

        boolean showBung = cbBung.isChecked();
        boolean showTako = cbTako.isChecked();
        boolean showHodduk = cbHodduk.isChecked();
        boolean showFlower = cbFlower.isChecked();

        // 붕어빵 마커
        for (Marker m : bungMarkers) {
            m.setMap(showBung ? naverMap : null);
        }

        // 타코야끼 마커
        for (Marker m : takoMarkers) {
            m.setMap(showTako ? naverMap : null);
        }

        // 호떡 마커
        for (Marker m : hoddukMarkers) {
            m.setMap(showHodduk ? naverMap : null);
        }

        // 국화빵 마커
        for (Marker m : flowerMarkers) {
            m.setMap(showFlower ? naverMap : null);
        }
    }

    private void showDetailPanel(BungStore store) {
        tvTitle.setText(store.name);
        tvType.setText(getTypeText(store.type));
        tvHours.setText(store.openHours);
        tvPrice.setText(store.priceInfo);
        tvDesc.setText(store.description);

        detailPanel.setVisibility(View.VISIBLE);
    }

    private void hideDetailPanel() {
        detailPanel.setVisibility(View.GONE);
    }

    private String getTypeText(StoreType type) {
        switch (type) {
            case BUNGBANG:
                return "붕어빵";
            case TAKOYAKI:
                return "타코야끼";
            case HODDUK:
                return "호떡";
            case FLOWERBANG:
                return "국화빵";
            default:
                return "간식";
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = locationSource.onRequestPermissionsResult(
                    requestCode, permissions, grantResults
            );
            if (!granted) {
                if (naverMap != null) {
                    naverMap.setLocationTrackingMode(LocationTrackingMode.None);
                }
                return;
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private OverlayImage getMarkerIcon(StoreType type) {
        switch (type) {
            case BUNGBANG:
                return getResizedMarkerIcon(R.drawable.bungbang, 80, 80);
            case TAKOYAKI:
                return getResizedMarkerIcon(R.drawable.taco, 80, 80);
            case HODDUK:
                return getResizedMarkerIcon(R.drawable.hodduk, 80, 80);
            case FLOWERBANG:
                return getResizedMarkerIcon(R.drawable.flowerbang, 80, 80);
            default:
                return getResizedMarkerIcon(R.drawable.bungbang, 80, 80);
        }
    }


    private OverlayImage getResizedMarkerIcon(int drawableId, int width, int height) {
        Bitmap original = BitmapFactory.decodeResource(getResources(), drawableId);
        Bitmap resized = Bitmap.createScaledBitmap(original, width, height, false);
        return OverlayImage.fromBitmap(resized);
    }


}
