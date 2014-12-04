package edu.uco.jstone25.bzzt;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class TestDataCollectionService extends DataCollectionService {
	
	TestAccelerometerSource as;
	TestGPSSource gs;
	
	@Override
	public void onCreate() {
		Log.d("DCS SVC", "onCreate");

		try {
			as = new TestAccelerometerSource(this.getApplicationContext());
			gs = new TestGPSSource(this.getApplicationContext(), this);
		} catch (IOException e) {
			// don't start the sensors if the constructors couldn't open the output files
			Log.e("DCS SVC", "failed to start sensors");
			Toast.makeText(getApplicationContext(), "Unable to start service" + e.toString(), Toast.LENGTH_LONG).show();
			stopSelf();
			return;
		}
		
		as.start();
		gs.start();
	}
	
	@Override
	public void onDestroy() {
		Log.i("DCS SVC", "onDestroy");
		as.stop();
		gs.stop();
		super.onDestroy();
	}

	public void updateCurrentLocation(Location location) {
		Log.i("DCS SVC", "Broadcasting location: " + location.toString());
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
