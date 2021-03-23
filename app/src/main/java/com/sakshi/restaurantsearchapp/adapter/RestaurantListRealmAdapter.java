package com.sakshi.restaurantsearchapp.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sakshi.restaurantsearchapp.model.Restaurant;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

public class RestaurantListRealmAdapter extends RealmRecyclerViewAdapter<Restaurant, RestaurantListRealmAdapter.ViewHolder> {
    private String TAG = "RestaurantListAdapter";
    private Context context;
    private String contactNo = "";
    private Realm realm;

    public RestaurantListRealmAdapter(Context context, OrderedRealmCollection<Restaurant> realmCollection) {
        super(realmCollection, true);
        this.context = context;
        realm = Realm.getDefaultInstance();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        String id = getItem(viewHolder.getAdapterPosition()).getPhotos().toString();
        Log.e(TAG, "Id:  " + id);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
