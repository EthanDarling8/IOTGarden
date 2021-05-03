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

public class PlantsFragment extends Fragment {

    View root;
    private RecyclerViewAdapter adapter;
    private DatabaseReference mDatabase;

    private final LocalDate today = LocalDate.now();

    private SoilReading sr;
    private final List<Stemma> stemmaList = new ArrayList<>();
    private final String TAG = "Plant Fragment";

    public OnHomeFragmentListener mCallBack;
    private Object[] srKeyArray;

    public interface OnHomeFragmentListener {
        void onRefreshClicked();
        void stemmaClicked(String name, View v);
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
        PlantsViewModel plantsViewModel = new ViewModelProvider(this).get(PlantsViewModel.class);
        root = inflater.inflate(R.layout.fragment_plants, container, false);

        plantsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            public void onChanged(@Nullable String s) {
                mDatabase = FirebaseDatabase.getInstance().getReference();

                // Read from the database
                ValueEventListener soilListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sr = snapshot.getValue(SoilReading.class);
                        assert sr != null;

                        srKeyArray = sr.stemma_1.keySet().toArray();
                        Arrays.sort(srKeyArray);
                        List<Entry> dayEntries = new ArrayList<>();

                        // Populate entry list for dayChart
                        Reading dayValue = null;
                        dayValue = populateDayEntryList(sr, srKeyArray, dayEntries, dayValue, today);

                        initRecyclerView(root);

                        adapter.setOnStemmaClickListener(new RecyclerViewAdapter.ClickListener() {
                            @Override
                            public void onStemmaClick(String name, View v) {
                                mCallBack.stemmaClicked(name, v);
                            }
                        });

                        // Add value to recycler list
                        stemmaList.clear();
                        stemmaList.add(new Stemma(dayValue, dayValue.soil, sr, dayValue.date));
                        adapter.addItems(stemmaList);
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
     * Finds the seven day minimum and maximum for the past seven days.
     *
     * @param srKeyArray Object[] Array of all the soil reading keys
     * @return String
     */
    private String sevenDayMinMax(Object[] srKeyArray) {
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
        return String.format(Locale.US, "Min/Max: %d/%d", min, max) ;
    }

    /**
     * Initializes the recycler view and it's adapter.
     *
     * @param view View
     */
    private void initRecyclerView(View view) {
        String minMax = sevenDayMinMax(srKeyArray);
        Log.d(TAG, "iniRecyclerView: init recyclerview.");
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        adapter = new RecyclerViewAdapter(getContext(), stemmaList, minMax);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}