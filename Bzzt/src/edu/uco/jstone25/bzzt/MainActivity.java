package edu.uco.jstone25.bzzt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.quadtree.PointQuadTree;

import edu.uco.jstone25.bzzt.BrowseModeDialogFragment.BrowseModeListener;
import edu.uco.jstone25.bzzt.InsertNoticeDialogFragment.InsertNoticeListener;
import edu.uco.jstone25.bzzt.Notice.QueryNoticesListener;

public class MainActivity extends Activity implements BrowseModeListener, InsertNoticeListener, QueryPointsListener<AccelerationPoint>, QueryNoticesListener {
	
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

	private class QueryPoints extends AsyncTask<Double, Void, HashMap<Integer, TreeMap<Integer, AccelerationPoint>>> {
		
		private QueryPointsListener<AccelerationPoint> listener;
		
		public QueryPoints(QueryPointsListener<AccelerationPoint> l) {
			listener = l;
		}
		
		@Override
		protected HashMap<Integer, TreeMap<Integer, AccelerationPoint>> doInBackground(Double... params) {

			HashMap<Integer, TreeMap<Integer, AccelerationPoint>> h = new HashMap<Integer, TreeMap<Integer, AccelerationPoint>>();
			try {
				URL url = new URL("http://bzzt-app.com/submit?want=points&x0="
							+ URLEncoder.encode(Double.toString(params[0]), "UTF-8") + "&y0="
							+ URLEncoder.encode(Double.toString(params[1]), "UTF-8") + "&x1="
							+ URLEncoder.encode(Double.toString(params[2]), "UTF-8") + "&y1="
							+ URLEncoder.encode(Double.toString(params[3]), "UTF-8"));
				BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

				String line;
				try {
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
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return h;
		}

		@Override
		protected void onPostExecute(HashMap<Integer, TreeMap<Integer, AccelerationPoint>> hm) {
			/* hr.getStatusLine(), hr.getStatusLine().getStatusCode()) */
			listener.setSeriesMap(hm);
		}
	}
	
	private class QueryNotices extends AsyncTask<Double, Void, List<Notice>> {
		
		private QueryNoticesListener listener;
		
		public QueryNotices(QueryNoticesListener l) {
			listener = l;
		}
		
		@Override
		protected List<Notice> doInBackground(Double... params) {
			List<Notice> nl = new ArrayList<Notice>();

			try {
				URL url = new URL("http://bzzt-app.com/submit?want=notices&x0="
							+ URLEncoder.encode(Double.toString(params[0]), "UTF-8") + "&y0="
							+ URLEncoder.encode(Double.toString(params[1]), "UTF-8") + "&x1="
							+ URLEncoder.encode(Double.toString(params[2]), "UTF-8") + "&y1="
							+ URLEncoder.encode(Double.toString(params[3]), "UTF-8"));
				BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

				String line;
				try {
					for (line = br.readLine(); line != null; line = br.readLine()) {
						nl.add(new Notice(line));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return nl;
		}

		@Override
		protected void onPostExecute(List<Notice> nl) {
			listener.setNoticeList(nl);
		}
	}

	private PointQuadTree<AccelerationPoint> pqt;
	private HashMap<Integer, TreeMap<Integer, AccelerationPoint>> series;
	private List<Polyline> lines;
	private List<Notice> notices;
	
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
			map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
				@Override
				public void onMapLoaded() {
					queryLocalPoints();
					queryLocalNotices();
				}
			});
			
			map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
				@Override
				public void onCameraChange(CameraPosition arg0) {
					if (!isMyServiceRunning(myServiceClass)) {
						queryLocalPoints();
						queryLocalNotices();
					}
				}
			});
			
			map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
				
				@Override
				public void onMapClick(LatLng arg0) {
					// did we touch a circle?
					for(Notice n : notices) {
						if (n.getCircle() != null) {
							float results[] = new float[3];
							Location.distanceBetween(n.getLocation().latitude, n.getLocation().longitude, arg0.latitude, arg0.longitude, results);
							if (results[0] <= n.getCircle().getRadius()) {
								Toast.makeText(getApplicationContext(), n.getMessage(), Toast.LENGTH_LONG).show();
							}
						}
					}
				}
			});
		}
		
		lines = new ArrayList<Polyline>();
		notices = new ArrayList<Notice>();
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
	
	private void queryLocalNotices() {
		double x0, y0, x1, y1;
		x0 = map.getProjection().getVisibleRegion().latLngBounds.southwest.latitude;
		y0 = map.getProjection().getVisibleRegion().latLngBounds.southwest.longitude;
		x1 = map.getProjection().getVisibleRegion().latLngBounds.northeast.latitude;
		y1 = map.getProjection().getVisibleRegion().latLngBounds.northeast.longitude;
		new QueryNotices(this).execute(x0, y0, x1, y1);
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
	
	private void uploadMessage(LatLng loc, String message) {
		class UploadFile extends AsyncTask<String, Void, HttpResponse> {
		
			@Override
			protected HttpResponse doInBackground(String... params) {
				HttpResponse hr = null;
				HttpClient hc = new DefaultHttpClient();
				HttpPost hp = new HttpPost("http://bzzt-app.com/submit");
				MultipartEntityBuilder mpe = MultipartEntityBuilder.create();
				mpe.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
				    .addTextBody("message", params[0])
				    .addTextBody("latitude", params[1])
				    .addTextBody("longitude", params[2]);
				hp.setEntity(mpe.build());

				try {
	                hr = hc.execute(hp);
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
				int http_status_code = hr.getStatusLine().getStatusCode();

            	if (http_status_code >= 200 && http_status_code < 400) {
            		Toast.makeText(getApplicationContext(), "Notice upload successful", Toast.LENGTH_SHORT).show();
            	} else {
            		Toast.makeText(getApplicationContext(), "Upload failed: " + hr.getStatusLine(), Toast.LENGTH_LONG).show();
            	}
			}
		}
		
		try {
			new UploadFile().execute(
					URLEncoder.encode(message, "UTF-8"),
					URLEncoder.encode(Double.toString(loc.latitude), "UTF-8"),
					URLEncoder.encode(Double.toString(loc.longitude), "UTF-8")
					);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onInsertNoticeInsert(LatLng loc, String message, DialogFragment df) {
		if (loc != null) {
			// Toast.makeText(getApplicationContext(), "Adding notice at " + loc, Toast.LENGTH_SHORT).show();
			map.addCircle(new CircleOptions().center(loc).radius(5).fillColor(Color.RED).strokeColor(Color.BLACK));
			uploadMessage(loc, message);
		}
	}
	
	void removeOldLines(List<Polyline> ls) {
		for(Polyline p : ls) {
			p.remove();
		}
	}
	
	void renderSeries(HashMap<Integer, TreeMap<Integer, AccelerationPoint>> h) {
		Log.d("bzzt", "renderSeries");
		AccelerationPoint a0 = null, a1;
		removeOldLines(lines);
		for (TreeMap<Integer, AccelerationPoint> sequence : h.values()) {
			for (int i : sequence.keySet()) { // TreeMap presents ascending-sorted keyset
				a1 = sequence.get(i);

				if (a0 != null && a0.getSequence() == a1.getSequence() - 1) {
					PolylineOptions po = new PolylineOptions();
					po.add(a0.getLatLng(), a1.getLatLng()).width(5);
					
					if (Math.abs(a1.getAccel()) <= 1.0) {
						po.color(Color.GREEN);
					} else if (Math.abs(a1.getAccel()) <= 2.0) {
						po.color(Color.YELLOW);
					} else {
						po.color(Color.RED);
					}

					lines.add(map.addPolyline(po));
				}

				a0 = a1;
			}
		}
	}

	@Override
	public void setPointTree(PointQuadTree<AccelerationPoint> p) {
		Log.d("bzzt", "setPointTree");
		pqt = p;
	}

	@Override
	public void setSeriesMap(
			HashMap<Integer, TreeMap<Integer, AccelerationPoint>> h) {
		Log.d("bzzt", "setSeriesMap");
		series = h;
		renderSeries(series);
	}
	
	private void removeOldNotices(List<Notice> l) {
		for(Notice n : l) {
			if (n.getCircle() != null) n.getCircle().remove();
		}
	}
	
	private void renderNotices(List<Notice> l) {
		removeOldNotices(notices);
		for(Notice n : l) {
			n.setCircle(map.addCircle(new CircleOptions().center(n.getLocation()).radius(5).fillColor(Color.RED).strokeColor(Color.BLACK)));
		}
		notices = l;
	}

	@Override
	public void setNoticeList(List<Notice> l) {
		renderNotices(l);
	}
}
