/*
  Author: Ethan Darling
  Class: CS 3270
  RecyclerViewAdapter.java
 */

package com.example.iotgarden.recycler;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotgarden.R;
import com.example.iotgarden.stemma.Stemma;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Recycler view to display all of the sounds in the room database.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    // Fields
    private List<Stemma> stemmaList;

    private Context context;
    private static final String TAG = "RecyclerViewAdapter";

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, current;
        CardView parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.item_stemma_name);
            current = itemView.findViewById(R.id.item_stemma_current);
            parentLayout = itemView.findViewById(R.id.item_parent_layout);
        }
    }

    public RecyclerViewAdapter(Context context, List<Stemma> stemmaList) {
        this.context = context;
        this.stemmaList = stemmaList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called.");
        Stemma stemma = stemmaList.get(position);
        holder.name.setText(stemma.reading.name);
        holder.current.setText(stemma.reading.soil.moisture);

        holder.parentLayout.setOnClickListener(v -> {

        });
    }

    @Override
    public int getItemCount() {
        return stemmaList.size();
    }


    public void addItems(List<Stemma> stemmas) {
        notifyDataSetChanged();
        Log.d(TAG, "new Items Added: " + stemmas.toString() + "\n");
    }
}
