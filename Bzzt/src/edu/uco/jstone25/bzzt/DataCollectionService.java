package edu.uco.jstone25.bzzt;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class DataCollectionService extends Service {
	
	private static String UPLOAD_URL = "http://bzzt-app.com/submit/";
	
	private Notification.Builder nbServiceStatus;
	private Location oldLocation;
	private float metersTraveled, avgSpeed, curSpeed;
	long t0, duration;
	
	private DecimalFormat df;
	
	AccelerometerSource as;
	GPSSource gs;
	
	String asf, gsf;
	
	private class UploadFile extends AsyncTask<Context, Void, HttpResponse> {
		private Context ctx;
		
		@Override
		protected HttpResponse doInBackground(Context... params) {
			ctx = params[0];
			HttpResponse hr = null;
			HttpClient hc = new DefaultHttpClient();
			HttpPost hp = new HttpPost(UPLOAD_URL);
			MultipartEntityBuilder mpe = MultipartEntityBuilder.create();
			mpe.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                // .setBoundary("--------------------" + (System.nanoTime() + System.currentTimeMillis()))
                .addPart("accel", new FileBody(new File(asf)))
                .addPart("gps", new FileBody(new File(gsf)));
			hp.setEntity(mpe.build());
			try {
                hr = hc.execute(hp);
			} catch (ClientProtocolException e) {
                Log.d("DCV SVC CPE", e.toString());
                e.printStackTrace();
			} catch (IOException e) {
                Log.d("DCV SVC IOE", e.toString());
                e.printStackTrace();
			}
            return hr;
		}
		
		@Override
		protected void onPostExecute(HttpResponse hr) {
			Intent i = new Intent(MainActivity.HTTP_STATUS);
			if (hr != null) {
				i.putExtra(MainActivity.HTTP_STATUS, hr.getStatusLine().toString());
				i.putExtra(MainActivity.HTTP_STATUS_CODE, hr.getStatusLine().getStatusCode());
			}
			LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
		}
	}
	
	private void uploadAndBail() {
		new UploadFile().execute(this);
		stopSelf();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("DCS SVC", "onStartCommand:" + ((intent == null) ? "null intent" : intent.toString()) + ";" + flags + ";" + startId + ";" + this + ";as=" + as + ";gs=" + gs);
		if (intent.getAction() == MainActivity.STOP_SERVICE) {
			uploadAndBail();
		}
		return START_STICKY;
	}

	private void setupNotification() {
		nbServiceStatus = new Notification.Builder(this)
			.setTicker("Bzzt capture service started.")
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("Bzzt! Capturing In Progress")
			.setContentText("Ride beginning")
			.setOngoing(true)
			.setUsesChronometer(true)
			.setAutoCancel(false)
			// .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.uco))
			.setContentIntent(
				PendingIntent.getActivity(
					this, 0,
					new Intent(this, MainActivity.class)
						.putExtra(MainActivity.BZZT_CAMERA_POSITION_ZOOM, 17f)
					, PendingIntent.FLAG_UPDATE_CURRENT));

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(
			MainActivity.BZZT_SERVICE_STATUS_NOTIFICATION_ID, nbServiceStatus.build());
	}
	
	private void concludeNotification() {
		// cancel the first one
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MainActivity.BZZT_SERVICE_STATUS_NOTIFICATION_ID);

		Notification.Builder nbServiceStatus = new Notification.Builder(getApplicationContext())
			.setTicker("Bzzt capture service stopped.")
			.setSmallIcon(R.drawable.ic_launcher)
			.setAutoCancel(false)
			.setContentIntent(null);

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(
			MainActivity.BZZT_SERVICE_STATUS_NOTIFICATION_ID, nbServiceStatus.build());

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MainActivity.BZZT_SERVICE_STATUS_NOTIFICATION_ID);
	}

	@Override
	public void onCreate() {
		Log.d("DCS SVC", "onCreate");
		
		df = new DecimalFormat();
		df.setMaximumFractionDigits(2);

		try {
			as = new AccelerometerSource(this.getApplicationContext());
			gs = new GPSSource(this.getApplicationContext(), this);
			setupNotification();
		} catch (IOException e) {
			// don't start the sensors if the constructors couldn't open the output files
			Log.e("DCS SVC", "failed to start sensors");
			Toast.makeText(getApplicationContext(), "Unable to start service" + e.toString(), Toast.LENGTH_LONG).show();
			stopSelf();
			return;
		}
		
		as.start();
		gs.start();
		
		asf = as.getLogFilePath();
		gsf = gs.getLogFilePath();
	}
	
	@Override
	public void onDestroy() {
		Log.i("DCS SVC", "onDestroy");

		concludeNotification();
		if (as != null) as.stop();
		if (gs != null) gs.stop();
		super.onDestroy();
	}
	
	private void updateStatus() {
		nbServiceStatus.setContentText("Distance: " + df.format(metersTraveled) + "m; " + df.format(avgSpeed) + " m/s avg; " + df.format(curSpeed) + "m/s cur.");
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
			.notify(MainActivity.BZZT_SERVICE_STATUS_NOTIFICATION_ID, nbServiceStatus.build());
	}
	
	private void updateDistance(Location l) {
		if (oldLocation != null) {
			float newDistance = oldLocation.distanceTo(l);
			long new_duration = SystemClock.elapsedRealtime() - t0;
			duration += new_duration;
			metersTraveled += newDistance;
			avgSpeed = metersTraveled / (duration/1000);
			curSpeed = newDistance / (new_duration/1000);
		}
		oldLocation = l;
		t0 = SystemClock.elapsedRealtime();
	}

	public void updateCurrentLocation(Location location) {
		Log.d("DCS SVC", "Broadcasting location");
		Intent i = new Intent(MainActivity.UPDATE_CURRENT_LOCATION);
		i.putExtra("location", location);
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
		
		updateDistance(location);
		updateStatus();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("DCS SVC", "onBind");
		return null;
	}
}
