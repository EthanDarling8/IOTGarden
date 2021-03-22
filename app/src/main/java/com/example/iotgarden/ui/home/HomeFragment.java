package com.example.iotgarden.ui.home;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
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
        final TextView textView = root.findViewById(R.id.text_home);

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
                        String first = srKeyArray[0].toString();
                        Reading sRead = sr.stemma_1.get(first);

                        textView.setText(String.format("name: %s \n date: %s/%s/%s %s:00 \n" +
                                        "moisture: %s temperature: %s",
                                sRead.name, sRead.date.month, sRead.date.day, sRead.date.year, sRead.date.hour,
                                sRead.soil.moisture, sRead.soil.temperature));

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