package com.example.haibulance;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FirebaseDatabaseHelper {
    private FirebaseDatabase mDatabase;
    private DatabaseReference mReferenceUsers;
    private List<Report> reports = new ArrayList<>();

    public interface DataStatus{
        void DataIsLoaded(List<Report> reports, List<String> keys);
        void DataIsInserted();
        void DataIsUpdated();
        void DataIsDeleated();
    }
    public FirebaseDatabaseHelper(String path){
        mDatabase = FirebaseDatabase.getInstance();
        mReferenceUsers = mDatabase.getReference(path);
    }

    public void readReports(final DataStatus dataStatus){
        mReferenceUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reports.clear();
                List<String> keys = new ArrayList<>();
                for (DataSnapshot keyNode: dataSnapshot.getChildren()){
                    keys.add(keyNode.getKey());
                    Report report = keyNode.getValue(Report.class);
                    reports.add(report);
                }
                dataStatus.DataIsLoaded(reports, keys);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
