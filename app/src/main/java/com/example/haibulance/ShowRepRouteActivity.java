package com.example.haibulance;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
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
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

public class ShowRepRouteActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    private final int MENU_CODE = 0;

    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap map;
    private CurrentSession currentSession;
    private Report currentRep;
    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;
    private MapboxNavigation navigation;
    private LatLng destination;
    private LatLng origin;


    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_show_route);

        navigation = new MapboxNavigation(this, getString(R.string.access_token));
        mapView = findViewById(R.id.route_map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        currentSession = new CurrentSession();
        currentRep = currentSession.getRep();
        destination = currentRep.getLocation();
        origin = currentRep.getOgLocation();
    }

    /**
     * called automatically when the map (mapbox) is ready. includes style loading.
     * @param mapboxMap a reference to the map which the function was called on
     */
    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                addDestinationIconSymbolLayer(style);

                //find my location

                Point orig = Point.fromLngLat(origin.getLongitude(), origin.getLatitude());
                Point dest = Point.fromLngLat(destination.getLongitude(), destination.getLatitude());
                mapboxMap.addMarker(new MarkerOptions()
                        .icon(IconFactory.getInstance(ShowRepRouteActivity.this).fromResource(R.drawable.red_marker))
                        .position(origin)
                        .title("מיקום מקורי: " + currentRep.getOgLocationName()));
                mapboxMap.addMarker(new MarkerOptions()
                        .icon(IconFactory.getInstance(ShowRepRouteActivity.this).fromResource(R.drawable.green_marker))
                        .position(destination)
                        .title("מיקום לאחר איסוף: " + currentRep.getLocationName()));
                getRoute(orig, dest);
            }
        });
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    /**
     * shows the report route on map
     * @param orig the report origin (ogLocation)
     * @param dest the new location after
     */
    private void getRoute(Point orig, Point dest){
        NavigationRoute.builder(ShowRepRouteActivity.this)
                .accessToken(getString(R.string.access_token))
                .origin(orig)
                .destination(dest)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null || response.body().routes().size() == 0){
                            Log.d("showrouteactivity", "no routs found ):");
                            return;
                        }
                        // Route fetched from NavigationRoute
                        DirectionsRoute route = response.body().routes().get(0);
                        currentRoute = response.body().routes().get(0);
                        if (navigationMapRoute != null)
                            navigationMapRoute.removeRoute();
                        else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, map);
                            navigationMapRoute.addRoute(route);
                        }
                    }
                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.d("NaviActivity", t.getMessage());
                    }
                });
    }

//==============================================================================================
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
            permissionsManager = new PermissionsManager(this);
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
            //finish();
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
        navigation.onDestroy();
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
                    currentSession.setMenuActivityFinished(false);
                    currentSession.setOnRepActivity(false);
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
                //Intent intent = new Intent(this, MainActivity.class);
                //startActivity(intent);
                currentSession.setOnRepActivity(false);
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
