package edu.uco.jstone25.bzzt;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class DataCollectionService extends Service {
	
	AccelerometerSource as;
	GPSSource gs;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		try {
			as = new AccelerometerSource(this.getApplicationContext());
			gs = new GPSSource(this.getApplicationContext());
		} catch (IOException e) {
			// don't start the sensors if the constructors couldn't open the output files
			Toast.makeText(getApplicationContext(), "Unable to start service" + e.toString(), Toast.LENGTH_LONG).show();
			stopSelf();
			return START_NOT_STICKY;
		}
		
		as.start();
		gs.start();

		Toast.makeText(getApplicationContext(), "Seems like it started?", Toast.LENGTH_SHORT).show();

		return START_NOT_STICKY; // don't automatically restart if killed
	}
	
	@Override
	public void onDestroy() {
		as.stop();
		gs.stop();
		super.onDestroy();
	}
}
