package pub.gusten.gbgcommuter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pub.gusten.gbgcommuter.models.Departure;
import pub.gusten.gbgcommuter.models.Route;
import pub.gusten.gbgcommuter.models.Stop;
import pub.gusten.gbgcommuter.services.ApiService;
import pub.gusten.gbgcommuter.services.TrackerService;

public class MainActivity extends AppCompatActivity {


    private ApiService apiService;
    private boolean hasBoundApiService;
    private ServiceConnection apiConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            hasBoundApiService = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            hasBoundApiService = true;
            ApiService.LocalBinder mLocalBinder = (ApiService.LocalBinder)service;
            apiService = mLocalBinder.getService();
        }
    };

    private TrackerService tracker;
    private boolean hasBoundTracker;
    private ServiceConnection trackerConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            hasBoundTracker = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            hasBoundTracker = true;
            TrackerService.LocalBinder mLocalBinder = (TrackerService.LocalBinder)service;
            tracker = mLocalBinder.getService();
        }
    };

    private List<Stop> availableStops;
    private Stop selectedFrom;
    private Stop selectedTo;
    private Departure selectedDeparture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set ui components
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if (hasBoundTracker && selectedDeparture != null && selectedFrom != null && selectedTo != null) {
                tracker.startTracking(new Route(selectedFrom.name, Long.toString(selectedFrom.id), selectedTo.name, Long.toString(selectedTo.id), selectedDeparture.line));
            }
        });

        // Load locations from JSON file
        loadLocations();

        StopArrayAdapter adapter = new StopArrayAdapter(this, android.R.layout.simple_list_item_1, availableStops);

        AutoCompleteTextView fromSelector = (AutoCompleteTextView)findViewById(R.id.main_from_location);
        fromSelector.setAdapter(adapter);
        fromSelector.setThreshold(1);
        fromSelector.setOnItemClickListener((parent, arg1, pos, id) -> {
            selectedFrom = adapter.getItem(pos);
            listLines();
        });

        AutoCompleteTextView toSelector = (AutoCompleteTextView)findViewById(R.id.main_to_location);
        toSelector.setAdapter(adapter);
        toSelector.setThreshold(1);
        toSelector.setOnItemClickListener((parent, arg1, pos, id) -> {
            selectedTo = adapter.getItem(pos);
            listLines();
        });

        // Start necessary services
        startService(new Intent(this, TrackerService.class));
        bindService(new Intent(this, TrackerService.class), trackerConnection, BIND_AUTO_CREATE);

        startService(new Intent(this, ApiService.class));
        bindService(new Intent(this, ApiService.class), apiConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void listLines() {
        if (!hasBoundApiService || selectedTo == null || selectedFrom == null) {
            return;
        }

        final Context contextRef = this;
        apiService.fetchDepartures(Long.toString(selectedFrom.id), Long.toString(selectedTo.id), new ApiService.DeparturesRequest() {
            @Override
            public void onRequestCompleted(List<Departure> departures) {
                ListView listView = (ListView) findViewById(R.id.available_routes_list);
                DepartureAdapter adapter = new DepartureAdapter(contextRef, departures);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    selectedDeparture = adapter.getItem(position);
                });
            }

            @Override
            public void onRequestFailed() {

            }
        });
    }

    private void loadLocations() {
        availableStops = new ArrayList<>();

        try {
            InputStream inputStream = getAssets().open("locations.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];

            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            JSONArray locations = new JSONArray(json);

            for(int i = 0; i < locations.length(); i++) {
                JSONObject tmp = locations.getJSONObject(i);
                Stop tmpStop = new Stop(
                    tmp.getString("name"),
                    tmp.getLong("id"),
                    tmp.getDouble("lat"),
                    tmp.getDouble("lon"),
                    tmp.getInt("weight")
                );
                availableStops.add(tmpStop);
            }
        } catch (IOException | JSONException ex) {
            ex.printStackTrace();
        }
    }
}
