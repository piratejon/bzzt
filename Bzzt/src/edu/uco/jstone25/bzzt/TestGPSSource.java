package edu.uco.jstone25.bzzt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class TestGPSSource {

	private TestDataCollectionService dataCollectionService;
	private static ArrayList<SpaceTimeCoordinate> locations;
	long nanosecondOffset;
	int lastIndex;
	Handler timerHandler;
	Runnable timerRunnable;
	
	private class SpaceTimeCoordinate {
		private long nanoseconds;
		private double latitude, longitude;
		
		SpaceTimeCoordinate(long ns, double lati, double longi) {
			setNanoseconds(ns);
			setLatitude(lati);
			setLongitude(longi);
		}

		public SpaceTimeCoordinate(String line) {
			// 54574126601618:35.44271268,-97.59743031
			String[] bits = line.split("[:,]");
			setNanoseconds(Long.parseLong(bits[0], 10) - nanosecondOffset);
			setLatitude(Double.parseDouble(bits[1]));
			setLongitude(Double.parseDouble(bits[2]));
		}

		public long getNanoseconds() {
			return nanoseconds;
		}

		public void setNanoseconds(long nanoseconds) {
			this.nanoseconds = nanoseconds;
		}

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}
	}
	
	ArrayList<SpaceTimeCoordinate> populateCoords(Context ctx, String fileName) {
		ArrayList<SpaceTimeCoordinate> coords = new ArrayList<SpaceTimeCoordinate>();
		BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.gpsacc)));
		try {
			nanosecondOffset = Long.parseLong(br.readLine().split(":",0)[0],10) - SystemClock.elapsedRealtimeNanos();
			for(String line; (line = br.readLine()) != null; ) {
				coords.add(new SpaceTimeCoordinate(line));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return coords;
	}
	
	public TestGPSSource(Context context, TestDataCollectionService testDataCollectionService) throws IOException {
		dataCollectionService = testDataCollectionService;
		locations = populateCoords(context, "gps.txt");
		
		timerHandler = new Handler();
		timerRunnable = new Runnable() {
			@Override
			public void run() {
				Location l = new Location("");
				long currentNanos = SystemClock.elapsedRealtimeNanos();
				int i;
				// Log.d("GPS SVC", "i0: " + lastIndex + "; nanos: " + locations.get(lastIndex).getNanoseconds());
				for ( i = lastIndex; i < locations.size() && locations.get(i).getNanoseconds() < currentNanos; i += 1);
				// Log.d("GPS SVC", "i1:" + lastIndex + "; nanos: " + locations.get(i).getNanoseconds());

				if (i < locations.size()) {
					lastIndex = i;
				} else {
					lastIndex = locations.size()-1;
				}

				l.setLatitude(locations.get(lastIndex).getLatitude());
				l.setLongitude(locations.get(lastIndex).getLongitude());

				onLocationChanged(l);
				
				timerHandler.postDelayed(this, 250);
			}
		};
	}
	
	public void onLocationChanged(Location location) {
		// get next location thing
		Log.d("GPSSource", "Location Updated");
		dataCollectionService.updateCurrentLocation(location);
	}

	public void start() {
		// mLocationManager.requestLocationUpdates(provider, 400, 1, this);
		timerHandler.postDelayed(timerRunnable, 500);
	}
	
	public void stop() {
		// mLocationManager.removeUpdates(this);
		timerHandler.removeCallbacks(timerRunnable);
	}
}
