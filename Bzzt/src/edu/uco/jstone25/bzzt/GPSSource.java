package edu.uco.jstone25.bzzt;

import java.io.IOException;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSSource implements LocationListener {

	private LocationManager mLocationManager;
	private String provider;
	private LogFile lf;

	public GPSSource(Context context) throws IOException {
		lf = new LogFile("bzzt-gps-");
		mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		provider = mLocationManager.getBestProvider(crit, true);
		mLocationManager.getLastKnownLocation(provider);
	}

	@Override
	public void onLocationChanged(Location location) {
		try {
			lf.appendFile(location.getLatitude() + "," + location.getLongitude());
		} catch (IOException e) {
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void start() {
		mLocationManager.requestLocationUpdates(provider, 400, 1, this);
	}
	
	public void stop() {
		mLocationManager.removeUpdates(this);
		lf.close();
	}
}
