package com.example.haibulance;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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

public class AnalysisActivity extends AppCompatActivity implements View.OnClickListener {

    private final int MENU_CODE = 0;

    private RecyclerView mRecyclerView;
    private GraphView graph;
    private Spinner yearOptions;
    private String yearSelected;
    private Double monthSelected;
    private DatabaseReference mDatabase;
    private Button showOnMapBtn;

    private CurrentSession currentSession;

    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        currentSession = new CurrentSession();
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

    @Override
    public void onClick(View v) {
        if (v == showOnMapBtn){
            if (monthSelected == null){
                Toast.makeText(AnalysisActivity.this, "please select a month", Toast.LENGTH_SHORT).show();
                return;
            }
            CurrentSession currentSession = new CurrentSession();
            currentSession.setRequestedMonth(String.valueOf((int)(double)monthSelected));
            currentSession.setRequestedYear(yearSelected);
            Intent intent = new Intent(this, ShowStatisticsOnMapActivity.class);
            startActivity(intent);
        }
    }

    /**
     * sets the spinner view to choose a year to show on the graph
     */
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

    /**
     * sets the graph view according to the selected year
     */
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
                        //if (counter == 2) break;
                        //Log.d("position", String.format("X: %s, Y: %s", String.valueOf(dataPoint.getX()), String.valueOf(dataPoint.getY())));
                        DataPoint dataPoint = new DataPoint(Integer.valueOf(ds.getKey()), ds.getChildrenCount());
                        series.appendData(dataPoint, false, 12);
                        counter++;
                    }
                    if (counter%2 == 0){ //solves a problem that graph will not show the last point
                        DataPoint dataPoint = new DataPoint(counter+1, 0);
                        series.appendData(dataPoint, false, 12);
                    }
                    series.setDrawDataPoints(true);
                    graph.addSeries(series);
                    graph.setMinimumWidth(0);
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

    /**
     * sets the graph labels with the months from the database
     */
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

    /**
     * shows the reports of the selected month on the recycler view
     */
    public void fillRecycler(){
        String path = String.format("reports/%s/%s", yearSelected, (int)(double)monthSelected);
        new FirebaseDatabaseHelper(path).readReports(new FirebaseDatabaseHelper.DataStatus() {
            @Override
            public void DataIsLoaded(List<Report> reports, List<String> keys) {
                new ReportsRecyclerConfig().setmConfig(mRecyclerView, AnalysisActivity.this, reports, keys);
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
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.radius:
                Intent intent1 = new Intent(this, ChooseRadiusActivity.class);
                startActivity(intent1);
                return true;
            case R.id.more:
                return true;
            case R.id.detailsItem:
                Intent intent2 = new Intent(this, UserDetailsActivity.class);
                startActivity(intent2);
                return true;
            case R.id.edDetailsItem:
                Intent intent3 = new Intent(this, EditDetailsActivity.class);
                startActivity(intent3);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}