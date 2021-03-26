package com.example.iotgarden.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private final String TAG = "Home Fragment";
    private DatabaseReference mDatabase;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
//        final TextView textView = root.findViewById(R.id.text_home);
        LineChart chart = (LineChart) root.findViewById(R.id.chart);

        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mDatabase = FirebaseDatabase.getInstance().getReference();

                // Read from the database
                ValueEventListener soilListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        SoilReading sr = snapshot.getValue(SoilReading.class);
                        assert sr != null;

                        Object[] srKeyArray = sr.stemma_1.keySet().toArray();
                        Arrays.sort(srKeyArray);
                        List<Entry> entries = new ArrayList<Entry>();

                        XAxis xAxis = chart.getXAxis();
                        xAxis.setAxisMinimum(0);
                        xAxis.setAxisMaximum(24);

                        YAxis yAxis = chart.getAxisLeft();
                        yAxis.setAxisMinimum(700);
                        yAxis.setAxisMaximum(800);

                        for (Object k : srKeyArray) {
                            Reading value = sr.stemma_1.get(k);
                            float x = Float.parseFloat(value.date.hour + value.date.minute) / 100;
                            float y = Float.parseFloat(value.soil.moisture);
                            entries.add(new Entry(x, y));
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Moisture");
                        dataSet.setValueTextColor(255);

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