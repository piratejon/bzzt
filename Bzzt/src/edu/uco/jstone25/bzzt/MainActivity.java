package edu.uco.jstone25.bzzt;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final String GET_CURRENT_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.GET_CURRENT_LOCATION";
	public static final String UPDATE_CURRENT_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.UPDATE_CURRENT_LOCATION";
	public static final String STOP_SERVICE = "uco.edu.jstone25.bzzt.MainActivity.STOP_SERVICE";
	
	private DataUpdateReceiver dataUpdateReceiver;
	
	private GoogleMap map;
	private Location location;
	
	@Override
	protected void onStart() {
		Log.d("MainActivity", "onStart");
		super.onStart();
	}
	
	@Override
	protected void onRestart() {
		Log.d("MainActivity", "onRestart");
		super.onRestart();
	}
	
	@Override
	protected  void onStop() {
		Log.d("MainActivity", "onStop");
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		Log.d("MainActivity", "onDestroy");
		stopService(new Intent(STOP_SERVICE, null, this, TestDataCollectionService.class));
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d("MainActivity", "onPause");
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d("MainActivity", "onResume");
		
		if(dataUpdateReceiver == null) {
			dataUpdateReceiver = new DataUpdateReceiver();
		}
		IntentFilter intentFilter = new IntentFilter(UPDATE_CURRENT_LOCATION);
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(dataUpdateReceiver, intentFilter);
		
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MainActivity", "onCreate");
		setContentView(R.layout.activity_main);
		
		Toast.makeText(getApplicationContext(), "Starting sensors ...", Toast.LENGTH_LONG).show();
		
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

		Intent mServiceIntent = new Intent(this, TestDataCollectionService.class);
		mServiceIntent.putExtra(GET_CURRENT_LOCATION, true);
		startService(mServiceIntent);
	}
	
	private void updateCurrentLocation(Location new_location) {
		// compute speed with lat/long
		Log.d("MainActivity", "Location updated to " + new_location.getLatitude() + "," + new_location.getLongitude());
		LatLng currentLatLng = new LatLng(new_location.getLatitude(), new_location.getLongitude());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 17);
		// map.animateCamera(cameraUpdate);
		map.moveCamera(cameraUpdate);
		map.addCircle(new CircleOptions().center(currentLatLng).radius(1).fillColor(Color.BLUE).strokeColor(Color.BLUE));
		location = new_location;
	}
	
	private class DataUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(UPDATE_CURRENT_LOCATION)) {
                updateCurrentLocation((Location) intent.getParcelableExtra("location"));
            }
		}
	}
}
