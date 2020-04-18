package com.example.haibulance;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReportActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, PermissionsListener {

    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int LOCATION_REQUEST_CODE = 3;
    private static final int WSTORAGE_REQUEST_CODE = 2;
    private static final int RSTORAGE_REQUEST_CODE = 4;
    static final int REQUEST_IMAGE_CAPTURE = 5;
    private final int PICK_IMAGE_REQUEST = 6;
    private final int MENU_CODE = 7;

    private TextView hour_;
    private EditText specieedtxt;
    private Button reportButt;
    private EditText descriptoin;
    private TextView locName;
    private ImageView image;
    private AutoCompleteTextView specie;

    private String locnameStr;
    private Geocoder geo;
    private LatLng latLng;
    private String time;
    private RepTime rawTime;
    // Uri indicates, where the image will be picked from
    private Uri imgUri;
    private boolean imageAdded = false;
    private String randID = UUID.randomUUID().toString();

    private CurrentSession currentSession;
    private User currentUser;
    private Report report;
    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;


    // instance for firebase storage and StorageReference
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap map;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, String.valueOf(R.string.access_token));
        setContentView(R.layout.activity_report);

        rawTime = new RepTime(LocalDateTime.now());
        geo = new Geocoder(this);
        hour_ = findViewById(R.id.hour_tv);
        reportButt = findViewById(R.id.report_butt);
        descriptoin = findViewById(R.id.dscr_edtxt);
        locName = findViewById(R.id.report_locname);
        image = findViewById(R.id.img_pickup);
        hour_.setText(rawTime.ToString());


        specie = findViewById(R.id.specie_edtxt);
        specie.setThreshold(1); //will start working from first character


        progressDialog = new ProgressDialog(this);
        currentSession = new CurrentSession();
        currentUser = currentSession.getUser();
        // get the Firebase  storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference("reports");

        mapView = findViewById(R.id.mini_map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        reportButt.setOnClickListener(this);
        image.setOnClickListener(this);
        locName.setOnClickListener(this);
        specie.setOnClickListener(this);

        latLng = new LatLng();
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        latLng.setLatitude(myLocation.getLatitude());
        latLng.setLongitude(myLocation.getLongitude());
    }

    @Override
    public void onClick(View view) {
        if (view == reportButt){
            make_report();
        }
        else if (view == image) {
                createIMGDialog();
        }
        else if (view == specie){
            specie.showDropDown();
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                //find my location
                //LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                //Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                addMarker(map);

                //find location name
                try {
                    List<Address> results = geo.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
                    Address address = results.get(0);
                    locnameStr = address.getAddressLine(0);
                    locName.setText(locnameStr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void createIMGDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .show();

        dialog.setContentView(R.layout.take_or_choose_img);
        dialog.findViewById(R.id.left_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                takePic();
            }
        });

        dialog.findViewById(R.id.right_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                SelectImage();
            }
        });
    }

    protected void takePic(){
        if (!checkCamPermission())
            requestCamPermission();
        if (!checkRStoragePermission())
            requestRStoragePermission();
        if (!checkWStoragePermission())
            requestWStoragePermission();
        if (checkWStoragePermission() && checkRStoragePermission() && checkCamPermission()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void SelectImage() {
        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    private void uploadData() {
        report = new Report(specie.getText().toString(), latLng, locnameStr, time, descriptoin.getText().toString(), currentUser.getName(), "unpicked", rawTime);
        report.setImgKey("default");
        currentSession.setRep(report);

        if (imgUri == null) writeToDatabase(report);

        else {
            // Code for showing progressDialog while uploading
            progressDialog.setMessage("Uploading image...");
            progressDialog.show();

            // Defining the child of storageReference
            StorageReference ref = storageReference.child("images/" + randID);
            // adding listeners on upload
            // or failure of image
            ref.putFile(imgUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Image uploaded successfully
                            // Dismiss dialog
                            progressDialog.dismiss();
                            //Toast.makeText(ReportActivity.this, "Image Uploaded!", Toast.LENGTH_SHORT).show();

                            if (imgUri != null) {
                                report.setImgKey(randID);
                                writeToDatabase(report);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast.makeText(ReportActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        // Progress Listener for loading
                        // percentage on the dialog box
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            //progressDialog.setMessage("Uploaded " + (int)progress + "%");
                            //progressDialog.dismiss();
                        }
                    });
        }
    }

    private void aiRecognition(Uri uri){
        FirebaseVisionImage image1;
        try {
            image1 = FirebaseVisionImage.fromFilePath(getApplicationContext(), uri);
            FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                    .getOnDeviceImageLabeler();
            labeler.processImage(image1)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>
                                    (ReportActivity.this, android.R.layout.select_dialog_item);
                            // Task completed successfully
                            for (FirebaseVisionImageLabel label: labels) {
                                String text = label.getText();
                                String entityId = label.getEntityId();
                                float confidence = label.getConfidence();
                                adapter.add(String.format("%s (%s)", text, confidence));
                                Log.d("ai result: ", String.format("text: %s, entryid: %s, confi: %s", text, entityId, confidence));






                                Log.d("translated text: ", "translating...");
                                // Create an English-Hebrew translator:
                                FirebaseTranslatorOptions options =
                                        new FirebaseTranslatorOptions.Builder()
                                                .setSourceLanguage(FirebaseTranslateLanguage.EN)
                                                .setTargetLanguage(FirebaseTranslateLanguage.HE)
                                                .build();

                                final FirebaseTranslator englishHebrewTranslator =
                                        FirebaseNaturalLanguage.getInstance().getTranslator(options);

                                FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                                        .requireWifi()
                                        .build();
                                englishHebrewTranslator.downloadModelIfNeeded(conditions)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void v) {
                                                        // Model downloaded successfully. Okay to start translating.
                                                        // (Set a flag, unhide the translation UI, etc.)
                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Model couldn’t be downloaded or other internal error.
                                                        // ...
                                                    }
                                                });

                                englishHebrewTranslator.translate(text)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<String>() {
                                                    @Override
                                                    public void onSuccess(@NonNull String translatedText) {
                                                        Log.d("translated text: ", translatedText);
                                                        adapter.add(String.format("%s (%s)", translatedText, confidence));
                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Error.
                                                        // ...
                                                    }
                                                });
                            }








                            specie.setAdapter(adapter);
                            specie.showDropDown();
                            specie.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    specie.setText(adapter.getItem(i).split(" ")[0]);
                                }
                            });
                        }

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            // ...
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    try{
                        // Assume block needs to be inside a Try/Catch block.
                        String path = Environment.getExternalStorageDirectory().toString();
                        OutputStream fOut = null;
                        Integer counter = 0;
                        File directory = new File(path);
                        directory.mkdirs();
                        File file = new File(path, String.format("IMG%d.jpg", counter)); // the File to save , append increasing numeric counter to prevent files from getting overwritten.

                        try {
                            fOut = new FileOutputStream(file);

                        }
                        catch (FileNotFoundException fe){
                        }
                        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                        imgUri = Uri.fromFile(file);
                        aiRecognition(imgUri);
                        fOut.flush(); // Not really required
                        fOut.close(); // do not forget to close the stream
                        MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
                    }
                    catch (IOException e){
                        Log.d("creating uri failed", e.toString());
                    }
                    image.setImageBitmap(imageBitmap);
                }

            case PICK_IMAGE_REQUEST:
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    // Get the Uri of data
                    imgUri = data.getData();
                    try {
                        // Setting image on image view using Bitmap
                        Bitmap bitmap = MediaStore
                                .Images
                                .Media
                                .getBitmap(getContentResolver(), imgUri);
                        image.setImageBitmap(bitmap);
                    }
                    catch (IOException e) {
                        // Log the exception
                        e.printStackTrace();
                    }
                    aiRecognition(imgUri);
                }

            case MENU_CODE:
                if (currentSession.isMenuActivityFinished()) {
                    currentSession.setMenuActivityFinished(false);
                    currentSession.setOnRepActivity(false);
                    finish();
                }
        }
    }

    public void make_report() {
        String specie_ = String.valueOf(specie.getText());
        String desc = String.valueOf(descriptoin.getText());
        if (TextUtils.isEmpty(specie_)) {
            //חלון ששואל "האם בטוח שלא להכניס סוג חיה?"
            Toast.makeText(ReportActivity.this, "נא להכניס סוג חיה", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(desc)) {
            Toast.makeText(ReportActivity.this, "נא להכניס תיאור", Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            uploadData();
            currentUser.addReport();
        }
    }

    public void addMarker(@NonNull MapboxMap mapboxMap) {
                mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.getLatitude(), latLng.getLongitude()))
                .title("My location"));
    }

    public void writeToDatabase(Report report) {
        //uploadPhoto(myFile);
        Log.d("report created", "rep created");
        RepTime repTime = report.getRawTime();
        mDatabase = mDatabase.child(String.valueOf(repTime.getYear())).child(String.valueOf(repTime.getMonth()));
        report._setDatabaseRep(mDatabase);
        mDatabase.push().setValue(report);
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                currentSession.setOnRepActivity(false);
                finish();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ReportActivity.this, "error creating a report database: " + error, Toast.LENGTH_SHORT).show();
                //Intent intent = new Intent(ReportActivity.this, MainActivity.class);
                //startActivity(intent);
                currentSession.setOnRepActivity(false);
                finish();
            }
        });
    }


    //================================================================================================================================
    //================================================================================================================================


    private void requestCamPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);
    }
    private void requestRStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                RSTORAGE_REQUEST_CODE);
    }

    private void requestWStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WSTORAGE_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    if (!checkWStoragePermission())
                        requestWStoragePermission();
                    else
                        takePic();
                } else {
                    //Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestCamPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
            case WSTORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    if (!checkRStoragePermission())
                        requestRStoragePermission();
                    else
                        takePic();
                } else {
                    //Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case RSTORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    takePic();
                } else {
                    //Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case LOCATION_REQUEST_CODE:
                permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ReportActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    private boolean checkCamPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }
    private boolean checkWStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }
    private boolean checkRStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }


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
            ActivityCompat.requestPermissions(ReportActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            //permissionsManager.requestLocationPermissions(this);
        }
    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //Toast.makeText(this, "give me your location", Toast.LENGTH_LONG).show();
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
            //Toast.makeText(this, "user location permission not granted", Toast.LENGTH_LONG).show();
            //Intent intent = new Intent(this, MainActivity.class);
            //startActivity(intent);
            currentSession.setOnRepActivity(false);
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
