package com.example.iotgarden.ui.home;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mDatabase = FirebaseDatabase.getInstance().getReference();

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

                        XAxis xAxis = chart.getXAxis();
                        xAxis.setAxisMinimum(0);
                        xAxis.setAxisMaximum(24);

                        YAxis yAxis = chart.getAxisLeft();
                        yAxis.setAxisMinimum(700);
                        yAxis.setAxisMaximum(800);

                        for (Object k : srKeyArray) {
                            Reading value = sr.stemma_1.get(k);
                            float tempX = Float.parseFloat(value.date.hour + value.date.minute) / 100;

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

                        entries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));

                        LineDataSet dataSet = new LineDataSet(entries, "Moisture");
                        dataSet.setDrawCircles(false);

                        LineData lineData = new LineData(dataSet);

                        chart.setData(lineData);
                        chart.invalidate(); // refresh

                        /*textView.setText(String.format("name: %s \n date: %s/%s/%s %s:00 \n" +
                                        "moisture: %s temperature: %s",
                                sRead.name, sRead.date.month, sRead.date.day, sRead.date.year, sRead.date.hour,
                                sRead.soil.moisture, sRead.soil.temperature));*/

                        /*for (Reading r : sr.stemma_1.values()) {
                            Log.d(TAG, String.format("name: %s day: %s moisture: %s",
                                    r.name, r.date.day, r.soil.moisture));
                            textView.setText(String.format("name: %s \n date: %s/%s/%s %s:00 \n" +
                                            "moisture: %s temperature: %s",
                                    r.name, r.date.month, r.date.day, r.date.year, r.date.hour,
                                    r.soil.moisture, r.soil.temperature));
                        }*/

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