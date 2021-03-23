package com.sakshi.restaurantsearchapp.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sakshi.restaurantsearchapp.GlideApp;
import com.sakshi.restaurantsearchapp.R;
import com.sakshi.restaurantsearchapp.model.RestaurantResponse;

import java.util.ArrayList;
import java.util.List;

public class RestaurantListAdapter extends RecyclerView.Adapter<RestaurantListAdapter.ViewHolder>  implements Filterable {

    private Context context;
    private ArrayList<RestaurantResponse> restaurantResponseArrayList;
    private List<RestaurantResponse> restaurantResponseFiltered;

    public RestaurantListAdapter(Context context, ArrayList<RestaurantResponse> restaurantResponseArrayList) {
        this.context = context;
        this.restaurantResponseArrayList = restaurantResponseArrayList;
        this.restaurantResponseFiltered = restaurantResponseArrayList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_restaurant, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        holder.tvRestaurantName.setText(restaurantResponseArrayList.get(position).getName());
        holder.tvLocation.setText(restaurantResponseArrayList.get(position).getLocality());
        holder.tvAverageCost.setText(restaurantResponseArrayList.get(position).getAverage_cost_for_two());
        holder.tvOpenTiming.setText(restaurantResponseArrayList.get(position).getTimings());
        holder.tvCuisines.setText(restaurantResponseArrayList.get(position).getCuisines());

        GlideApp
                .with(context)
                .load(restaurantResponseArrayList.get(position).getThumb())
                .into(holder.imageFood);

    }

    @Override
    public int getItemCount() {
        return restaurantResponseArrayList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    restaurantResponseFiltered = restaurantResponseArrayList;
                } else {
                    List<RestaurantResponse> filteredList = new ArrayList<>();
                    for (RestaurantResponse row : restaurantResponseArrayList) {
                        if (row.getName().toLowerCase().contains(charString.toLowerCase()) || row.getCity().contains(charSequence)) {
                            filteredList.add(row);
                        }
                    }

                    restaurantResponseFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = restaurantResponseFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                restaurantResponseFiltered = (ArrayList<RestaurantResponse>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageFood;
        private TextView tvRestaurantName;
        private TextView tvLocation;
        private TextView tvAverageCost;
        private TextView tvOpenTiming;
        private TextView tvCuisines;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageFood = itemView.findViewById(R.id.imageFood);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvAverageCost = itemView.findViewById(R.id.tvAverageCost);
            tvOpenTiming = itemView.findViewById(R.id.tvOpenTiming);
            tvCuisines = itemView.findViewById(R.id.tvCuisines);
        }
    }
}
