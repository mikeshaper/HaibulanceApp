package com.example.haibulance;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
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
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.mapbox.turf.TurfMeta;
import com.mapbox.turf.TurfTransformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.turf.TurfConstants.UNIT_METERS;

public class ChooseRadiusActivity extends AppCompatActivity  implements View.OnClickListener, OnMapReadyCallback, PermissionsListener, LocationListener {

    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private Spinner radiusOptions;
    private Button okButt;
    private Button cancleButt;
    private CurrentSession currentSession;
    private User currentUser;
    private Map<String, Integer> radiusDict;
    private String circleRadiusStr;
    //private LatLng myLatLng;
    private Location myLocation;

    private static final String TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID
            = "TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID";
    private static final String TURF_CALCULATION_FILL_LAYER_ID = "TURF_CALCULATION_FILL_LAYER_ID";
    private static final String CIRCLE_CENTER_SOURCE_ID = "CIRCLE_CENTER_SOURCE_ID";
    private static final String CIRCLE_CENTER_ICON_ID = "CIRCLE_CENTER_ICON_ID";
    private static final String CIRCLE_CENTER_LAYER_ID = "CIRCLE_CENTER_LAYER_ID";
    private int circleSteps = 180;
    private int circleRadius = 0;
    private String circleUnit = UNIT_METERS;

    private final int MENU_CODE = 1;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_choose_radius);

        mapView = findViewById(R.id.radius_map);
        okButt = findViewById(R.id.radius_ok_butt);
        cancleButt = findViewById(R.id.radius_cancle_butt);
        radiusOptions = findViewById(R.id.radius_spinner);

        currentSession = new CurrentSession();
        currentUser = currentSession.getUser();
        //circleRadius = currentUser.getReportsRadius();

        ArrayList<String> radiuses = new ArrayList<>();
        radiuses.add("all");
        radiuses.add("100m");
        radiuses.add("500m");
        radiuses.add("1km");
        radiuses.add("1.5km");
        radiuses.add("2km");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, radiuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        radiusOptions.setAdapter(adapter);

        radiusDict = new HashMap<>();
        radiusDict.put("100m", 100);
        radiusDict.put("500m", 500);
        radiusDict.put("1km", 1000);
        radiusDict.put("1.5km", 1500);
        radiusDict.put("2km", 2000);
        radiusDict.put("all", 0);

        okButt.setOnClickListener(ChooseRadiusActivity.this);
        cancleButt.setOnClickListener(ChooseRadiusActivity.this);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) this);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync( ChooseRadiusActivity.this);
    }

    @Override
    public void onClick(View view) {
        if (view == okButt){
            if (circleRadiusStr != null){
                //FirebaseDatabase.getInstance().getReference("users").child(currentUser._getDatabaseKey()).child("reportsRadius").setValue(circleRadius);
                currentUser._setReportsRadius(circleRadius);
                Toast.makeText(this, String.format("reports showing radius changed to %s", circleRadiusStr), Toast.LENGTH_LONG);
                finish();
            }
            else createRadiusDialog();//Toast.makeText(this, "please select radius", Toast.LENGTH_LONG);
        }
        else if (view == cancleButt) finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;

        mapboxMap.setStyle(new Style.Builder().fromUri(Style.MAPBOX_STREETS)
                .withImage(CIRCLE_CENTER_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                        getResources().getDrawable(R.drawable.blue_marker)))
                .withSource(new GeoJsonSource(TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID))
                .withLayer(new SymbolLayer(CIRCLE_CENTER_LAYER_ID,
                        CIRCLE_CENTER_SOURCE_ID).withProperties(
                        iconImage(CIRCLE_CENTER_ICON_ID),
                        iconIgnorePlacement(true),
                        iconAllowOverlap(true),
                        iconOffset(new Float[] {0f, -4f})
                )),  new Style.OnStyleLoaded() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                initPolygonCircleFillLayer();
                GeoJsonSource geoJsonSource = new GeoJsonSource("circle-source",
                        Point.fromLngLat(32.0452857, 34.82474));
                style.addSource(geoJsonSource);

                CircleLayer circleLayer = new CircleLayer("circle-layer", "circle-source");
                circleLayer.setProperties(
                        PropertyFactory.visibility(Property.VISIBLE),
                        PropertyFactory.circleRadius(25f),
                        PropertyFactory.circleColor(Color.argb(1, 55, 148, 179)));
                style.addLayer(circleLayer);

                //addMarkers(map);

                radiusOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                    {
                        if (myLocation == null) {
                            Toast.makeText(ChooseRadiusActivity.this, String.format("סליחה, יש בעיה במציאת מיקומך. יש לחכות מספר שניות ולנסות שוב"), Toast.LENGTH_LONG);
                            return;
                        }
                        circleRadiusStr = parent.getItemAtPosition(position).toString();
                        circleRadius = radiusDict.get(circleRadiusStr).intValue();
                        drawPolygonCircle(Point.fromLngLat(myLocation.getLongitude(), myLocation.getLatitude()));
                    } // to close the onItemSelected
                    public void onNothingSelected(AdapterView<?> parent)
                    {
                    }
                });
            }
        });
    }

    /**
     * Update the {@link FillLayer} based on the GeoJSON retrieved via
     * {@link #getTurfPolygon(Point, double, int, String)}.
     *
     * @param circleCenter the center coordinate to be used in the Turf calculation.
     */
    private void drawPolygonCircle(Point circleCenter) {
        map.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // Use Turf to calculate the Polygon's coordinates
                Polygon polygonArea = getTurfPolygon(circleCenter, circleRadius, circleSteps, circleUnit);
                GeoJsonSource polygonCircleSource = style.getSourceAs(TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID);
                if (polygonCircleSource != null) {
                    polygonCircleSource.setGeoJson(Polygon.fromOuterInner(
                            LineString.fromLngLats(TurfMeta.coordAll(polygonArea, false))));//
                }
            }
        });
    }

    /**
     * Use the Turf library {@link TurfTransformation#circle(Point, double, int, String)} method to
     * retrieve a {@link Polygon} .
     *
     * @param centerPoint a {@link Point} which the circle will center around
     * @param radius the radius of the circle
     * @param steps  number of steps which make up the circle parameter
     * @param units  one of the units found inside {@link com.mapbox.turf.TurfConstants}
     * @return a {@link Polygon} which represents the newly created circle
     */
    private Polygon getTurfPolygon(@NonNull Point centerPoint, @NonNull double radius,
                                   @NonNull int steps, @NonNull String units) {
        return TurfTransformation.circle(centerPoint, radius, steps, units);
    }

    /**
     * Add a {@link FillLayer} to display a {@link Polygon} in a the shape of a circle.
     */
    private void initPolygonCircleFillLayer() {
        map.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // Create and style a FillLayer based on information that will come from the Turf calculation
                FillLayer fillLayer = new FillLayer(TURF_CALCULATION_FILL_LAYER_ID,
                        TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID);
                fillLayer.setProperties(
                        fillColor(Color.parseColor("#f5425d")),
                        fillOpacity(.7f));
                style.addLayerBelow(fillLayer, CIRCLE_CENTER_LAYER_ID);
            }
        });
    }

    public void addMarkers(@NonNull MapboxMap mapboxMap){
    //    Marker marker = mapboxMap.addMarker(new MarkerOptions()
    //            .icon(IconFactory.getInstance(ChooseRadiusActivity.this).fromResource(R.drawable.myloc_icon))
    //            .position(myLatLng)
    //            .title("me"));
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("reports");
        ValueEventListener reportListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Report object and use the values to update the UI
                mapboxMap.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    Report rep = ds.getValue(Report.class);
                    float repAge = rep.getRawTime().ageInHrs();
                    LatLng hospitalLoc = new LatLng(32.0452857, 34.82474); ////המיקום של שער הספארי
                    LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                    boolean inRadius = currentUser.getReportsRadius() != 0 && rep.distanceFrom(myLatLng) > currentUser.getReportsRadius();
                    if (repAge > 24.0 || !rep.getStatus().equals("unpicked") || rep.sameLoc(hospitalLoc) || inRadius){Log.d("repfaild", String.format("age: %s, status: %s", repAge, rep.getStatus()));}
                    Marker marker = mapboxMap.addMarker(new MarkerOptions()
                            .icon(IconFactory.getInstance(ChooseRadiusActivity.this).fromResource(rep.iconColor()))
                            .position(rep.getLocation())
                            .title(rep.ToString()));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Report failed
            }
        };
        mDatabase.addValueEventListener(reportListener);
    }

    public void createRadiusDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this).show();
        dialog.setContentView(R.layout.save_radius_dialog);
        dialog.findViewById(R.id.left_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                FirebaseDatabase.getInstance().getReference("users").child(currentUser._getDatabaseKey()).child("reportsRadius").setValue(0);
                currentUser._setReportsRadius(0);
                Toast.makeText(ChooseRadiusActivity.this, String.format("reports showing radius changed to %s", 0), Toast.LENGTH_LONG);
                finish();
            }
        });
        dialog.findViewById(R.id.right_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        try {
            Looper.loop();
        } catch (RuntimeException e) { }
    }

//================================================================================================
// ===============================================================================================

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
                    //enableLocationComponent(style);
                }
            });        } else {
            Toast.makeText(this, "currentUser location permission not granted", Toast.LENGTH_LONG).show();
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
                //Intent intent1 = new Intent(this, ChooseRadiusActivity.class);
                //startActivityForResult(intent1, MENU_CODE);
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


    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        //map.clear();
        drawPolygonCircle(Point.fromLngLat(location.getLongitude(), location.getLatitude()));
        //myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
    @Override
    public void onProviderEnabled(String provider) {

    }
    @Override
    public void onProviderDisabled(String provider) {

    }
}
