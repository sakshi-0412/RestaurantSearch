package com.sakshi.restaurantsearchapp;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.sakshi.restaurantsearchapp.adapter.RestaurantListAdapter;
import com.sakshi.restaurantsearchapp.adapter.RestaurantListRealmAdapter;
import com.sakshi.restaurantsearchapp.model.Restaurant;
import com.sakshi.restaurantsearchapp.model.RestaurantResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    private RecyclerView rvRestaurantList;
    private TextView tvEmptyView;
    private RestaurantListRealmAdapter restaurantListRealmAdapter;
    private RestaurantListAdapter restaurantListAdapter;

    private ArrayList permissionsToRequest;
    private ArrayList permissionsRejected = new ArrayList();
    private ArrayList permissions = new ArrayList();

    private double cityLatitude;
    private double cityLongitude;

    private ArrayList<RestaurantResponse> restaurantResponseArrayList;

    private final static int ALL_PERMISSIONS_RESULT = 101;
    LocationTrack locationTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);

        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0)
                requestPermissions((String[]) permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                        ALL_PERMISSIONS_RESULT);
        }

        restaurantResponseArrayList = new ArrayList<>();


        initializeComponents();
    }

    private void getMobileLocation() {
        locationTrack = new LocationTrack(MainActivity.this);
        if (locationTrack.canGetLocation()) {

            double latitude = locationTrack.getLatitude();
            double longitude = locationTrack.getLongitude();

            cityLatitude = latitude;
            cityLongitude = longitude;

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                String cityName = addresses.get(0).getAddressLine(0);
                String stateName = addresses.get(0).getAddressLine(1);
                String countryName = addresses.get(0).getAddressLine(2);
                Log.e(TAG, "cityName:\t " + cityName + "\nstateName:\t" + stateName + "\ncountryName:\t" + countryName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "Longitude:\t " + longitude + "\nLatitude:\t" + latitude);

            //Toast.makeText(getApplicationContext(), "Longitude:" + longitude + "\nLatitude:" + latitude, Toast.LENGTH_SHORT).show();
        } else {

            locationTrack.showSettingsAlert();
        }
    }


    private ArrayList findUnAskedPermissions(ArrayList wanted) {
        ArrayList result = new ArrayList();

        for (Object perm : wanted) {
            if (!hasPermission((String) perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (Object perms : permissionsToRequest) {
                    if (!hasPermission((String) perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale((String) permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions((String[]) permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationTrack.stopListener();
    }

    private void initializeComponents() {
        tvEmptyView = findViewById(R.id.tvEmptyView);
        rvRestaurantList = findViewById(R.id.rvRestaurantList);
        rvRestaurantList.hasFixedSize();
        rvRestaurantList.setLayoutManager(new LinearLayoutManager(this));
        // recyclerView.setAdapter(adapter);
        //getMobileLocation();

        getMobileLocation();
        getLocation();
        getRestaurantList();

    }

    private void getLocation() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://developers.zomato.com/api/v2.1/locations?query=Surat";
        //String url = "https://developers.zomato.com/api/v2.1/search?&lat=27&lon=153";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        // Log.e("Response", response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.e("ERROR", "error => " + error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("user-key", "*********");
                params.put("Accept", "application/json");

                return params;
            }
        };
        queue.add(postRequest);
    }

    private void getRestaurantList() {
        RequestQueue queue = Volley.newRequestQueue(this);
        final MaterialDialog materialDialog = progressDialog();
        materialDialog.show();

        String url = "https://developers.zomato.com/api/v2.1/search?lat=" + cityLatitude + "&lon=" + cityLongitude;
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        materialDialog.dismiss();
                        //Log.e("Response", response.toString());
                        try {
                            String resultsFound = response.getString(Constants.ApiKeys.RESULT_FOUND);
                            JSONArray restaurantsArray = response.getJSONArray(Constants.ApiKeys.RESTAURANTS);

                            for (int restaurantsArrayCount = 0; restaurantsArrayCount < restaurantsArray.length(); restaurantsArrayCount++) {
                                JSONObject restaurantsObject = restaurantsArray.getJSONObject(restaurantsArrayCount);
                                Log.e(TAG, "JSONObject: " + restaurantsObject);
                                String name = restaurantsObject.getString(Constants.ApiKeys.NAME);
                                String url = restaurantsObject.getString(Constants.ApiKeys.URL);

                                JSONObject locationObject = restaurantsObject.getJSONObject(Constants.ApiKeys.LOCATION);
                                String locality = locationObject.getString(Constants.ApiKeys.LOCALITY);
                                String city = locationObject.getString(Constants.ApiKeys.CITY);

                                String cuisines = restaurantsObject.getString(Constants.ApiKeys.CUISINES);
                                String timings = restaurantsObject.getString(Constants.ApiKeys.TIMING);
                                String average_cost_for_two = restaurantsObject.getString(Constants.ApiKeys.AVERAGE_COST_FOR_TWO);
                                String thumb = restaurantsObject.getString(Constants.ApiKeys.THUMB);

                                JSONObject userRatingObject = restaurantsObject.getJSONObject(Constants.ApiKeys.USER_RATING);
                                String aggregate_rating = userRatingObject.getString(Constants.ApiKeys.AGGREGATE_RATING);

                                restaurantResponseArrayList = new ArrayList<>();

                                RestaurantResponse restaurantResponse = new RestaurantResponse();

                                restaurantResponse.setName(name);
                                restaurantResponse.setLocality(locality);
                                restaurantResponse.setCuisines(cuisines);
                                restaurantResponse.setTimings(timings);
                                restaurantResponse.setAverage_cost_for_two(average_cost_for_two);
                                restaurantResponse.setThumb(thumb);
                                restaurantResponse.setCity(city);
                                restaurantResponse.setAggregate_rating(aggregate_rating);

                                restaurantResponseArrayList.add(restaurantResponse);

                                restaurantListAdapter = new RestaurantListAdapter(MainActivity.this, restaurantResponseArrayList);
                                rvRestaurantList.setAdapter(restaurantListAdapter);

                                if (restaurantResponseArrayList.size() <= 0) {
                                    tvEmptyView.setVisibility(View.VISIBLE);
                                }


                            }


                            //TODO: NOTE: Do not use realm due to primary key issue in reponse
                           /* Realm realm = Realm.getDefaultInstance();
                            realm.executeTransactionAsync(new Realm.Transaction() {
                                @Override
                                public void execute(@NotNull Realm realm) {
                                    realm.createOrUpdateAllFromJson(Restaurant.class, restaurantsArray);
                                   // realm.createOrUpdateObjectFromJson(RestaurantResponse.class, response);
                                }
                            }, new Realm.Transaction.OnSuccess() {

                                @Override
                                public void onSuccess() {
                                        restaurantListAdapter = new RestaurantListAdapter(MainActivity.this, getMemberRealmList());
                                        rvRestaurantList.setAdapter(restaurantListAdapter);
                                }
                            }, new Realm.Transaction.OnError() {
                                @Override
                                public void onError(@NonNull Throwable error) {
                                    Log.e(TAG, "" + error);
                                }
                            });*/

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        materialDialog.dismiss();
                        Log.e("ERROR", "error => " + error.toString());
                        tvEmptyView.setVisibility(View.VISIBLE);
                        tvEmptyView.setText(error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("user-key", "*******");
                params.put("Accept", "application/json");

                return params;
            }
        };
        queue.add(postRequest);
    }

    private OrderedRealmCollection<Restaurant> getMemberRealmList() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Restaurant.class).findAll();
    }

    private MaterialDialog progressDialog() {

        return new MaterialDialog.Builder(this)
                .progress(true, 100)
                .content(getString(R.string.loading_details))
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (restaurantListAdapter != null) {
                    restaurantListAdapter.getFilter().filter(query);
                } else {
                    tvEmptyView.setVisibility(View.VISIBLE);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (restaurantListAdapter != null) {
                    restaurantListAdapter.getFilter().filter(query);
                } else {
                    tvEmptyView.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
