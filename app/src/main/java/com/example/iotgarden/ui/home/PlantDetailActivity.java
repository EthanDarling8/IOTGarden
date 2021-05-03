package com.example.iotgarden.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.iotgarden.R;
import com.example.iotgarden.recycler.RecyclerViewAdapter;
import com.example.iotgarden.stemma.Reading;
import com.example.iotgarden.stemma.SoilReading;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class PlantDetailActivity extends AppCompatActivity {

    private static LineChart weekChart;
    private static LineChart dayChart;
    private static LineChart monthChart;
    private static LineData dayLineData;
    private static LineData weekLineData;
    private static LineData monthLineData;

    private final LocalDate today = LocalDate.now();
    private final LocalDate pastSeven = today.minusDays(7);

    private SoilReading sr;
    private final String TAG = "Plant Detail Activity";
    private String stemmaName;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        dayChart = findViewById(R.id.dayChart);
        weekChart = findViewById(R.id.weekChart);
        monthChart = findViewById(R.id.monthChart);

        stemmaName = getIntent().getStringExtra("SESSION_NAME");

        FloatingActionButton fab = findViewById(R.id.fab_details_edit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PlantDetailActivity.this, PlantEditActivity.class);
                intent.putExtra("SESSION_NAME", stemmaName);
                startActivity(intent);
            }
        });

        toolBarLayout.setTitle(stemmaName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        ValueEventListener soilListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sr = snapshot.getValue(SoilReading.class);
                assert sr != null;

                Set<String> srKeySet = sr.stemma_1.keySet();
                for (String k : srKeySet) {
                    if (!Objects.requireNonNull(sr.stemma_1.get(k)).name.equals(stemmaName)) {
                        srKeySet.remove(k);
                    }
                }

                Object[] srKeyArray = srKeySet.toArray();
                Arrays.sort(srKeyArray);
                List<Entry> dayEntries = new ArrayList<>();
                List<Entry> weekEntries = new ArrayList<>();
                List<Entry> monthEntries = new ArrayList<>();

                // Create custom chart axes for dayChart
                createAxes(dayChart, true, 0, true, 23);
                if (today.getDayOfMonth() >= 7) {
                    createAxes(weekChart, true, pastSeven.getDayOfMonth(), true, today.getDayOfMonth());
                } else {
                    createAxes(weekChart, true, 1, true, today.getDayOfMonth());
                }
                createAxes(monthChart, true, 1, true, today.getMonth().length(false));

                // Populate entry lists
                populateDayEntryList(sr, srKeyArray, dayEntries, today);
                populateWeekEntryList(srKeyArray, weekEntries);
                populateMonthEntryList(srKeyArray, monthEntries, today);

                // Sort entries by date
                Collections.sort(dayEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
                Collections.sort(weekEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
                Collections.sort(monthEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));

                // Find 7 day min and max
                sevenDayMinMax(srKeyArray);

                // Create data sets
                dayLineData = createDataSet(dayEntries, "Moisture");
                weekLineData = createDataSet(weekEntries, "Moisture");
                monthLineData = createDataSet(monthEntries, "Moisture");

                // Initialize Descriptions
                Description dayDesc = new Description();
                dayDesc.setTextSize(12f);
                dayDesc.setText(getString(R.string.select_point));
                dayChart.setDescription(dayDesc);
                Description weekDesc = new Description();
                weekDesc.setTextSize(12f);
                weekDesc.setText(getString(R.string.select_point));
                weekChart.setDescription(weekDesc);
                Description monthDesc = new Description();
                monthDesc.setTextSize(12f);
                monthDesc.setText(getString(R.string.select_point));
                monthChart.setDescription(monthDesc);

                // Create charts and set selection text
                onChartValueSelected(dayDesc, dayChart, "Time: %.0f:00 Moisture: %.0f");
                refreshChart(dayChart, dayLineData);
                onChartValueSelected(weekDesc, weekChart, "Day: %.0f Moisture: %.0f");
                refreshChart(weekChart, weekLineData);
                onChartValueSelected(monthDesc, monthChart, "Day: %.0f Moisture: %.0f");
                refreshChart(monthChart, monthLineData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "soilListener:onCancelled: ", error.toException());
            }
        };
        mDatabase.addValueEventListener(soilListener);
    }

    private void onChartValueSelected(Description weekDesc, LineChart chart, String s) {
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                weekDesc.setText(String.format(Locale.US, s, e.getX(), e.getY()));
                chart.setDescription(weekDesc);
            }

            @Override
            public void onNothingSelected() {
                weekDesc.setText(getString(R.string.select_point));
                chart.setDescription(weekDesc);
            }
        });
        chart.setTouchEnabled(true);
        chart.getAxisRight().setEnabled(false);
    }


    /**
     * Finds the seven day minimum and maximum for the past seven days.
     *
     * @param srKeyArray Object[] Array of all the soil reading keys
     */
    private void sevenDayMinMax(Object[] srKeyArray) {
        Reading dayValue;
        int min = 0;
        int max = 0;
        for (Object k : srKeyArray) {
            dayValue = sr.stemma_1.get(k);
            assert dayValue != null;
            int moisture = Integer.parseInt(dayValue.soil.moisture);

            LocalDate valueDate = LocalDate.of(
                    Integer.parseInt(dayValue.date.year),
                    Integer.parseInt(dayValue.date.month),
                    Integer.parseInt(dayValue.date.day));

            // If the values date is after today's date minus 7 days.
            if (valueDate.isAfter(today.minusDays(7))) {
                if (min == 0 && max == 0) {
                    min = moisture;
                    max = moisture;
                } else if (moisture < min) {
                    min = moisture;
                } else if (moisture > min && moisture > max) {
                    max = moisture;
                }
            }
        }
    }

    /**
     * Populates the day entry list from the firebase database. The entry list is used to create the
     * data set for the day line chart.
     *
     * @param sr         SoilReading Hash Map from database
     * @param srKeyArray Object[] Array of all the soil reading keys
     * @param dayEntries List<Entry>  List of entries
     * @param today      LocalDate of today's date
     */
    private void populateDayEntryList(SoilReading sr, Object[] srKeyArray, List<Entry> dayEntries, LocalDate today) {
        for (Object k : srKeyArray) {
            Reading value = sr.stemma_1.get(k);
            String valueDate = value.date.year + value.date.month + value.date.day;
            String todayDate = today.toString().replace("-", "");

            if (valueDate.equals(todayDate)) {
                float tempX = Float.parseFloat(value.date.hour + value.date.minute) / 100;

                // Normalize time data
                float x = normalizeData(tempX);

                float y = Float.parseFloat(value.soil.moisture);

                dayEntries.add(new Entry(x, y));
            }
        }
    }

    /**
     * Populates the week entry list from the firebase database. The entry list is used to create
     * the ata set for the week line chart.
     *
     * @param srKeyArray  Object[] Array of all the soil reading keys
     * @param weekEntries List<Entry>  List of entries for the week
     */
    private void populateWeekEntryList(Object[] srKeyArray, List<Entry> weekEntries) {
        for (Object k : srKeyArray) {
            Reading weekValue = sr.stemma_1.get(k);
            int valueDayInt = Integer.parseInt(weekValue.date.day);
            int x, y;
            int increment = 1;

            // If the day of the month is between 1 and 7
            while (increment <= 7) {
                y = getDayAverage(sr, srKeyArray, valueDayInt);
                x = valueDayInt;
                increment++;

                if (!weekEntries.contains(new Entry(x, y))) {
                    weekEntries.add(new Entry(x, y));
                }
            }
        }
    }

    /**
     * Populates the month entry list from the firebase database. The entry list is used to create
     * the data set for the month line chart.
     *
     * @param srKeyArray   Object[] Array of all the soil reading keys
     * @param monthEntries List<Entry>  List of entries for the month
     * @param today        LocalDate Today's Local Date
     */
    private void populateMonthEntryList(Object[] srKeyArray, List<Entry> monthEntries, LocalDate today) {
        for (Object k : srKeyArray) {
            Reading monthValue = sr.stemma_1.get(k);
            int year = Integer.parseInt(monthValue.date.year);
            int month = Integer.parseInt(monthValue.date.month);
            int day = Integer.parseInt(monthValue.date.day);
            LocalDate date = LocalDate.of(year, month, day);
            int x, y;

            if (date.getMonthValue() == today.getMonthValue() && date.getYear() == today.getYear()) {
                y = getDayAverage(sr, srKeyArray, day);
                x = day;

                if (!monthEntries.contains(new Entry(x, y))) {
                    monthEntries.add(new Entry(x, y));
                }
            }
        }
    }

    /**
     * Create the set of data for the chart based off of the data entries
     *
     * @param entries List<Entry> List of entries
     * @param label   String
     * @return LineData
     */
    @NotNull
    private LineData createDataSet(List<Entry> entries, String label) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(4f);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawValues(false);
        return new LineData(dataSet);
    }

    /**
     * Creates the X and Y axes for the Line chart
     *
     * @param chart LineChart
     */
    private void createAxes(LineChart chart, boolean setMin, int minInt, boolean setMax, int maxInt) {
        // Create X Axis
        XAxis xAxis = chart.getXAxis();

        if (setMin)
            xAxis.setAxisMinimum(minInt);
        else
            xAxis.resetAxisMinimum();

        if (setMax)
            xAxis.setAxisMaximum(maxInt);
        else
            xAxis.resetAxisMinimum();

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(true);

        // Create Y Axis
        YAxis yAxis = chart.getAxisLeft();
        //yAxis.setAxisMinimum(750);
        //yAxis.setAxisMaximum(800);
    }

    /**
     * Normalizes the x coordinate data for the dayChart.
     *
     * @param tempX float that represents a temporary x coordinate
     * @return float
     */
    private float normalizeData(float tempX) {
        float x = 0f;
        if (tempX % 1 >= 0.5) {
            x = (float) Math.ceil(tempX);
        } else if (tempX % 1 < 0.5) {
            x = (float) Math.floor(tempX);
        } else if (tempX % 1 == 0) {
            x = tempX;
        }
        return x;
    }

    /**
     * Gets the average moisture for a given day of the month.
     *
     * @param sr         SoilReading Hash Map from database
     * @param srKeyArray Object[] Array of all the soil reading keys
     * @param day        int the given day of the month
     * @return int
     */
    private int getDayAverage(SoilReading sr, Object[] srKeyArray, int day) {
        Reading value;
        int avg = 0;
        int valueNumber = 0;

        for (Object k : srKeyArray) {
            value = sr.stemma_1.get(k);

            int valueDayInt = Integer.parseInt(value.date.day);
            if (valueDayInt == day) {
                valueNumber++;
                avg += Integer.parseInt(value.soil.moisture);
            }
        }
        return avg / valueNumber;
    }

    /**
     * Refreshes a specified chart
     *
     * @param chart    LineChart
     * @param lineData LineData
     */
    public static void refreshChart(LineChart chart, LineData lineData) {
        chart.setData(lineData);
        chart.notifyDataSetChanged();
        chart.fitScreen();
        chart.invalidate();
    }

}