package edu.uco.jstone25.bzzt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import edu.uco.jstone25.bzzt.BrowseModeDialogFragment.BrowseModeListener;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements BrowseModeListener {
	
	public static final String GET_CURRENT_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.GET_CURRENT_LOCATION";
	public static final String UPDATE_CURRENT_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.UPDATE_CURRENT_LOCATION";
	public static final String STOP_SERVICE = "uco.edu.jstone25.bzzt.MainActivity.STOP_SERVICE";
	public static final String BROWSE_LONGTOUCH_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.BROWSE_LONGTOUCH_LOCATION";
	public static final String BZZT_IS_SERVICE_RUNNING = "uco.edu.jstone25.bzzt.MainActivity.BZZT_IS_SERVICE_RUNNING";
	public static final String BZZT_CAMERA_POSITION_ZOOM = "uco.edu.jstone25.bzzt.MainActivity.BZZT_CAMERA_POSITION_ZOOM";
	
	private static final int BZZT_SERVICE_STATUS_NOTIFICATION_ID = 0;
	
	private DataUpdateReceiver dataUpdateReceiver;
	
	private GoogleMap map;
	private Location location;
	
	private static Class<?> myServiceClass = edu.uco.jstone25.bzzt.TestDataCollectionService.class;

	/*
	 * From: <http://stackoverflow.com/a/5921190> accessed 2014-11-10
	 */
	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		Log.d("MainActivity", "onStart");
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();

		Log.d("MainActivity", "onRestart");
	}
	
	@Override
	protected  void onStop() {
		super.onStop();

		Log.d("MainActivity", "onStop");
	}
	
	@Override
	protected void onDestroy() {
		Log.d("MainActivity", "onDestroy");
		
		super.onDestroy();
	}

	private void setupNotification() {
		Notification.Builder nbServiceStatus = new Notification.Builder(getApplicationContext())
			.setTicker("Bzzt capture service started.")
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("Bzzt! Capturing Data (content title)")
			.setContentText("Content Text")
			.setOngoing(true)
			.setUsesChronometer(true)
			.setAutoCancel(false)
			// .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.uco))
			.setContentIntent(
				PendingIntent.getActivity(
					this, 0,
					new Intent(this, MainActivity.class)
						.putExtra(BZZT_CAMERA_POSITION_ZOOM, map.getCameraPosition().zoom)
					, PendingIntent.FLAG_UPDATE_CURRENT));

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(
			BZZT_SERVICE_STATUS_NOTIFICATION_ID, nbServiceStatus.build());
	}
	
	private void concludeNotification() {
		Notification.Builder nbServiceStatus = new Notification.Builder(getApplicationContext())
			.setTicker("Bzzt capture service stopped.")
			.setSmallIcon(R.drawable.ic_launcher)
			.setAutoCancel(false)
			.setContentIntent(null);

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(
			BZZT_SERVICE_STATUS_NOTIFICATION_ID, nbServiceStatus.build());

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(BZZT_SERVICE_STATUS_NOTIFICATION_ID);
	}

	@Override
	protected void onPause() {
		super.onPause();

		Log.d("MainActivity", "onPause");
		
		unregisterFromLocationUpdates();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(isMyServiceRunning(myServiceClass)) {
			registerForLocationUpdates();
		} else {
			dataUpdateReceiver = null;
		}

		Log.d("MainActivity", "onResume");
	}
	
	private void registerForLocationUpdates() {
		dataUpdateReceiver = new DataUpdateReceiver();
		IntentFilter intentFilter = new IntentFilter(UPDATE_CURRENT_LOCATION);
		LocalBroadcastManager.getInstance(this).registerReceiver(dataUpdateReceiver, intentFilter);
		Log.d("Bzzt", "Registered receiver: " + dataUpdateReceiver);
	}
	
	private void unregisterFromLocationUpdates() {
		Log.d("Bzzt", "Unregistering receiver: " + dataUpdateReceiver);
		// if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
		dataUpdateReceiver = null;
		Log.d("Bzzt", "Successfully unregistered receiver." + dataUpdateReceiver);
	}
	
	private void drawCoordinates() {
		BufferedReader br = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.gpsacc)));
		try {
			LatLng loc = null;
			// nanosecondOffset = Long.parseLong(br.readLine().split(":",0)[0],10) - SystemClock.elapsedRealtimeNanos();
			int i = 0;
			for(String line; (line = br.readLine()) != null && i < 500; i += 1 ) {
				String[] bits = line.split(",");
				// 54575102795729,35.44272711,-97.59743783,1
				// color is last component, 2=red, 1=yellow, 0=green
				int color = Integer.parseInt(bits[3]);
				loc = new LatLng(Double.parseDouble(bits[1]), Double.parseDouble(bits[2]));
				addPointToMap(loc, color == 0 ? Color.GREEN : ( color == 1 ? Color.YELLOW : Color.RED));
			}
			if (loc != null) putMapOverPoint(loc);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void animateMapTo(Location loc, float zoom) {
		LatLng currentLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, zoom > 0 ? zoom : map.getCameraPosition().zoom);
		map.moveCamera(cameraUpdate);
	}
	
	private void setCurrentMapLocation() {
		setCurrentMapLocation(-1);
	}

	private void setCurrentMapLocation(float zoom) {
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = lm.getBestProvider(crit, true);
		if (provider != null) {
			animateMapTo(lm.getLastKnownLocation(provider), zoom);
		} else {
			Toast.makeText(getApplicationContext(), "Failed to get last known location", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MainActivity", "onCreate: " + savedInstanceState);
		setContentView(R.layout.activity_main);
		
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		
		map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
			@Override
			public void onMapLongClick(LatLng loc) {
				// browse mode, launch action fragment for capture/playback or view/add hazard
				BrowseModeDialogFragment bmdf = new BrowseModeDialogFragment();
				Bundle browseModeFragmentArguments = new Bundle();
				browseModeFragmentArguments.putBoolean(BZZT_IS_SERVICE_RUNNING, isMyServiceRunning(myServiceClass));
				browseModeFragmentArguments.putParcelable(BROWSE_LONGTOUCH_LOCATION, loc);
				bmdf.setArguments(browseModeFragmentArguments);
				bmdf.show(getFragmentManager(), "lol");
			}
		});
		
		onNewIntent(getIntent());
		
		if (isMyServiceRunning(myServiceClass)) {
			registerForLocationUpdates();
		} else {
			setCurrentMapLocation(17);
		}
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				setCurrentMapLocation(extras.getFloat(BZZT_CAMERA_POSITION_ZOOM));
			}
		}
	}
	
	private void addPointToMap(LatLng loc, int c) {
		map.addCircle(new CircleOptions().center(loc).radius(1).fillColor(c).strokeColor(c));
	}
	
	private void putMapOverPoint(LatLng loc) {
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(loc, 17);
		map.animateCamera(cameraUpdate);
	}

	private void startDataListenerService() {
		Intent mServiceIntent = new Intent(this, myServiceClass);
		mServiceIntent.putExtra(GET_CURRENT_LOCATION, true);
		if(null != startService(mServiceIntent)) {
            setupNotification();
            registerForLocationUpdates();
		} else {
			Toast.makeText(getApplicationContext(), "Failed to start data capture service.", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void stopDataListenerService() {
		if(stopService(new Intent(STOP_SERVICE, null, this, myServiceClass))) {
			// Toast.makeText(getApplicationContext(), "Stopped data capture service.", Toast.LENGTH_SHORT).show();
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(BZZT_SERVICE_STATUS_NOTIFICATION_ID);
			concludeNotification();
            unregisterFromLocationUpdates();
		} else {
			Toast.makeText(getApplicationContext(), "Failed to stop data capture service.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onBrowseMenuSelect(int position, DialogFragment df) {
		switch(position) {
		case 0:
			if (isMyServiceRunning(myServiceClass)) {
                stopDataListenerService();
			} else {
                startDataListenerService();
			}
			break;

		case 1:
			Toast.makeText(getApplicationContext(), "warm fuzzies", Toast.LENGTH_SHORT).show();
			break;

		default:
		}
	}
	
	private void updateCurrentLocation(Location new_location, float zoom) {
		// compute speed with lat/long
		Log.d("MainActivity", "Location updated to " + new_location.getLatitude() + "," + new_location.getLongitude());
		LatLng currentLatLng = new LatLng(new_location.getLatitude(), new_location.getLongitude());
		animateMapTo(new_location, zoom);
		map.addCircle(new CircleOptions().center(currentLatLng).radius(1).fillColor(Color.BLUE).strokeColor(Color.BLUE));
		location = new_location;
	}
	
	private class DataUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(UPDATE_CURRENT_LOCATION)) {
            	updateCurrentLocation((Location)intent.getParcelableExtra("location"), -1);
            }
		}
	}
}
