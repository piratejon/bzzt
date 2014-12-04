package edu.uco.jstone25.bzzt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.quadtree.PointQuadTree;

import edu.uco.jstone25.bzzt.BrowseModeDialogFragment.BrowseModeListener;
import edu.uco.jstone25.bzzt.InsertNoticeDialogFragment.InsertNoticeListener;

public class MainActivity extends Activity implements BrowseModeListener, InsertNoticeListener, QueryPointsListener<AccelerationPoint> {
	
	public static final String GET_CURRENT_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.GET_CURRENT_LOCATION";
	public static final String UPDATE_CURRENT_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.UPDATE_CURRENT_LOCATION";
	public static final String STOP_SERVICE = "uco.edu.jstone25.bzzt.MainActivity.STOP_SERVICE";
	public static final String BROWSE_LONGTOUCH_LOCATION = "uco.edu.jstone25.bzzt.MainActivity.BROWSE_LONGTOUCH_LOCATION";
	public static final String BZZT_IS_SERVICE_RUNNING = "uco.edu.jstone25.bzzt.MainActivity.BZZT_IS_SERVICE_RUNNING";
	public static final String BZZT_CAMERA_POSITION_ZOOM = "uco.edu.jstone25.bzzt.MainActivity.BZZT_CAMERA_POSITION_ZOOM";
	public static final String HTTP_STATUS = "uco.edu.jstone25.bzzt.MainActivity.BZZT_HTTP_STATUS";
	public static final String HTTP_STATUS_CODE = "uco.edu.jstone25.bzzt.MainActivity.BZZT_HTTP_STATUS_CODE";
	
	public static final int BZZT_SERVICE_STATUS_NOTIFICATION_ID = 0;
	
	private DataUpdateReceiver dataUpdateReceiver;
	
	private class QueryPoints extends AsyncTask<Double, Void, HttpResponse> {
		
		private QueryPointsListener<AccelerationPoint> listener;
		
		public QueryPoints(QueryPointsListener<AccelerationPoint> l) {
			listener = l;
		}
		
		@Override
		protected HttpResponse doInBackground(Double... params) {
			URI uri;
			try {
				uri = new URIBuilder()
					.setScheme("http")
					.setHost("www.bzzt-app.com")
					.setPath("/submit")
					.setParameter("x0", Double.toString(params[0]))
					.setParameter("y0", Double.toString(params[1]))
					.setParameter("x1", Double.toString(params[2]))
					.setParameter("y1", Double.toString(params[3]))
					.build();
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
				return null;
			}
			HttpResponse hr = null;
			HttpClient hc = new DefaultHttpClient();
			HttpGet hg = new HttpGet(uri);

			try {
                hr = hc.execute(hg);
			} catch (ClientProtocolException e) {
                Log.d("bzzt CPE", e.toString());
                e.printStackTrace();
			} catch (IOException e) {
                Log.d("bzzt IOE", e.toString());
                e.printStackTrace();
			}
            return hr;
		}
		
		@Override
		protected void onPostExecute(HttpResponse hr) {
			/* hr.getStatusLine(), hr.getStatusLine().getStatusCode()) */
			// send payload to quad-tree populating function which then draws them
			if (hr.getEntity().getContentLength() > 0) {
				try {
					PointQuadTree<AccelerationPoint> apqt = new PointQuadTree<AccelerationPoint>(-90f, -180f, 90f, 180f);
					BufferedReader br = new BufferedReader(new InputStreamReader(hr.getEntity().getContent()));
					String line;
					HashMap<Integer, TreeMap<Integer, AccelerationPoint>> h = new HashMap<Integer, TreeMap<Integer, AccelerationPoint>>();
					for (line = br.readLine(); line != null; line = br.readLine()) {
						TreeMap<Integer, AccelerationPoint> sequence;
						AccelerationPoint ap = new AccelerationPoint(line);
						if (!h.containsKey(ap.getSeries())) {
							sequence = new TreeMap<Integer, AccelerationPoint>();
							h.put(ap.getSeries(), sequence);
						} else {
							sequence = h.get(ap.getSeries());
						}
						sequence.put(ap.getSequence(), ap);
						apqt.add(ap);
					}
					
					listener.setPointTree(apqt);
					listener.setSeriesMap(h);
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private PointQuadTree<AccelerationPoint> pqt;
	private HashMap<Integer, TreeMap<Integer, AccelerationPoint>> series;
	
	private GoogleMap map;
	private Location location;
	
	// private static Class<?> myServiceClass = edu.uco.jstone25.bzzt.TestDataCollectionService.class;
	private static Class<?> myServiceClass = edu.uco.jstone25.bzzt.DataCollectionService.class;

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
		}

		Log.d("MainActivity", "onResume");
	}
	
	private void registerForLocationUpdates() {
		if (dataUpdateReceiver == null) {
			dataUpdateReceiver = new DataUpdateReceiver();
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(HTTP_STATUS);
			intentFilter.addAction(UPDATE_CURRENT_LOCATION);
			LocalBroadcastManager.getInstance(this).registerReceiver(dataUpdateReceiver, intentFilter);
			Log.d("Bzzt", "Registered receiver: " + dataUpdateReceiver);
		} else {
			Log.d("Bzzt", "Refusing to register over non-null dataUpdateReceiver");
		}
	}
	
	private void unregisterFromLocationUpdates() {
		if (dataUpdateReceiver != null) {
			Log.d("Bzzt", "Unregistering receiver: " + dataUpdateReceiver.toString());
			// stopService(new Intent(this, myServiceClass));
			try {
				unregisterReceiver(dataUpdateReceiver);
				Log.d("Bzzt", "Successfully unregistered receiver.");
			} catch(Exception e) {
				Log.d("Bzzt", "Exception unregistering receiver");
			}
			dataUpdateReceiver = null;
		}
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
	
	private void launchBrowseModeFragment(LatLng loc) {
        // browse mode, launch action fragment for capture/playback or view/add hazard
        BrowseModeDialogFragment bmdf = new BrowseModeDialogFragment();
        Bundle browseModeFragmentArguments = new Bundle();
        browseModeFragmentArguments.putBoolean(BZZT_IS_SERVICE_RUNNING, isMyServiceRunning(myServiceClass));
        browseModeFragmentArguments.putParcelable(BROWSE_LONGTOUCH_LOCATION, loc);
        bmdf.setArguments(browseModeFragmentArguments);
        bmdf.show(getFragmentManager(), "lol");
    }
	
	private void launchInsertObstacleFragment(LatLng loc) {
        InsertNoticeDialogFragment indf = new InsertNoticeDialogFragment();
        Bundle insertNoticeFragmentArguments = new Bundle();
        insertNoticeFragmentArguments.putParcelable(BROWSE_LONGTOUCH_LOCATION, loc);
        indf.setArguments(insertNoticeFragmentArguments);
        indf.show(getFragmentManager(), "lol");
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
				launchBrowseModeFragment(loc);
			}
		});
		
		dataUpdateReceiver = null;
		
		onNewIntent(getIntent());
		
		if (isMyServiceRunning(myServiceClass)) {
			registerForLocationUpdates();
		} else {
			setCurrentMapLocation(17);
			queryLocalPoints();
		}
	}
	
	private void queryLocalPoints() {
		double x0, y0, x1, y1;
		x0 = map.getProjection().getVisibleRegion().latLngBounds.southwest.latitude;
		y0 = map.getProjection().getVisibleRegion().latLngBounds.southwest.longitude;
		x1 = map.getProjection().getVisibleRegion().latLngBounds.northeast.latitude;
		y1 = map.getProjection().getVisibleRegion().latLngBounds.northeast.longitude;
		new QueryPoints(this).execute(x0, y0, x1, y1);
		// calls thing that draws points when finished
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
            registerForLocationUpdates();
		} else {
			Toast.makeText(getApplicationContext(), "Failed to start data capture service.", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void stopDataListenerService() {
		// send an intent for the service to upload data then stop itself
		if(startService(new Intent(this, myServiceClass).setAction(STOP_SERVICE)) != null) {
			Toast.makeText(getApplicationContext(), "Stopping data capture service...", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), "Failed to stop data capture service.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onBrowseMenuSelect(int position, LatLng location, DialogFragment df) {
		switch(position) {
		case 0:
			if (isMyServiceRunning(myServiceClass)) {
                stopDataListenerService();
			} else {
                startDataListenerService();
			}
			break;

		case 1:
			// Toast.makeText(getApplicationContext(), "warm fuzzies", Toast.LENGTH_SHORT).show();
			// toss up a fragment for dis shit
			// Toast.makeText(getApplicationContext(), "lol", Toast.LENGTH_SHORT).show();
			launchInsertObstacleFragment(location);
			break;

		default:
		}
	}
	
	private void updateCurrentLocation(Location new_location, float zoom) {
		// compute speed with lat/long
		Log.d("MainActivity", "Location updated to " + new_location.getLatitude() + "," + new_location.getLongitude());
		Log.d("MainActivity", new_location.toString());
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
            } else if(intent.getAction().equals(HTTP_STATUS)) {
            	if (intent.hasExtra(HTTP_STATUS_CODE) && (intent.getIntExtra(HTTP_STATUS_CODE, 999) >= 200 && intent.getIntExtra(HTTP_STATUS_CODE,  999) < 400)) {
            		Toast.makeText(getApplicationContext(), "Upload successful", Toast.LENGTH_SHORT).show();
            	} else {
            		Toast.makeText(getApplicationContext(), "Upload failed: " + intent.getStringExtra(HTTP_STATUS_CODE), Toast.LENGTH_LONG).show();
            	}
            	// the service should have stopped itself by now so go ahead and stop listening
           		unregisterFromLocationUpdates();
            }
		}
	
		public DataUpdateReceiver() {
			Log.d("DUR", "constructor" + this.toString());
		}
	}

	@Override
	public void onInsertNoticeInsert(LatLng loc, String title, String message,
			DialogFragment df) {
		if (loc != null) {
			// Toast.makeText(getApplicationContext(), "Adding notice at " + loc, Toast.LENGTH_SHORT).show();
			map.addCircle(new CircleOptions().center(loc).radius(5).fillColor(Color.RED).strokeColor(Color.BLACK));
		}
	}
	
	void renderSeries(HashMap<Integer, TreeMap<Integer, AccelerationPoint>> h) {
		AccelerationPoint a0 = null;
		for (TreeMap<Integer, AccelerationPoint> sequence : h.values()) {
			for (int i : sequence.keySet()) { // TreeMap presents ascending-sorted keyset
				AccelerationPoint a = sequence.get(i);

				if (a0 != null) {
					map.addPolyline((new PolylineOptions()).add(a0.getLatLng(), a.getLatLng()).width(5).color(Color.BLUE));
				}

				a0 = a;
			}
		}
	}

	@Override
	public void setPointTree(PointQuadTree<AccelerationPoint> p) {
		pqt = p;
	}

	@Override
	public void setSeriesMap(
			HashMap<Integer, TreeMap<Integer, AccelerationPoint>> h) {
		series = h;
		renderSeries(series);
	}
}
