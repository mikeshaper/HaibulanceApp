package com.example.haibulance;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.List;

public class UsersList extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);
        mRecyclerView = findViewById(R.id.recyclerview_users);
        new FirebaseDatabaseHelper().readUsers(new FirebaseDatabaseHelper.DataStatus() {
            @Override
            public void DataIsLoaded(List<User> users, List<String> keys) {
                new RecyclerView_Config().setmConfig(mRecyclerView, UsersList.this, users, keys);
            }
            @Override
            public void DataIsInserted() {

            }

            @Override
            public void DataIsUpdated() {

            }

            @Override
            public void DataIsDeleated() {

            }
        });
    }
}
