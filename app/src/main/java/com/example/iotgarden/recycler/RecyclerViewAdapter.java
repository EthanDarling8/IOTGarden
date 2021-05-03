/*
  Author: Ethan Darling
  RecyclerViewAdapter.java
 */

package com.example.iotgarden.recycler;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotgarden.R;
import com.example.iotgarden.stemma.Stemma;

import java.util.List;
import java.util.Locale;

/**
 * Recycler view to display all of the sounds in the room database.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private final List<Stemma> stemmaList;
    private final String minMax;

    private Context context;
    private static ClickListener clickListener;
    private static final String TAG = "RecyclerViewAdapter";

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, current, minMax;
        CardView parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.item_stemma_name);
            current = itemView.findViewById(R.id.item_stemma_current);
            minMax = itemView.findViewById(R.id.item_stemma_min_max);
            parentLayout = itemView.findViewById(R.id.item_parent_layout);
        }
    }

    public RecyclerViewAdapter(Context context, List<Stemma> stemmaList, String minMax) {
        this.context = context;
        this.stemmaList = stemmaList;
        this.minMax = minMax;
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
        holder.current.setText(String.format(Locale.US, "Current: %s", stemma.reading.soil.moisture));
        holder.minMax.setText(minMax);

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onStemmaClick(stemma.reading.name, holder.parentLayout);
            }
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

    public void setOnStemmaClickListener(ClickListener clickListener) {
        RecyclerViewAdapter.clickListener = clickListener;
    }

    /**
     * Interface for when an item in the recycler view is clicked on.
     */
    public interface ClickListener {
        void onStemmaClick(String name, View v);
    }
}
