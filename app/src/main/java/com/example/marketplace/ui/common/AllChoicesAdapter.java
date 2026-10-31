package com.example.marketplace.ui.common;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A dropdown list that always shows every choice. The default one hides the other choices once
 * one is selected, which makes changing your mind confusing.
 */
public class AllChoicesAdapter extends ArrayAdapter<String> {

    private final List<String> all;

    public AllChoicesAdapter(@NonNull Context context, @NonNull List<String> items) {
        super(context, android.R.layout.simple_list_item_1, new ArrayList<>(items));
        this.all = new ArrayList<>(items);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = all;
                results.count = all.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
