package com.example.haibulance;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class User {
    private String name;
    private String email;
    private String password;
    private int reportsRadius;
    private String databaseKey = "";
    private int id;
    private int pickups = 0;
    private int reports = 0;
    private DatabaseReference databaseUser;


    /**
     * no params constructor
     */
    public User(){}

    /**
     * constructor that takes all the essential params
     */
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.reportsRadius = 0;
    }

    public void addPickup() {
        this.pickups++;
        databaseUser.child("pickups").setValue(pickups);
    }
    public void addReport() {
        this.reports++;
        databaseUser.child("reports").setValue(reports);
    }

    /**
     * check if the given report is in the user's reports-show-radius
     * @param rep the given report
     * @param myLatLng device's location
     * @return if the given report is in the user's reports-show-radius
     */
    public Boolean RepInRad(Report rep, LatLng myLatLng){
        return (reportsRadius != 0 && rep.distanceFrom(myLatLng) < reportsRadius) || (reportsRadius == 0);
    }

    public int getPickups() {
        return pickups;
    }
    public void setPickups(int pickups) {
        this.pickups = pickups;
    }
    public int getReports() {
        return reports;
    }
    public void setReports(int reports) {
        this.reports = reports;
    }
    public int getReportsRadius() {
        return reportsRadius;
    }
    public void setReportsRadius(int reportsRadius) {
        this.reportsRadius = reportsRadius;
    }
    public void _setReportsRadius(int reportsRadius) {
        this.reportsRadius = reportsRadius;
        databaseUser.child("reportsRadius").setValue(reportsRadius);
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String _getDatabaseKey() {
        return databaseKey;
    }
    public void setDatabaseKey(String databaseKey) {
        this.databaseKey = databaseKey;
        databaseUser = FirebaseDatabase.getInstance().getReference("users").child(databaseKey);
    }

    public String ToString(){return String.format("name: %s, Email: %s, password: %s", name, email, password);}
}

