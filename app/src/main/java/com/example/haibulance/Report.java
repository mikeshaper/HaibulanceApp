package com.example.haibulance;

import android.location.Location;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class Report{

    /**
     * the functions which starts with _ are used to get things without letting firebase get them automatically
     */

    private String specie;
    private String description;
    private LatLng location;
    private LatLng ogLocation;
    private LatLng destination;
    private String locationName;
    private String ogLocationName;
    private String time;
    private String status;
    private String reporterName;
    private String databaseKey;
    private String imgKey;
    private RepTime rawTime;
    private DatabaseReference databaseRep;

    /**
     * no params constructor
     */
    public Report(){}

    /**
     * constructor that takes all the essential params
     */
    public Report(String specie, LatLng location, String locationName, String time, String description, String reporterName, String status, RepTime rawTime){
        this.specie = specie;
        this.location = location;
        this.ogLocation = location;
        this.description = description;
        this.locationName = locationName;
        this.ogLocationName = locationName;
        this.time = rawTime.ToString();
        this.reporterName = reporterName;
        this.status = status;
        this.rawTime = rawTime;
    }

    /**
     * calculate the distance of the report from the dest param
     * @return the distance as a float in meters
     */
    public float distanceFrom(LatLng dest){
        float[] dist = new float[1];
        Location.distanceBetween(dest.getLatitude(), dest.getLongitude(), location.getLatitude(), location.getLongitude(), dist);
        return dist[0];
    }

    /**
     * @return the icon color of the report on the main map. it calculate it by the report life time
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public int iconColor(){
        float repAge = rawTime.ageInHrs();
        if (repAge < 8){
            if (repAge < 3) return R.drawable.blue_marker;
            return R.drawable.yellow_marker;
        }
        return R.drawable.mapbox_marker_icon_default;
    }

    /**
     * @param latLng location 1
     * @param latLng2 location 2
     * @return if the 2 location have the same latitude & longitude
     */
    public boolean sameLoc(LatLng latLng, LatLng latLng2){
        return latLng != null && latLng2 != null && latLng2.getLatitude() == latLng.getLatitude() && latLng2.getLongitude() == latLng.getLongitude();}

    /**
     * check if the report should be shown on the main map (when its on pickup its unpickable so i don't want other users to see it)
     * @return the report location is null or [0,0]
     */
    public boolean _isNullLoc(){
        return location == null || (location.getLatitude() == 0 && location.getLongitude() == 0);
    }

    public String getTime() {
        return time;
    }
    public RepTime getRawTime() {
        return rawTime;
    }
    public void setRawTime(RepTime rawTime) {
        this.rawTime = rawTime;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void _setStatus(String status) {
        this.status = status;
        databaseRep.child("status").setValue(status);
    }
    public String getImgKey() {
        return this.imgKey;
    }
    public void setImgKey(String imgkey) {
        this.imgKey = imgkey;
    }
    public String getDatabaseKey() {
        return databaseKey;
    }
    public void setDatabaseKey(String databaseKey) {
        this.databaseKey = databaseKey;
        databaseRep = databaseRep.child(databaseKey);
    }
    public String getSpecie() {
        return specie;
    }
    public String getDescription() {
        return description;
    }
    public String getLocationName() {
        return locationName;
    }
    public void _setLocationName(String locationName) {
        this.locationName = locationName;
        databaseRep.child("locationName").setValue(locationName);
    }
    public LatLng getLocation(){return location;}
    public void setLocation(LatLng location) {
        this.location = location;
    }
    public void _setLocation(LatLng location) {
        this.location = location;
        databaseRep.child("location").setValue(location);
    }
    public LatLng getOgLocation() {
        return ogLocation;
    }
    public void setOgLocation(LatLng ogLocation) {
        this.ogLocation = ogLocation;
    }
    public String getOgLocationName() {
        return ogLocationName;
    }
    public void setOgLocationName(String ogLocationName) {
        this.ogLocationName = ogLocationName;
    }
    public String getReporterName() {
        return reporterName;
    }
    public double _getLat() {
        return location.getLatitude();
    }
    public double _getLon() {
        return location.getLongitude();
    }
    public LatLng _getDestination(){return destination;}
    public void setDestination(LatLng destination) {
        this.destination = destination;
    }
    public String getDatabaseRep() {
        return String.format("%s/%s", databaseRep.getParent().getKey(), databaseRep.getKey());
    }
    public DatabaseReference _getDatabaseRep() {
        return this.databaseRep;
    }
    public void setDatabaseRep(String path) {
        this.databaseRep = FirebaseDatabase.getInstance().getReference("reports").child(path);
    }
    public void _setDatabaseRep(DatabaseReference databaseReference) {
        this.databaseRep = databaseReference;
    }

    /**
     * @return the report details (specie, description) organized like "hedgehog, wounded"
     */
    public String ToString(){
        return String.format("%s, %s", specie, description);
    }
}
