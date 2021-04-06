package com.example.iotgarden.ui.home;

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

import com.example.iotgarden.R;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private final String TAG = "Home Fragment";
    private DatabaseReference mDatabase;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        LineChart chart = (LineChart) root.findViewById(R.id.chart);
        TextView stemmaOne = root.findViewById(R.id.chartTitle);

        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onChanged(@Nullable String s) {
                mDatabase = FirebaseDatabase.getInstance().getReference();
                LocalDate today = LocalDate.now();
                LocalDate pastSeven = today.minusDays(7);

                // Read from the database
                ValueEventListener soilListener = new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        SoilReading sr = snapshot.getValue(SoilReading.class);
                        assert sr != null;

                        Object[] srKeyArray = sr.stemma_1.keySet().toArray();
                        Arrays.sort(srKeyArray);
                        List<Entry> entries = new ArrayList<>();

                        // Create X Axis
                        XAxis xAxis = chart.getXAxis();
                        xAxis.setAxisMinimum(0);
                        xAxis.setAxisMaximum(24);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setDrawLabels(true);

                        // Create Y Axis
                        YAxis yAxis = chart.getAxisLeft();
                        //yAxis.setAxisMinimum(750);
                        //yAxis.setAxisMaximum(800);

                        Reading value = null;

                        // Populate entry list
                        for (Object k : srKeyArray) {
                            value = sr.stemma_1.get(k);
                            String valueDate = value.date.year + value.date.month + value.date.day;
                            String todayDate = today.toString().replace("-", "");

                            if (valueDate.equals(todayDate)) {
                                float tempX = Float.parseFloat(value.date.hour + value.date.minute) / 100;

                                // Normalize time data
                                float x = 0f;
                                if (tempX % 1 >= 0.5) {
                                    x = (float) Math.ceil(tempX);
                                } else if (tempX % 1 < 0.5) {
                                    x = (float) Math.floor(tempX);
                                } else if (tempX % 1 == 0) {
                                    x = tempX;
                                }

                                float y = Float.parseFloat(value.soil.moisture);

                                entries.add(new Entry(x, y));
                            }
                        }

                        // Set chart title text
                        assert value != null;
                        stemmaOne.setText(value.name);

                        // Sort entries by date
                        entries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));

                        int min = 0;
                        int max = 0;

                        // Find 7 day min and max
                        for (Object k : srKeyArray) {
                            value = sr.stemma_1.get(k);
                            assert value != null;
                            int moisture = Integer.parseInt(value.soil.moisture);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                LocalDate valueDate = LocalDate.of(
                                        Integer.parseInt(value.date.year),
                                        Integer.parseInt(value.date.month),
                                        Integer.parseInt(value.date.day));

                                // If the values date is after today's date minus 7 days.
                                if (valueDate.isAfter(today.minusDays(7))) {
                                    if (min == 0 && max == 0) {
                                        min = moisture;
                                        max = moisture;
                                    }
                                    else if (moisture < min) {
                                        min = moisture;
                                    }
                                    else if (moisture > min && moisture > max) {
                                        max = moisture;
                                    }
                                }
                            }
                        }

                        // Set min max text
                        TextView minText = root.findViewById(R.id.minTxt);
                        minText.setText(String.format(Locale.US, "7 Day Min: %d", min));
                        TextView maxText = root.findViewById(R.id.maxTxt);
                        maxText.setText(String.format(Locale.US, "7 Day Max: %d", max));

                        // Set recent reading text
                        TextView recentReading = root.findViewById(R.id.recentReading);
                        recentReading.setText(String.format(Locale.US,
                                "Current: %.0f",
                                entries.get(entries.size() - 1).getY()));

                        // Create data set
                        LineDataSet dataSet = new LineDataSet(entries, "Moisture");
                        dataSet.setDrawCircles(false);
                        dataSet.setLineWidth(4f);
                        dataSet.setValueTextSize(9f);
                        dataSet.setDrawValues(false);

                        LineData lineData = new LineData(dataSet);

                        // Create chart and set selection text
                        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
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

                        chart.setTouchEnabled(true);
                        chart.getAxisRight().setEnabled(false);
                        chart.setData(lineData);
                        chart.getDescription().setEnabled(false);
                        chart.invalidate(); // refresh
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
}