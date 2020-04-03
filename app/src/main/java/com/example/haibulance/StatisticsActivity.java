package com.example.haibulance;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView mRecyclerView;
    private GraphView graph;
    private Spinner yearOptions;
    private String yearSelected;
    private Double monthSelected;
    private DatabaseReference mDatabase;
    private Button showOnMapBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        mRecyclerView = findViewById(R.id.recyclerview_reports);
        graph = findViewById(R.id.graphView);
        yearOptions = findViewById(R.id.year_spinner);
        showOnMapBtn = findViewById(R.id.show_stat_on_map_btn);

        showOnMapBtn.setOnClickListener(this);
        setSpinner();
        yearOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                graph.removeAllSeries();
                mRecyclerView.setAdapter(null);
                yearSelected = parent.getItemAtPosition(position).toString();
                if (yearSelected != "choose a year") setGraph();
            } // to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    public void setSpinner(){
        ArrayList<String> years = new ArrayList<>();
        years.add("choose a year");
        mDatabase = FirebaseDatabase.getInstance().getReference("reports");
        ValueEventListener reportListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    years.add(ds.getKey());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Report failed
            }
        };
        mDatabase.addValueEventListener(reportListener);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearOptions.setAdapter(adapter);
    }

    public void setGraph(){
        try {
            LineGraphSeries<DataPoint> series = new LineGraphSeries< >(new DataPoint[] {});

            mDatabase = FirebaseDatabase.getInstance().getReference("reports").child(yearSelected);
            ValueEventListener reportListener = new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int counter = 0;
                    for (DataSnapshot ds: dataSnapshot.getChildren()) {
                        DataPoint dataPoint = new DataPoint(Integer.valueOf(ds.getKey()), ds.getChildrenCount());
                        //Log.d("sdfgfg", String.format("X: %s, Y: %s", String.valueOf(dataPoint.getX()), String.valueOf(dataPoint.getY())));
                        series.appendData(dataPoint, false, 12);
                        counter++;
                    }
                    series.setDrawDataPoints(true);
                    graph.addSeries(series);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Report failed
                }
            };
            mDatabase.addValueEventListener(reportListener);

            series.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    String msg = "X: " + dataPoint.getX() + "\nY: " + dataPoint.getY();
                    //Toast.makeText(StatisticsActivity.this, msg, Toast.LENGTH_SHORT).show();
                    monthSelected = dataPoint.getX();
                    fillRecycler();
                }
            });
            setGraphLables();
        }
        catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void setGraphLables(){
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX) {
                int month = (int)value;
                if (isValueX && month > 0) {
                    Log.d("value: ", String.valueOf(value));
                    String monthName = new DateFormatSymbols().getMonths()[month-1];
                    return monthName.substring(0,3);
                    //return "Month " + super.formatLabel(value, isValueX);
                }
                return super.formatLabel(value, isValueX);
            }
        });
    }

    public void fillRecycler(){
        String path = String.format("reports/%s/%s", yearSelected, (int)(double)monthSelected);
        new FirebaseDatabaseHelper(path).readReports(new FirebaseDatabaseHelper.DataStatus() {
            @Override
            public void DataIsLoaded(List<Report> reports, List<String> keys) {
                new ReportsRecyclerConfig().setmConfig(mRecyclerView, StatisticsActivity.this, reports, keys);
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

    @Override
    public void onClick(View v) {
        if (v == showOnMapBtn){
            if (monthSelected == null){
                Toast.makeText(StatisticsActivity.this, "please select a month", Toast.LENGTH_SHORT).show();
                return;
            }
            CurrentSession currentSession = new CurrentSession();
            currentSession.setRequestedMonth(String.valueOf((int)(double)monthSelected));
            currentSession.setRequestedYear(yearSelected);
            Intent intent = new Intent(this, ShowStatisticsOnMapActivity.class);
            startActivity(intent);
        }
    }
}