package com.example.haibulance;
//haibulance id in google ads: ca-app-pub-5099612993587566~7360269361

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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
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
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.turf.TurfConstants.UNIT_METERS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, PermissionsListener, LocationListener {

    private static final int RADIUS_CODE = 1;
    private final int MENU_CODE = 2;
    private final int REPORT_CODE = 3;


    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap map;
    private Button flora;
    private Button report;
    private Button centerize;
    private ProgressBar progressBar;
    private AdView mAdView;


    private boolean startPickup; //נועד לפתור בעיה של onDataChange (שלא ניתן לעשות finish() בתוכו)
    private DatabaseReference mDatabase;
    private FirebaseDatabase db;
    private FirebaseAuth firebaseAuth;
    private Location myLocation;

    private CurrentSession currentSession;
    private User currentUser;
    private int year;
    private int month;

    Style style;

    private static final String TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID
            = "TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID";
    private static final String TURF_CALCULATION_FILL_LAYER_ID = "TURF_CALCULATION_FILL_LAYER_ID";
    private static final String CIRCLE_CENTER_SOURCE_ID = "CIRCLE_CENTER_SOURCE_ID";
    private static final String CIRCLE_CENTER_ICON_ID = "CIRCLE_CENTER_ICON_ID";
    private static final String CIRCLE_CENTER_LAYER_ID = "CIRCLE_CENTER_LAYER_ID";
    private int circleSteps = 180;
    private int circleRadius = 0;
    private String circleUnit = UNIT_METERS;


    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        currentSession = new CurrentSession();
        getUserFromDatabase();

        //currentUser = currentSession.getUser();

        // get the Firebase instance
        db = FirebaseDatabase.getInstance();

        // initialise views
        flora = findViewById(R.id.flora_butt);
        report = findViewById(R.id.report_butt);
        centerize = findViewById(R.id.centerize_butt);
        mapView = findViewById(R.id.mapView1);
        progressBar = findViewById(R.id.progressBar_markerClicked);
        progressBar.setVisibility(View.INVISIBLE);

        flora.setOnClickListener(this);
        report.setOnClickListener(this);
        centerize.setOnClickListener(this);

        year = LocalDateTime.now().getYear();
        month = LocalDateTime.now().getMonthValue();
        setAdd();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(MainActivity.this);

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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View view) {
        if (view == flora) {
            Log.d("clicked", "flora");
            Intent intent = new Intent(this, FloraActivity.class);
            startActivity(intent);
        }
        else if (view == report) {
            currentSession.setOnRepActivity(false);
            if (myLocation != null) {
                Log.d("clicked", "report");
                checkRepExists();
            }
            else Toast.makeText(MainActivity.this, "sorry, there is a problem with finding your location", Toast.LENGTH_LONG).show();

        }
        else if (view == centerize){
            if (true){
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.zoomWhileTracking(16f);
            }
        }
    }

    /**
     * called automatically when the map (mapbox) is ready. includes style loading.
     * @param mapboxMap a reference to the map which the function was called on
     */
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        locationComponent = map.getLocationComponent();
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
            public void onStyleLoaded(@NonNull Style loadedStyle) {
                style = loadedStyle;
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

                //drawPolygonCircle(myLocation);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    addMarkers();
                }
                mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        //int id = (int)marker.getId();
                        progressBar.setVisibility(View.VISIBLE);
                        LatLng latLng = marker.getPosition();
                        mDatabase = FirebaseDatabase.getInstance().getReference("reports/" + year);
                        for (int i = month-1; i<=month; i++) {
                            ValueEventListener findReportListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        Report rep = ds.getValue(Report.class);
                                        if (rep.sameLoc(rep.getLocation(), latLng)) {
                                            rep.setDatabaseKey(ds.getKey());
                                            currentSession.setRep(rep);
                                            startPickup = true;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    // Getting Report failed
                                }
                            };
                            mDatabase.child(String.valueOf(i)).addValueEventListener(findReportListener);
                        }
                        progressBar.setVisibility(View.INVISIBLE);
                        if (currentSession.getRep() == null)
                            Toast.makeText(MainActivity.this, "ERROR: could not find the report", Toast.LENGTH_LONG).show();
                        else {
                            Log.d("Main", "starting pickup...");
                            Intent intent = new Intent(MainActivity.this, PickupActivity.class);
                            startActivity(intent);
                        }
                        return true;
                    }
                });

                mDatabase = FirebaseDatabase.getInstance().getReference("reports");
                ValueEventListener reportListener = new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get Report object and use the values to update the UI
                        map.clear();
                        addMarkers();
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Getting Report failed
                    }
                };
                mDatabase.addValueEventListener(reportListener);

            }
        });
    }

    /**
     * this function is called when the user wants to report on an animal but there is already a close report to it.
     * it asks the user if he mean the report that exists.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkRepExists() {
        final boolean[] makeRep = {true};
        mDatabase = FirebaseDatabase.getInstance().getReference(String.format("reports/%s/%s", year, month));
        ValueEventListener findReportListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double lat = myLocation.getLatitude();
                double lon = myLocation.getLongitude();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Report rep = ds.getValue(Report.class);
                    if (!rep._isNullLoc()) {
                        float[] distance = new float[1];
                        LatLng repLoc = rep.getLocation();
                        Location.distanceBetween(lat, lon, repLoc.getLatitude(), repLoc.getLongitude(), distance);
                        float repAge = 0;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            repAge = rep.getRawTime().ageInHrs();
                        }
                        if (distance[0] < 100 && !currentSession.isOnRepActivity() && !rep.getStatus().equals("caseClosed") && repAge < 24.0 && !rep.sameLoc(rep.getLocation(), hospitalLoc)) {
                            Log.d("dclicked", "creating dialog.. " + distance[0]);
                            if (!createCloseRepDialog(rep.ToString())) {
                                makeRep[0] = false;
                                return;
                            }
                            //while (!dialogEnded) {Log.d("dclicked", "dialog isnt ended yet");}
                            //dialogEnded= false;
                        }
                    }
                }
                if (makeRep[0] && !currentSession.isOnRepActivity()){
                    Log.d("dclicked", "starting report...");
                    currentSession.setOnRepActivity(true);
                    Intent intent = new Intent(MainActivity.this, ReportActivity.class);
                    startActivityForResult(intent, REPORT_CODE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        mDatabase.addValueEventListener(findReportListener);
    }

    /**
     * a helper function that called by checkRepExists func. creates a dialog with the user.
     * @param repTxt the string of the close report that already exists to put on the dialog.
     * @return if the user said that he mean the report that exist or is it a new unreported one.
     */
    public boolean createCloseRepDialog(String repTxt) {
        final boolean[] makeRep = {true};
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException(); }};
        final AlertDialog dialog = new AlertDialog.Builder(this).show();
        dialog.setContentView(R.layout.close_rep_dialog);
        TextView t = dialog.findViewById(R.id.rep_name);
        t.setText("do you mean to report on: " + repTxt + "?");
        dialog.findViewById(R.id.left_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("dclicked", "left");
                dialog.dismiss();
                makeRep[0] = false;
                handler.sendMessage(handler.obtainMessage());
            }
        });
        dialog.findViewById(R.id.right_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("dclicked", "right");
                dialog.dismiss();
                handler.sendMessage(handler.obtainMessage());
            }
        });

        try {
            Looper.loop();
        } catch (RuntimeException e) { }

        return makeRep[0];
    }

    LatLng hospitalLoc = new LatLng(32.0452857, 34.82474); ////המיקום של שער הספארי
    /**
     * this func adds the reports to the main map as markers colored by the report existence time
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void addMarkers(){
        map.clear();
        //if (myLocation == null) return;
        int year = LocalDateTime.now().getYear();
        int month = LocalDateTime.now().getMonthValue();
        mDatabase = FirebaseDatabase.getInstance().getReference("reports").child(String.valueOf(year));
        for (int i = month-1; i <=month; i++)
        {
            ValueEventListener reportListener = new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Get Report object and use the values to update the UI
                    for (DataSnapshot ds: dataSnapshot.getChildren()) {
                        Report rep = ds.getValue(Report.class);
                        float repAge = rep.getRawTime().ageInHrs();
                        LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                        boolean inRadius = currentUser.RepInRad(rep, myLatLng);
                        if (rep._isNullLoc() || repAge > 24.0 || rep.getStatus().equals("caseClosed") || rep.sameLoc(rep.getLocation(), hospitalLoc) || !inRadius){}
                        else {
                            Marker marker = map.addMarker(new MarkerOptions()
                                    .icon(IconFactory.getInstance(MainActivity.this).fromResource(rep.iconColor()))
                                    .position(rep.getLocation())
                                    .title(rep.ToString()));
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Report failed
                }
            };
            mDatabase.child(String.valueOf(i)).addValueEventListener(reportListener);
        }
    }

    /**
     * get user obj from the database (firebase) and put it in the CurrenSession as a static param
     */
    public void getUserFromDatabase(){
        firebaseAuth = FirebaseAuth.getInstance();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference membersRef = rootRef.child("users").child(firebaseAuth.getUid());
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                currentUser = dataSnapshot.getValue(User.class);
                currentUser.setDatabaseKey(firebaseAuth.getCurrentUser().getUid());
                currentSession.setUser(currentUser);
                circleRadius = currentUser.getReportsRadius();
                drawPolygonCircle(myLocation);
                Log.d("checkUser", currentSession.getUser().ToString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
        membersRef.addListenerForSingleValueEvent(valueEventListener);
    }

    /**
     * Update the {@link FillLayer} based on the GeoJSON retrieved via
     * {@link #getTurfPolygon(Point, double, int, String)}.
     *
     * @param loc the center coordinate to be used in the Turf calculation.
     */
    private void drawPolygonCircle(Location loc) {
        if (map == null || loc == null) return;
        Point circleCenter = Point.fromLngLat(loc.getLongitude(), loc.getLatitude());
        map.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // Use Turf to calculate the Polygon's coordinates
                Polygon polygonArea = getTurfPolygon(circleCenter, circleRadius, circleSteps, circleUnit);
                GeoJsonSource polygonCircleSource = style.getSourceAs(TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID);
                if (polygonCircleSource != null) {
                    polygonCircleSource.setGeoJson(Polygon.fromOuterInner(
                            LineString.fromLngLats(TurfMeta.coordAll(polygonArea, false))));
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
                        fillOpacity(.7f),
                        fillColor(Color.GREEN));

                style.addLayerBelow(fillLayer, CIRCLE_CENTER_LAYER_ID);
            }
        });
    }

    /**
     * sets the add view to show random adds provided by google
     */
    public void setAdd(){
        MobileAds.initialize(this, "ca-app-pub-5099612993587566~7360269361");//"ca-app-pub-5099612993587566~7360269361");
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        });
    }

//==============================================================================================
// ===============================================================================================


    /**
     * map methods
     */
    private LocationComponent locationComponent;
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
// Create and customize the LocationComponent's options
            LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(this).build();
// Get an instance of the component
            LocationComponent locationComponent = map.getLocationComponent();


            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .locationComponentOptions(customLocationComponentOptions)
                            .build();

// Activate with options
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING_COMPASS);

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
        //Toast.makeText(this, "give me your location", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            map.getStyle(new Style.OnStyleLoaded() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);

                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    //myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            });
        }
        else {
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REPORT_CODE:
                //currentSession.setOnRepActivity(false);
            case MENU_CODE:
                map.clear();
                addMarkers();
                circleRadius = currentUser.getReportsRadius();
                drawPolygonCircle(myLocation);
                currentSession.setMenuActivityFinished(false);
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
            case R.id.statistics:
                Intent intent4 = new Intent(this, AnalysisActivity.class);
                startActivityForResult(intent4, MENU_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * this func just blocks back press as a way to close the app (i just hate it when my mom does it)
     */
    @Override
    public void onBackPressed() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        if(map == null) return;
        map.clear();
        addMarkers();
        drawPolygonCircle(location);
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
