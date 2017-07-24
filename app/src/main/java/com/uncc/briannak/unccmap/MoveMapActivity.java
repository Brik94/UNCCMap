package com.uncc.briannak.unccmap;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;

/**
 * Created by Bri on 7/23/2017.
 */

public class MoveMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        //Creates map fragment.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setMapType(MAP_TYPE_HYBRID);

        Uri uri = getIntent().getData();
        Cursor cursor =  getContentResolver().query(uri, null, null, null, null);

        if (cursor == null)
        {
            finish();
        }
        else {
            cursor.moveToFirst();

            int latitude = cursor.getColumnIndexOrThrow(MapDatabase.LATITUDE);
            int longitude = cursor.getColumnIndexOrThrow(MapDatabase.LONGITUDE);
            int bnameIndex = cursor.getColumnIndexOrThrow(MapDatabase.BUILDING_NAME);

            double latcoor = cursor.getDouble(latitude);
            double longcoor = cursor.getDouble(longitude);
            String bname = cursor.getString(bnameIndex);


            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latcoor, longcoor), 17));
            Marker building = map.addMarker(new MarkerOptions().position(new LatLng(latcoor, longcoor)).title(bname));
            building.showInfoWindow();

            //https://developers.google.com/maps/documentation/android-api/infowindows
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setIconified(false);
            searchView.clearFocus();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.search:
                onSearchRequested();
                return true;

            case android.R.id.home:
                Intent intent = new Intent(this, SearchableDictionary.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            default:
                return false;
        }
    }
}
