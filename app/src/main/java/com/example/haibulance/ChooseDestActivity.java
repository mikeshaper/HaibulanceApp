package com.example.haibulance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChooseDestActivity extends AppCompatActivity implements  View.OnClickListener, OnMapReadyCallback, PermissionsListener {

    private final int MENU_CODE = 1;

    private CurrentSession currentSession;
    private Report currentRep;
    private MapboxMap map;
    private MapView mapView;
    private EditText fill_in;
    private Button searchBtn;
    private PermissionsManager permissionsManager;
    private Button okBtn;
    private Button cancleBtn;
    private TextView destName;
    private AutoCompleteTextView autoCompleteDestName;
    private ProgressBar progressBar;

    private boolean choosed = false;
    private Geocoder geo;
    private NavigationMapRoute navigationMapRoute;
    private LatLng hospitalLoc = new LatLng(32.0452857, 34.82474); ////המיקום של שער הספארי
    private String addressToSearch;
    private Map<String, LatLng> placesDict;
    private LatLng dest;
    private String locnameStr;

    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_choose_dest);
        okBtn = findViewById(R.id.choose_dest_ok_butt);
        cancleBtn = findViewById(R.id.choose_dest_cancle_butt);
        destName = findViewById(R.id.dest_name);
        fill_in = findViewById(R.id.reri_password_edtxt);
        searchBtn = findViewById(R.id.address_search_btn);
        //recyclerView = findViewById(R.id.adrs_rcyview);
        progressBar = findViewById(R.id.progressBar_choosedest);
        progressBar.setVisibility(View.INVISIBLE);
        placesDict  = new HashMap<String, LatLng>();


        autoCompleteDestName = findViewById(R.id.autoTXT);

        autoCompleteDestName.setThreshold(1); //will start working from first character

        currentSession = new CurrentSession();
        currentRep = currentSession.getRep();

        geo = new Geocoder(this);
        mapView = findViewById(R.id.choose_dest_map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(ChooseDestActivity.this);

        searchBtn.setOnClickListener(this);
        okBtn.setOnClickListener(this);
        cancleBtn.setOnClickListener(this);
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view == okBtn){
            if (choosed) {
                currentRep.setDestination(dest);
                currentRep._setLocationName(locnameStr);
                finish();
            }
            else     Toast.makeText(ChooseDestActivity.this, "please choose destination", Toast.LENGTH_LONG).show();
        }
        else if (view == searchBtn){
            showOptions();
        }
        else if(view == cancleBtn){
            finish();
        }
    }

    /**
     * this func shows the auto fill destination names using Geocoder
     */
    public void showOptions(){
        addressToSearch = String.valueOf(autoCompleteDestName.getText());
        if (addressToSearch == null) return;

        progressBar.setVisibility(View.VISIBLE);
        Geocoder geocoder  = new Geocoder(this);
        List<Address> addresses;
        placesDict.clear();
        try {
            addresses = geocoder.getFromLocationName(addressToSearch, 10);
            if (addresses.size() == 0) Toast.makeText(ChooseDestActivity.this, "no matches", Toast.LENGTH_SHORT).show();
            for (Address ad: addresses){
                placesDict.put(ad.getFeatureName(), new LatLng(ad.getLatitude(), ad.getLongitude()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] names = placesDict.keySet().toArray(new String[placesDict.size()]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (ChooseDestActivity.this, android.R.layout.select_dialog_item, names);
        autoCompleteDestName.setAdapter(adapter);
        progressBar.setVisibility(View.INVISIBLE);
        autoCompleteDestName.showDropDown();
        autoCompleteDestName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Point point = placesDict.get(String.valueOf(autoCompleteTextView.getText()));
                //LatLng latLng = new LatLng(point.latitude(), point.longitude());
                LatLng latLng = placesDict.get(String.valueOf(autoCompleteDestName.getText()));
                if (isLegalDest(latLng)) {
                    map.clear();
                    map.addMarker(new MarkerOptions()
                            .icon(IconFactory.getInstance(ChooseDestActivity.this).fromResource(R.drawable.green_marker))
                            .position(latLng)
                            .title("destination"));
                    addMarker(map, currentRep.getLocation(), "report");
                    dest = latLng;
                    destName.setText(addressToSearch);
                    locnameStr = addressToSearch;
                    choosed = true;
                }
            }
        });
    }

    /**
     * called automatically when the map (mapbox) is ready. includes style loading.
     * @param mapboxMap a reference to the map which the function was called on
     */
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                addMarker(mapboxMap, currentRep.getLocation(), "report");

                //initTextViews();
                //initButtons();
            }
        });

        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            @SuppressWarnings({"MissingPermission"})
            public boolean onMapClick(@NonNull LatLng point) {
                progressBar.setVisibility(View.VISIBLE);
                if (isLegalDest(point)) {
                    mapboxMap.clear();
                    mapboxMap.addMarker(new MarkerOptions()
                            .icon(IconFactory.getInstance(ChooseDestActivity.this).fromResource(R.drawable.green_marker))
                            .position(point)
                            .title("destination"));
                    addMarker(mapboxMap, currentRep.getLocation(), "report");
                    dest = point;
                    //getRoute();
                    progressBar.setVisibility(View.INVISIBLE);

                    //find location name
                    try {
                        List<Address> results = geo.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
                        Address address = results.get(0);
                        locnameStr = address.getAddressLine(0);
                        destName.setText(locnameStr);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    choosed = true;
                }
                progressBar.setVisibility(View.INVISIBLE);
                return true;
            }
        });
    }

    public String[] geoCodingFunc(String address){
        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(getString(R.string.access_token))
                .query(address)
                .build();
        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                List<CarmenFeature> results = response.body().features();
                if (results.size() > 0) {
                    for (int i = 0; i < results.size() && placesDict.size() <= 5; i++) {
                    }

                } else {
                    // No result for your request were found.
                    Toast.makeText(ChooseDestActivity.this, "no matches", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        return placesDict.keySet().toArray(new String[placesDict.size()]);
    }


    /**
     * adds a maker on map
     * @param mapboxMap the map to add a marker on
     * @param latLng the location of the marker
     * @param Title the title of the marker
     */
    public void addMarker(@NonNull MapboxMap mapboxMap, LatLng latLng, String Title) {
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.getLatitude(), latLng.getLongitude()))
                .title(Title));
    }

    private void getRoute(){
        LatLng mLatLng = new LatLng();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(ChooseDestActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(ChooseDestActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mLatLng.setLatitude(myLocation.getLatitude());
        mLatLng.setLongitude(myLocation.getLongitude());
        Point origin = Point.fromLngLat(mLatLng.getLongitude(), myLocation.getLatitude());
        Point desti = Point.fromLngLat(dest.getLongitude(), dest.getLatitude());
        Point repLoc = Point.fromLngLat(currentRep.getLocation().getLongitude(), currentRep.getLocation().getLatitude());

        NavigationRoute.builder(ChooseDestActivity.this)
                .accessToken(getString(R.string.access_token))
                .origin(origin)
                .destination(desti)
                .addWaypoint(repLoc)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null || response.body().routes().size() == 0){
                            return;
                        }
                        // Route fetched from NavigationRoute
                        DirectionsRoute route = response.body().routes().get(0);

                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        }

                        map.getStyle().removeLayer("mapbox-navigation-waypoint-layer");
                        map.getStyle().removeSource("mapbox-navigation-waypoint-source");
                        navigationMapRoute = new NavigationMapRoute(null, mapView, map);
                        navigationMapRoute.addRoute(route);
                    }
                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    }
                });
    }

    /**
     * checks if the chosen destination actually brings the report closer to the hospital
     * @param dest the chosen destination's location
     * @return if the chosen destination actually brings the report closer to the hospital
     */
    public boolean isLegalDest(LatLng dest){
        float[] destToHospital = new float[1];
        Location.distanceBetween(dest.getLatitude(), dest.getLongitude(), hospitalLoc.getLatitude(), hospitalLoc.getLongitude(), destToHospital);
        if (destToHospital[0] > currentRep.distanceFrom(hospitalLoc)) {
            Toast.makeText(ChooseDestActivity.this, "the destination you chose isn't close to the hospital", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }


// ===============================================================================================
// ===============================================================================================


    /**
     * map methods
     */
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

// Get an instance of the component
            LocationComponent locationComponent = map.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(ChooseDestActivity.this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "give me your location", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            map.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });        } else {
            Toast.makeText(this, "user location permission not granted", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * called when an intent that was started for activity result is finished.
     * @param requestCode the code entered when the intent was started
     * @param resultCode the result code of the intent
     * @param data the data returned by the intent (if there was any)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MENU_CODE:
                if (currentSession.isMenuActivityFinished()) {
                    finish();
                }
        }
    }

    /**
     * activate the option menu at the top of the screen
     * @param menu the menu to activate
     * @return true (the menu was activated successfully)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.home_button:
                currentSession.setMenuActivityFinished(true);
                finish();
                return true;
            case R.id.radius:
                Intent intent1 = new Intent(this, ChooseRadiusActivity.class);
                startActivityForResult(intent1, MENU_CODE);
                return true;
            case R.id.more:
                return true;
            case R.id.detailsItem:
                Intent intent2 = new Intent(this, UserDetailsActivity.class);
                startActivityForResult(intent2, MENU_CODE);
                return true;
            case R.id.edDetailsItem:
                Intent intent3 = new Intent(this, EditDetailsActivity.class);
                startActivityForResult(intent3, MENU_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
