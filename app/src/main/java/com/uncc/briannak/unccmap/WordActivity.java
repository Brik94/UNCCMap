package com.uncc.briannak.unccmap;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.TextView;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;



/**
 * Displays a word and its definition.
 */
public class WordActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Uri uri = getIntent().getData();
        Cursor cursor =  getContentResolver().query(uri, null, null, null, null);

        if (cursor == null) 
        {
            finish();
        } 
        else 
        {
            cursor.moveToFirst();

            TextView word = (TextView) findViewById(R.id.code);
            TextView definition = (TextView) findViewById(R.id.building);
            TextView coordinates = (TextView) findViewById(R.id.coordinates);

            int codeIndex = cursor.getColumnIndexOrThrow(MapDatabase.SEARCH_CODE);
            int bnameIndex = cursor.getColumnIndexOrThrow(MapDatabase.BUILDING_NAME);


            int latitude = cursor.getColumnIndexOrThrow(MapDatabase.LATITUDE);
            int longitude = cursor.getColumnIndexOrThrow(MapDatabase.LONGITUDE);

            word.setText(cursor.getString(codeIndex));
            definition.setText(cursor.getString(bnameIndex));
            coordinates.setText(cursor.getString(latitude) + " , " + cursor.getString(longitude));

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