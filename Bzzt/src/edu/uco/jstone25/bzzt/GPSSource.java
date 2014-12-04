package edu.uco.jstone25.bzzt;

import java.io.IOException;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GPSSource implements LocationListener {

	private LocationManager mLocationManager;
	private String provider;
	private LogFile lf;
	private DataCollectionService dataCollectionService;

	public GPSSource(Context context, DataCollectionService dcs) throws IOException {
		lf = new LogFile("bzzt-gps-");
		dataCollectionService = dcs;
		mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		provider = mLocationManager.getBestProvider(crit, true);
		mLocationManager.getLastKnownLocation(provider);
	}
	
	@Override
	public void onLocationChanged(Location location) {
		try {
			lf.appendFile(location.getLatitude() + "," + location.getLongitude() + "," + location.getElapsedRealtimeNanos());
		} catch (IOException e) {
		}
		
		Log.d("GPSSource", "Location Updated");
		dataCollectionService.updateCurrentLocation(location);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	public void start() {
		mLocationManager.requestLocationUpdates(provider, 400, 1, GPSSource.this);
	}
	
	public void stop() {
		mLocationManager.removeUpdates(GPSSource.this);
		lf.close();
	}
	
	public String getLogFilePath() {
		return lf.getCanonicalPath();
	}
}
