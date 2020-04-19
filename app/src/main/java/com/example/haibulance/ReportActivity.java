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
import android.widget.Switch;
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
import java.util.Arrays;
import java.util.LinkedList;
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
    private Button reportButt;
    private EditText descriptoin;
    private TextView locName;
    private ImageView image;
    private AutoCompleteTextView specie;
    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap map;
    private Switch aiSwitch;

    private String locnameStr;
    private Geocoder geo;
    private LatLng latLng;
    private String time;
    private RepTime rawTime;
    // Uri indicates where the image will be picked from
    private Uri imgUri;
    private String randID = UUID.randomUUID().toString();
    private String[] allAnimalsSpecies;

    private CurrentSession currentSession;
    private User currentUser;
    private Report report;
    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    // instance for firebase storage and StorageReference
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private FirebaseTranslator englishHebrewTranslator;

    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
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
        aiSwitch = findViewById(R.id.anmlsAi_switch);
        specie = findViewById(R.id.specie_edtxt);

        specie.setThreshold(1); //will start working from first character
        hour_.setText(rawTime.ToString());

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
        aiSwitch.setOnClickListener(this);

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

        setEnglishHebrewTranslator();
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
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
        else if (view == aiSwitch){
            if (adapter != null){
                if (aiSwitch.isChecked()) specie.setAdapter(onlyAnimalsAdapter);
                else specie.setAdapter(adapter);
            }
            specie.showDropDown();
        }
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

    /**
     * opens the report's img for a larger view
     */
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

    /**
     * use camera api to take a picture
     */
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

    /**
     * use an api to get image from device storage
     */
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

    /**
     * upload report and image to database
     */
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

    ArrayAdapter<String> adapter;
    ArrayAdapter<String> onlyAnimalsAdapter;
    /**
     * using google ai recognition to recognize automatically the animal in the image that uploaded
     * @param uri the image uri
     */
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
                            adapter = new ArrayAdapter<String>
                                    (ReportActivity.this, android.R.layout.select_dialog_item);
                            onlyAnimalsAdapter = new ArrayAdapter<String>
                                    (ReportActivity.this, android.R.layout.select_dialog_item);
                            // Task completed successfully
                            for (FirebaseVisionImageLabel label: labels) {
                                String text = label.getText();
                                String entityId = label.getEntityId();
                                boolean isAnimal = checkIfAnimal(text);
                                float confidence = label.getConfidence();
                                adapter.add(String.format("%s (%s)", text, confidence));
                                if (isAnimal) onlyAnimalsAdapter.add(String.format("%s (%s)", text, confidence));
                                Log.d("ai result: ", String.format("text: %s, entryid: %s, confi: %s", text, entityId, confidence));
                                // Create an English-Hebrew translator:
                                List<String> alreadyTranslated =  new LinkedList<String>(Arrays.asList());
                                englishHebrewTranslator.translate(text)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<String>() {
                                                    @Override
                                                    public void onSuccess(@NonNull String translatedText) {
                                                        if (!alreadyTranslated.contains(translatedText)) {
                                                            alreadyTranslated.add(translatedText);
                                                            if (isAnimal)
                                                                onlyAnimalsAdapter.add(String.format("%s (%s)", translatedText, confidence));
                                                            adapter.add(String.format("%s (%s)", translatedText, confidence));
                                                        }
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
                            if (aiSwitch.isChecked()) specie.setAdapter(onlyAnimalsAdapter);
                            else specie.setAdapter(adapter);
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
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param str the recognized object
     * @return if the object is animal
     */
    public boolean checkIfAnimal(String str){
        List<String> lst = Arrays.asList(allAnimalsSpecies);
        return lst.contains(str);
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

    /**
     * sets the firebase translator and set the specie's adapter to all species in EN & HE
     */
    public void setEnglishHebrewTranslator(){
        FirebaseTranslatorOptions options =
                new FirebaseTranslatorOptions.Builder()
                        .setSourceLanguage(FirebaseTranslateLanguage.EN)
                        .setTargetLanguage(FirebaseTranslateLanguage.HE)
                        .build();
        englishHebrewTranslator =
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

        allAnimalsSpecies = getString(R.string.animals).split(" ");
        onlyAnimalsAdapter = new ArrayAdapter<String>
                (ReportActivity.this, android.R.layout.select_dialog_item);
        onlyAnimalsAdapter.addAll(allAnimalsSpecies);
        List<String> alreadyTranslated = new LinkedList<String>(Arrays.asList());
        for (String specie: allAnimalsSpecies) {
            englishHebrewTranslator.translate(specie)
                    .addOnSuccessListener(
                            new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(@NonNull String translatedText) {
                                    String translatedTxt = translatedText;
                                    if (!alreadyTranslated.contains(translatedTxt)) {
                                        alreadyTranslated.add(translatedTxt);
                                        onlyAnimalsAdapter.add(translatedTxt);
                                    }
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
        specie.setAdapter(onlyAnimalsAdapter);
    }

    /**
     * upload the report object to database
     */
    public void writeToDatabase(Report report) {
        //uploadPhoto(myFile);
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

    /**
     * permissions functions (for taking img and write/read device storage)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                    if (!checkRStoragePermission())
                        requestRStoragePermission();
                    else
                        takePic();
                } else {
                }
                break;
            case RSTORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    takePic();
                } else {
                }
                break;
            case LOCATION_REQUEST_CODE:
                permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
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



    private String namesStr =
            "Aardvark\n" +
                    "Abyssinian\n" +
                    "Adelie Penguin\n" +
                    "Affenpinscher\n" +
                    "Afghan Hound\n" +
                    "African Bush Elephant\n" +
                    "African Civet\n" +
                    "African Clawed Frog\n" +
                    "African Forest Elephant\n" +
                    "African Palm Civet\n" +
                    "African Penguin\n" +
                    "African Tree Toad\n" +
                    "African Wild Dog\n" +
                    "Ainu Dog\n" +
                    "Airedale Terrier\n" +
                    "Akbash\n" +
                    "Akita\n" +
                    "Alaskan Malamute\n" +
                    "Albatross\n" +
                    "Aldabra Giant Tortoise\n" +
                    "Alligator\n" +
                    "Alpine Dachsbracke\n" +
                    "American Bulldog\n" +
                    "American Cocker Spaniel\n" +
                    "American Coonhound\n" +
                    "American Eskimo Dog\n" +
                    "American Foxhound\n" +
                    "American Pit Bull Terrier\n" +
                    "American Staffordshire Terrier\n" +
                    "American Water Spaniel\n" +
                    "Amur Leopard\n" +
                    "Anatolian Shepherd Dog\n" +
                    "Angelfish\n" +
                    "Ant\n" +
                    "Anteater\n" +
                    "Antelope\n" +
                    "Appenzeller Dog\n" +
                    "Arctic Fox\n" +
                    "Arctic Hare\n" +
                    "Arctic Wolf\n" +
                    "Armadillo\n" +
                    "Asian Elephant\n" +
                    "Asian Giant Hornet\n" +
                    "Asian Palm Civet\n" +
                    "Asiatic Black Bear\n" +
                    "Australian Cattle Dog\n" +
                    "Australian Kelpie Dog\n" +
                    "Australian Mist\n" +
                    "Australian Shepherd\n" +
                    "Australian Terrier\n" +
                    "Avocet\n" +
                    "Axolotl\n" +
                    "Aye Aye\n"+
                    "Baboon\n" +
                    "Bactrian Camel\n" +
                    "Badger\n" +
                    "Balinese\n" +
                    "Banded Palm Civet\n" +
                    "Bandicoot\n" +
                    "Barb\n" +
                    "Barn Owl\n" +
                    "Barnacle\n" +
                    "Barracuda\n" +
                    "Basenji Dog\n" +
                    "Basking Shark\n" +
                    "Basset Hound\n" +
                    "Bat\n" +
                    "Bavarian Mountain Hound\n" +
                    "Beagle\n" +
                    "Bear\n" +
                    "Bearded Collie\n" +
                    "Bearded Dragon\n" +
                    "Beaver\n" +
                    "Bedlington Terrier\n" +
                    "Beetle\n" +
                    "Bengal Tiger\n" +
                    "Bernese Mountain Dog\n" +
                    "Bichon Frise\n" +
                    "Binturong\n" +
                    "Bird\n" +
                    "Birds Of Paradise\n" +
                    "Birman\n" +
                    "Bison\n" +
                    "Black Rhinoceros\n" +
                    "Black Russian Terrier\n" +
                    "Black Widow Spider\n" +
                    "Bloodhound\n" +
                    "Blue Lacy Dog\n" +
                    "Blue Whale\n" +
                    "Bluetick Coonhound\n" +
                    "Bobcat\n" +
                    "Bolognese Dog\n" +
                    "Bombay\n" +
                    "Bongo\n" +
                    "Bonobo\n" +
                    "Booby\n" +
                    "Border Collie\n" +
                    "Border Terrier\n" +
                    "Bornean Orang-utan\n" +
                    "Borneo Elephant\n" +
                    "Boston Terrier\n" +
                    "Bottlenose Dolphin\n" +
                    "Boxer Dog\n" +
                    "Boykin Spaniel\n" +
                    "Brazilian Terrier\n" +
                    "Brown Bear\n" +
                    "Budgerigar\n" +
                    "Buffalo\n" +
                    "Bull Mastiff\n" +
                    "Bull Shark\n" +
                    "Bull Terrier\n" +
                    "Bulldog\n" +
                    "Bullfrog\n" +
                    "Bumble Bee\n" +
                    "Burmese\n" +
                    "Burrowing Frog\n" +
                    "Butterfly\n" +
                    "Butterfly Fish\n" +
                    "Caiman\n" +
                    "Caiman Lizard\n" +
                    "Cairn Terrier\n" +
                    "Camel\n" +
                    "Canaan Dog\n" +
                    "Capybara\n" +
                    "Caracal\n" +
                    "Carolina Dog\n" +
                    "Cassowary\n" +
                    "Cat\n" +
                    "Caterpillar\n" +
                    "Catfish\n" +
                    "Cavalier King Charles Spaniel\n" +
                    "Centipede\n" +
                    "Cesky Fousek\n" +
                    "Chameleon\n" +
                    "Chamois\n" +
                    "Cheetah\n" +
                    "Chesapeake Bay Retriever\n" +
                    "Chicken\n" +
                    "Chihuahua\n" +
                    "Chimpanzee\n" +
                    "Chinchilla\n" +
                    "Chinese Crested Dog\n" +
                    "Chinook\n" +
                    "Chinstrap Penguin\n" +
                    "Chipmunk\n" +
                    "Chow Chow\n" +
                    "Cichlid\n" +
                    "Clouded Leopard\n" +
                    "Clown Fish\n" +
                    "Clumber Spaniel\n" +
                    "Coati\n" +
                    "Cockroach\n" +
                    "Collared Peccary\n" +
                    "Collie\n" +
                    "Common Buzzard\n" +
                    "Common Frog\n" +
                    "Common Loon\n" +
                    "Common Toad\n" +
                    "Coral\n" +
                    "Cottontop Tamarin\n" +
                    "Cougar\n" +
                    "Cow\n" +
                    "Coyote\n" +
                    "Crab\n" +
                    "Crab-Eating Macaque\n" +
                    "Crane\n" +
                    "Crested Penguin\n" +
                    "Crocodile\n" +
                    "Cross River Gorilla\n" +
                    "Curly Coated Retriever\n" +
                    "Cuscus\n" +
                    "Cuttlefish\n" +
                    "Dachshund\n" +
                    "Dalmatian\n" +
                    "Darwin's Frog\n" +
                    "Deer\n" +
                    "Desert Tortoise\n" +
                    "Deutsche Bracke\n" +
                    "Dhole\n" +
                    "Dingo\n" +
                    "Discus\n" +
                    "Doberman Pinscher\n" +
                    "Dodo\n" +
                    "Dog\n" +
                    "Dogo Argentino\n" +
                    "Dogue De Bordeaux\n" +
                    "Dolphin\n" +
                    "Donkey\n" +
                    "Dormouse\n" +
                    "Dragonfly\n" +
                    "Drever\n" +
                    "Duck\n" +
                    "Dugong\n" +
                    "Dunker\n" +
                    "Dusky Dolphin\n" +
                    "Dwarf Crocodile\n" +
                    "Eagle\n" +
                    "Earwig\n" +
                    "Eastern Gorilla\n" +
                    "Eastern Lowland Gorilla\n" +
                    "Echidna\n" +
                    "Edible Frog\n" +
                    "Egyptian Mau\n" +
                    "Electric Eel\n" +
                    "Elephant\n" +
                    "Elephant Seal\n" +
                    "Elephant Shrew\n" +
                    "Emperor Penguin\n" +
                    "Emperor Tamarin\n" +
                    "Emu\n" +
                    "English Cocker Spaniel\n" +
                    "English Shepherd\n" +
                    "English Springer Spaniel\n" +
                    "Entlebucher Mountain Dog\n" +
                    "Epagneul Pont Audemer\n" +
                    "Eskimo Dog\n" +
                    "Estrela Mountain Dog\n" +
                    "Falcon\n" +
                    "Fennec Fox\n" +
                    "Ferret\n" +
                    "Field Spaniel\n" +
                    "Fin Whale\n" +
                    "Finnish Spitz\n" +
                    "Fire-Bellied Toad\n" +
                    "Fish\n" +
                    "Fishing Cat\n" +
                    "Flamingo\n" +
                    "Flat Coat Retriever\n" +
                    "Flounder\n" +
                    "Fly\n" +
                    "Flying Squirrel\n" +
                    "Fossa\n" +
                    "Fox\n" +
                    "Fox Terrier\n" +
                    "French Bulldog\n" +
                    "Frigatebird\n" +
                    "Frilled Lizard\n" +
                    "Frog\n" +
                    "Fur Seal\n" +
                    "Galapagos Penguin\n" +
                    "Galapagos Tortoise\n" +
                    "Gar\n" +
                    "Gecko\n" +
                    "Gentoo Penguin\n" +
                    "Geoffroys Tamarin\n" +
                    "Gerbil\n" +
                    "German Pinscher\n" +
                    "German Shepherd\n" +
                    "Gharial\n" +
                    "Giant African Land Snail\n" +
                    "Giant Clam\n" +
                    "Giant Panda Bear\n" +
                    "Giant Schnauzer\n" +
                    "Gibbon\n" +
                    "Gila Monster\n" +
                    "Giraffe\n" +
                    "Glass Lizard\n" +
                    "Glow Worm\n" +
                    "Goat\n" +
                    "Golden Lion Tamarin\n" +
                    "Golden Oriole\n" +
                    "Golden Retriever\n" +
                    "Goose\n" +
                    "Gopher\n" +
                    "Gorilla\n" +
                    "Grasshopper\n" +
                    "Great Dane\n" +
                    "Great White Shark\n" +
                    "Greater Swiss Mountain Dog\n" +
                    "Green Bee-Eater\n" +
                    "Greenland Dog\n" +
                    "Grey Mouse Lemur\n" +
                    "Grey Reef Shark\n" +
                    "Grey Seal\n" +
                    "Greyhound\n" +
                    "Grizzly Bear\n" +
                    "Grouse\n" +
                    "Guinea Fowl\n" +
                    "Guinea Pig\n" +
                    "Guppy\n" +
                    "Hammerhead Shark\n" +
                    "Hamster\n" +
                    "Hare\n" +
                    "Harrier\n" +
                    "Havanese\n" +
                    "Hedgehog\n" +
                    "Hercules Beetle\n" +
                    "Hermit Crab\n" +
                    "Heron\n" +
                    "Highland Cattle\n" +
                    "Himalayan\n" +
                    "Hippopotamus\n" +
                    "Honey Bee\n" +
                    "Horn Shark\n" +
                    "Horned Frog\n" +
                    "Horse\n" +
                    "Horseshoe Crab\n" +
                    "Howler Monkey\n" +
                    "Human\n" +
                    "Humboldt Penguin\n" +
                    "Hummingbird\n" +
                    "Humpback Whale\n" +
                    "Hyena\n" +
                    "Ibis\n" +
                    "Ibizan Hound\n" +
                    "Iguana\n" +
                    "Impala\n" +
                    "Indian Elephant\n" +
                    "Indian Palm Squirrel\n" +
                    "Indian Rhinoceros\n" +
                    "Indian Star Tortoise\n" +
                    "Indochinese Tiger\n" +
                    "Indri\n" +
                    "Insect\n" +
                    "Irish Setter\n" +
                    "Irish WolfHound\n" +
                    "Jack Russel\n" +
                    "Jackal\n" +
                    "Jaguar\n" +
                    "Japanese Chin\n" +
                    "Japanese Macaque\n" +
                    "Javan Rhinoceros\n" +
                    "Javanese\n" +
                    "Jellyfish\n" +
                    "Kakapo\n" +
                    "Kangaroo\n" +
                    "Keel Billed Toucan\n" +
                    "Killer Whale\n" +
                    "King Crab\n" +
                    "King Penguin\n" +
                    "Kingfisher\n" +
                    "Kiwi\n" +
                    "Koala\n" +
                    "Komodo Dragon\n" +
                    "Kudu\n" +
                    "Labradoodle\n" +
                    "Labrador Retriever\n" +
                    "Ladybug\n" +
                    "Leaf-Tailed Gecko\n" +
                    "Lemming\n" +
                    "Lemur\n" +
                    "Leopard\n" +
                    "Leopard Cat\n" +
                    "Leopard Seal\n" +
                    "Leopard Tortoise\n" +
                    "Liger\n" +
                    "Lion\n" +
                    "Lionfish\n" +
                    "Little Penguin\n" +
                    "Lizard\n" +
                    "Llama\n" +
                    "Lobster\n" +
                    "Long-Eared Owl\n" +
                    "Lynx\n" +
                    "Macaroni Penguin\n" +
                    "Macaw\n" +
                    "Magellanic Penguin\n" +
                    "Magpie\n" +
                    "Maine Coon\n" +
                    "Malayan Civet\n" +
                    "Malayan Tiger\n" +
                    "Maltese\n" +
                    "Manatee\n" +
                    "Mandrill\n" +
                    "Manta Ray\n" +
                    "Marine Toad\n" +
                    "Markhor\n" +
                    "Marsh Frog\n" +
                    "Masked Palm Civet\n" +
                    "Mastiff\n" +
                    "Mayfly\n" +
                    "Meerkat\n" +
                    "Millipede\n" +
                    "Minke Whale\n" +
                    "Mole\n" +
                    "Molly\n" +
                    "Mongoose\n" +
                    "Mongrel\n" +
                    "Monitor Lizard\n" +
                    "Monkey\n" +
                    "Monte Iberia Eleuth\n" +
                    "Moorhen\n" +
                    "Moose\n" +
                    "Moray Eel\n" +
                    "Moth\n" +
                    "Mountain Gorilla\n" +
                    "Mountain Lion\n" +
                    "Mouse\n" +
                    "Mule\n" +
                    "Neanderthal\n" +
                    "Neapolitan Mastiff\n" +
                    "Newfoundland\n" +
                    "Newt\n" +
                    "Nightingale\n" +
                    "Norfolk Terrier\n" +
                    "North American Black Bear\n" +
                    "Norwegian Forest\n" +
                    "Numbat\n" +
                    "Nurse Shark\n" +
                    "Ocelot\n" +
                    "Octopus\n" +
                    "Okapi\n" +
                    "Old English Sheepdog\n" +
                    "Olm\n" +
                    "Opossum\n" +
                    "Orang-utan\n" +
                    "Ostrich\n" +
                    "Otter\n" +
                    "Oyster\n" +
                    "Pademelon\n" +
                    "Panther\n" +
                    "Parrot\n" +
                    "Patas Monkey\n" +
                    "Peacock\n" +
                    "Pekingese\n" +
                    "Pelican\n" +
                    "Penguin\n" +
                    "Persian\n" +
                    "Pheasant\n" +
                    "Pied Tamarin\n" +
                    "Pig\n" +
                    "Pika\n" +
                    "Pike\n" +
                    "Pink Fairy Armadillo\n" +
                    "Piranha\n" +
                    "Platypus\n" +
                    "Pointer\n" +
                    "Poison Dart Frog\n" +
                    "Polar Bear\n" +
                    "Pond Skater\n" +
                    "Poodle\n" +
                    "Pool Frog\n" +
                    "Porcupine\n" +
                    "Possum\n" +
                    "Prawn\n" +
                    "Proboscis Monkey\n" +
                    "Puffer Fish\n" +
                    "Puffin\n" +
                    "Pug\n" +
                    "Puma\n" +
                    "Purple Emperor\n" +
                    "Puss Moth\n" +
                    "Pygmy Hippopotamus\n" +
                    "Pygmy Marmoset\n" +
                    "Quail\n" +
                    "Quetzal\n" +
                    "Quokka\n" +
                    "Quoll\n" +
                    "Rabbit\n" +
                    "Raccoon\n" +
                    "Raccoon Dog\n" +
                    "Radiated Tortoise\n" +
                    "Ragdoll\n" +
                    "Rat\n" +
                    "Rattlesnake\n" +
                    "Red Knee Tarantula\n" +
                    "Red Panda\n" +
                    "Red Wolf\n" +
                    "Red-handed Tamarin\n" +
                    "Reindeer\n" +
                    "Rhinoceros\n" +
                    "River Dolphin\n" +
                    "River Turtle\n" +
                    "Robin\n" +
                    "Rock Hyrax\n" +
                    "Rockhopper Penguin\n" +
                    "Roseate Spoonbill\n" +
                    "Rottweiler\n" +
                    "Royal Penguin\n" +
                    "Russian Blue\n" +
                    "Sabre-Toothed Tiger\n" +
                    "Saint Bernard\n" +
                    "Salamander\n" +
                    "Sand Lizard\n" +
                    "Saola\n" +
                    "Scorpion\n" +
                    "Scorpion Fish\n" +
                    "Sea Dragon\n" +
                    "Sea Lion\n" +
                    "Sea Otter\n" +
                    "Sea Slug\n" +
                    "Sea Squirt\n" +
                    "Sea Turtle\n" +
                    "Sea Urchin\n" +
                    "Seahorse\n" +
                    "Seal\n" +
                    "Serval\n" +
                    "Sheep\n" +
                    "Shih Tzu\n" +
                    "Shrimp\n" +
                    "Siamese\n" +
                    "Siamese Fighting Fish\n" +
                    "Siberian\n" +
                    "Siberian Husky\n" +
                    "Siberian Tiger\n" +
                    "Silver Dollar\n" +
                    "Skunk\n" +
                    "Sloth\n" +
                    "Slow Worm\n" +
                    "Snail\n" +
                    "Snake\n" +
                    "Snapping Turtle\n" +
                    "Snowshoe\n" +
                    "Snowy Owl\n" +
                    "Somali\n" +
                    "South China Tiger\n" +
                    "Spadefoot Toad\n" +
                    "Sparrow\n" +
                    "Spectacled Bear\n" +
                    "Sperm Whale\n" +
                    "Spider Monkey\n" +
                    "Spiny Dogfish\n" +
                    "Sponge\n" +
                    "Squid\n" +
                    "Squirrel\n" +
                    "Squirrel Monkey\n" +
                    "Sri Lankan Elephant\n" +
                    "Staffordshire Bull Terrier\n" +
                    "Stag Beetle\n" +
                    "Starfish\n" +
                    "Stellers Sea Cow\n" +
                    "Stick Insect\n" +
                    "Stingray\n" +
                    "Stoat\n" +
                    "Striped Rocket Frog\n" +
                    "Sumatran Elephant\n" +
                    "Sumatran Orang-utan\n" +
                    "Sumatran Rhinoceros\n" +
                    "Sumatran Tiger\n" +
                    "Sun Bear\n" +
                    "Swan\n" +
                    "Tang\n" +
                    "Tapanuli Orang-utan\n" +
                    "Tapir\n" +
                    "Tarsier\n" +
                    "Tasmanian Devil\n" +
                    "Tawny Owl\n" +
                    "Termite\n" +
                    "Tetra\n" +
                    "Thorny Devil\n" +
                    "Tibetan Mastiff\n" +
                    "Tiffany\n" +
                    "Tiger\n" +
                    "Tiger Salamander\n" +
                    "Tiger Shark\n" +
                    "Tortoise\n" +
                    "Toucan\n" +
                    "Tree Frog\n" +
                    "Tropicbird\n" +
                    "Tuatara\n" +
                    "Turkey\n" +
                    "Turkish Angora\n" +
                    "Uakari\n" +
                    "Uguisu\n" +
                    "Umbrellabird\n" +
                    "Vampire Bat\n" +
                    "Vervet Monkey\n" +
                    "Vulture\n" +
                    "Wallaby\n" +
                    "Walrus\n" +
                    "Warthog\n" +
                    "Wasp\n" +
                    "Water Buffalo\n" +
                    "Water Dragon\n" +
                    "Water Vole\n" +
                    "Weasel\n" +
                    "Welsh Corgi\n" +
                    "West Highland Terrier\n" +
                    "Western Gorilla\n" +
                    "Western Lowland Gorilla\n" +
                    "Whale Shark\n" +
                    "Whippet\n" +
                    "White Faced Capuchin\n" +
                    "White Rhinoceros\n" +
                    "White Tiger\n" +
                    "Wild Boar\n" +
                    "Wildebeest\n" +
                    "Wolf\n" +
                    "Wolverine\n" +
                    "Wombat\n" +
                    "Woodlouse\n" +
                    "Woodpecker\n" +
                    "Woolly Mammoth\n" +
                    "Woolly Monkey\n" +
                    "Wrasse\n" +
                    "X-Ray Tetra\n" +
                    "Yak\n" +
                    "Yellow-Eyed Penguin\n" +
                    "Yorkshire Terrier\n" +
                    "Zebra\n" +
                    "Zebra Shark\n" +
                    "Zebu\n" +
                    "Zonkey\n" +
                    "Zorse";
}