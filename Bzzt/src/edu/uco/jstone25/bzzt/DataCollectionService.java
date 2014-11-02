package edu.uco.jstone25.bzzt;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class DataCollectionService extends Service {
	
	AccelerometerSource as;
	GPSSource gs;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated constructor stub
		Log.d("DCS SVC", "onCreate");
		try {
			as = new AccelerometerSource(this.getApplicationContext());
			gs = new GPSSource(this.getApplicationContext(), this);
		} catch (IOException e) {
			// don't start the sensors if the constructors couldn't open the output files
			Toast.makeText(getApplicationContext(), "Unable to start service" + e.toString(), Toast.LENGTH_LONG).show();
			stopSelf();
			return;
		}
		
		as.start();
		gs.start();

		Toast.makeText(getApplicationContext(), "Seems like it may have?", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onDestroy() {
		Log.d("DCS SVC", "onDestroy");
		as.stop();
		gs.stop();
		super.onDestroy();
	}

	public void updateCurrentLocation(Location location) {
		Intent i = new Intent(MainActivity.UPDATE_CURRENT_LOCATION);
		i.putExtra("location", location);
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("DCS SVC", "onBind");
		return null;
	}
}
