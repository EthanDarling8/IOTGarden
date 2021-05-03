package com.example.iotgarden.ui.home;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.iotgarden.R;
import com.example.iotgarden.stemma.Reading;
import com.example.iotgarden.stemma.SoilReading;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PlantEditActivity extends AppCompatActivity {

    String stemmaName, nameInputString;
    private TextInputEditText nameInput;
    private SoilReading sr;

    private final String TAG = "Plant Edit Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_plant_edit);
        setTitle("Edit Stemma");
        nameInput = findViewById(R.id.edit_input_name);
        stemmaName = getIntent().getStringExtra("SESSION_NAME");

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        FloatingActionButton fab = findViewById(R.id.fab_edit_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValueEventListener soilListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sr = snapshot.getValue(SoilReading.class);
                        assert sr != null;

                        Set<String> srKeySet = sr.stemma_1.keySet();
                        for (String k : srKeySet) {
                            if (Objects.requireNonNull(sr.stemma_1.get(k)).name.equals(stemmaName)) {
                                Reading reading = sr.stemma_1.get(k);
                                String date = reading.date.year + reading.date.month
                                        + reading.date.day + reading.date.hour
                                        + reading.date.minute;
                                mDatabase.child("stemma_1").child(date).child("name")
                                        .setValue(nameInput.getText().toString());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "soilListener:onCancelled: ", error.toException());
                    }
                };
                mDatabase.addValueEventListener(soilListener);

                Intent intent = new Intent(PlantEditActivity.this, PlantDetailActivity.class);
                intent.putExtra("SESSION_NAME", nameInput.getText().toString());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        nameInput.setText(stemmaName);
    }
}