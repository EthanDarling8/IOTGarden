package com.example.iotgarden.ui.home;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotgarden.R;
import com.example.iotgarden.recycler.RecyclerViewAdapter;
import com.example.iotgarden.stemma.Reading;
import com.example.iotgarden.stemma.SoilReading;
import com.example.iotgarden.stemma.Stemma;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerViewAdapter adapter;
    private DatabaseReference mDatabase;

    private static LineChart weekChart;
    private static LineChart dayChart;
    private static LineData dayLineData;
    private static LineData weekLineData;
    private final LocalDate today = LocalDate.now();
    private final LocalDate pastSeven = today.minusDays(7);

    private SoilReading sr;
    private final List<Stemma> stemmaList = new ArrayList<>();
    private final String TAG = "Home Fragment";

    public OnHomeFragmentListener mCallBack;

    public interface OnHomeFragmentListener {
        void onRefreshClicked();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OnHomeFragmentListener)
            mCallBack = (OnHomeFragmentListener) context;
        else
            throw new ClassCastException(context.toString() + "You must implement home fragment listener.");
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        dayChart = root.findViewById(R.id.dayChart);
        weekChart = root.findViewById(R.id.weekChart);

        initRecyclerView(root);

        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            public void onChanged(@Nullable String s) {
                mDatabase = FirebaseDatabase.getInstance().getReference();

                // Read from the database
                ValueEventListener soilListener = new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sr = snapshot.getValue(SoilReading.class);
                        assert sr != null;

                        Object[] srKeyArray = sr.stemma_1.keySet().toArray();
                        Arrays.sort(srKeyArray);
                        List<Entry> dayEntries = new ArrayList<>();
                        List<Entry> weekEntries = new ArrayList<>();

                        // Create custom chart axes for dayChart
                        createAxes(dayChart, true, 0, true, 24);
                        createAxes(weekChart, true, pastSeven.getDayOfMonth(),
                                true, today.minusDays(1).getDayOfMonth());

                        // Populate entry list for dayChart
                        Reading dayValue = null;
                        dayValue = populateDayEntryList(sr, srKeyArray, dayEntries, dayValue, today);

                        // Add value to recycler list
                        stemmaList.clear();
                        stemmaList.add(new Stemma(dayValue, dayValue.soil, sr, dayValue.date));
                        adapter.addItems(stemmaList);

                        // Populate entry list for weekChart
                        populateWeekEntryList(srKeyArray, weekEntries);

                        // Sort entries by date
                        dayEntries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));
                        weekEntries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));

                        // Find 7 day min and max
                        sevenDayMinMax(srKeyArray, root);

                        // Create data sets
                        dayLineData = createDataSet(dayEntries, "Moisture");
                        weekLineData = createDataSet(weekEntries, "Moisture");

                        // Create dayChart and set selection text
                        dayChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                            final TextView valueText = root.findViewById(R.id.valueText);

                            @Override
                            public void onValueSelected(Entry e, Highlight h) {
                                valueText.setText(String.format(Locale.US,
                                        "Selected: Time: %.0f:00 Moisture: %.0f",
                                        e.getX(), e.getY()));
                            }

                            @Override
                            public void onNothingSelected() {
                                valueText.setText(R.string.valueDetails);
                            }
                        });
                        dayChart.setTouchEnabled(true);
                        dayChart.getAxisRight().setEnabled(false);
                        dayChart.getDescription().setEnabled(false);
                        refreshDayChart();

                        // Create weekChart and set selection text
                        weekChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                            final TextView valueText = root.findViewById(R.id.valueText);

                            @Override
                            public void onValueSelected(Entry e, Highlight h) {
                                valueText.setText(String.format(Locale.US,
                                        "Selected: Day: %.0f Moisture: %.0f",
                                        e.getX(), e.getY()));
                            }

                            @Override
                            public void onNothingSelected() {
                                valueText.setText(R.string.valueDetails);
                            }
                        });
                        weekChart.setTouchEnabled(true);
                        weekChart.getAxisRight().setEnabled(false);
                        weekChart.getDescription().setEnabled(false);
                        refreshWeekChart();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "soilListener:onCancelled: ", error.toException());
                    }
                };
                mDatabase.addValueEventListener(soilListener);
            }
        });
        return root;
    }

    /**
     * Finds the seven day minimum and maximum for the past seven days.
     *
     * @param srKeyArray Object[] Array of all the soil reading keys
     * @param root       View for the HomeFragment
     */
    private void sevenDayMinMax(Object[] srKeyArray, View root) {
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

            // Set min max text
            TextView minMaxTxt = root.findViewById(R.id.minMaxTxt);
            minMaxTxt.setText(String.format(Locale.US, "7 Day Min/Max: %d/%d", min, max));
        }
    }

    /**
     * Populates the entry list from the firebase database. The entry list is used to create the
     * data set for the line chart.
     *
     * @param srKeyArray  Object[] Array of all the soil reading keys
     * @param weekEntries List<Entry>  List of entries for the week
     */
    private void populateWeekEntryList(Object[] srKeyArray, List<Entry> weekEntries) {
        Reading weekValue;
        for (Object k : srKeyArray) {
            weekValue = sr.stemma_1.get(k);
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
        dataSet.setDrawCircles(false);
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
     * Populates the entry list from the firebase database. The entry list is used to create the
     * data set for the line chart.
     *
     * @param sr         SoilReading Hash Map from database
     * @param srKeyArray Object[] Array of all the soil reading keys
     * @param entries    List<Entry>  List of entries
     * @param value      Reading that contains the name, date, and soil information.
     * @param today      LocalDate of today's date
     * @return Reading
     */
    private Reading populateDayEntryList(SoilReading sr, Object[] srKeyArray, List<Entry> entries, Reading value, LocalDate today) {
        for (Object k : srKeyArray) {
            value = sr.stemma_1.get(k);
            String valueDate = value.date.year + value.date.month + value.date.day;
            String todayDate = today.toString().replace("-", "");

            if (valueDate.equals(todayDate)) {
                float tempX = Float.parseFloat(value.date.hour + value.date.minute) / 100;

                // Normalize time data
                float x = normalizeData(tempX);

                float y = Float.parseFloat(value.soil.moisture);

                entries.add(new Entry(x, y));
            }
        }
        return value;
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
     * @param sr         SoilReading Hash Map from databaseg
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
     * Populates the entry list from the firebase database. The entry list is used to create the
     * data set for the line chart.
     *
     * @param sr         SoilReading Hash Map from database
     * @param srKeyArray Object[] Array of all the soil reading keys
     * @param entries    List<Entry>  List of entries
     * @param value      Reading that contains the name, date, and soil information.
     * @param today      LocalDate of today's date
     * @return Reading
     */
    private Reading populateWeekEntryList(SoilReading sr, Object[] srKeyArray, List<Entry> entries, Reading value, LocalDate today) {
        for (Object k : srKeyArray) {
            value = sr.stemma_1.get(k);

            int valueDayInt = Integer.parseInt(value.date.day);

            LocalDate valueDate = LocalDate.of(
                    Integer.parseInt(value.date.year),
                    Integer.parseInt(value.date.month),
                    valueDayInt);

            if (valueDate.isAfter(today.minusDays(8)) || valueDate.isBefore(today)) {
                int sum = 0;
                float tempX = 0f, x = 0f;

                int y = getDayAverage(sr, srKeyArray, valueDayInt);

                // Normalize time data
                x = normalizeData(tempX);

                entries.add(new Entry(x, y));
            }
        }
        return value;
    }

    /**
     * Initializes the recycler view and it's adapter.
     *
     * @param view View
     */
    private void initRecyclerView(View view) {
        Log.d(TAG, "iniRecyclerView: init recyclerview.");
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        adapter = new RecyclerViewAdapter(getContext(), stemmaList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * Refreshes the Week Chart when the FAB is clicked
     */
    public static void refreshWeekChart() {
        weekChart.setData(weekLineData);
        weekChart.notifyDataSetChanged();
        weekChart.fitScreen();
        weekChart.invalidate();
    }

    /**
     * Refreshes the Day Chart when the FAB is clicked
     */
    public static void refreshDayChart() {
        dayChart.setData(dayLineData);
        dayChart.notifyDataSetChanged();
        dayChart.fitScreen();
        dayChart.invalidate();
    }
}