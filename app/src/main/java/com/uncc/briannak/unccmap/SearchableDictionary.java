package com.uncc.briannak.unccmap;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


/**
 * The main activity for the dictionary.
 * Displays search results triggered by the search dialog and handles
 * actions from search suggestions.
 * https://stackoverflow.com/questions/20052852/how-to-show-a-floating-view-on-google-map-just-like-the-search-bar-in-this-image
 */
public class SearchableDictionary extends AppCompatActivity implements OnMapReadyCallback
{
    private TextView mTextView;
    private ListView mListView;
    private GoogleMap mMap;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        getApplicationInfo().targetSdkVersion = 14;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mTextView = (TextView) findViewById(R.id.text);
        mListView = (ListView) findViewById(R.id.list);

        handleIntent(getIntent());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        map.addMarker(new MarkerOptions().position(new LatLng(35.306105, -80.73445)).title("Marker"));
        //map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.306105, -80.73445), 15));
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        // Because this activity has set launchMode="singleTop", the system calls this method
        // to deliver the intent if this activity is currently the foreground activity when
        // invoked again (when the user executes a search from this activity, we don't create
        // a new instance of this activity, so the system delivers the search intent here)
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) 
    {
        if (Intent.ACTION_VIEW.equals(intent.getAction()))
        {
            // handles a click on a search suggestion; launches activity to show word
            Intent wordIntent = new Intent(this, WordActivity.class);
            wordIntent.setData(intent.getData());
            startActivity(wordIntent);
        } 
        else if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            // handles a search query
            String query = intent.getStringExtra(SearchManager.QUERY);
            showResults(query);
        }
    }

    /**
     * Searches the dictionary and displays results for the given query.
     * @param query The search query
     */
    private void showResults(String query) 
    {

        Cursor cursor = getContentResolver().query(MapProvider.CONTENT_URI, null, null, new String[]{query}, null);
        //managedQuery(MapProvider.CONTENT_URI, null, null, new String[] {query}, null);

        if (cursor == null) 
        {
            // There are no results
            mTextView.setText(getString(R.string.no_results, new Object[] {query}));
        }
        else 
        {
            // Display the number of results
            int count = cursor.getCount();
            String countString = getResources().getQuantityString(R.plurals.search_results,
                                    count, new Object[] {count, query});
            mTextView.setText(countString);

            // Specify the columns we want to display in the result
            String[] from = new String[] { MapDatabase.SEARCH_CODE,
                                           MapDatabase.BUILDING_NAME};

            // Specify the corresponding layout elements where we want the columns to go
            int[] to = new int[] { R.id.code, R.id.building};

            // Create a simple cursor adapter for the definitions and apply them to the ListView
            SimpleCursorAdapter words = new SimpleCursorAdapter(this, R.layout.result, cursor, from, to);
            mListView.setAdapter(words);

            // Define the on-click listener for the list items
            mListView.setOnItemClickListener(new OnItemClickListener() 
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
                {
                    // Build the Intent used to open WordActivity with a specific word Uri
                    Intent wordIntent = new Intent(getApplicationContext(), WordActivity.class);
                    Uri data = Uri.withAppendedPath(MapProvider.CONTENT_URI, String.valueOf(id));
                    wordIntent.setData(data);
                    startActivity(wordIntent);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setIconified(false);
        searchView.onActionViewExpanded();
            //menu.findItem(R.id.search).setActionView(searchView);
            //searchView.onActionViewExpanded();


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
            default:
                return false;
        }
    }
}