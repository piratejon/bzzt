package edu.uco.jstone25.bzzt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

// import com.noneuclideantriangles.bzzt.R;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class MainActivity extends Activity implements SensorEventListener, LocationListener {
	
	private String provider;
	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private TextView accelX, accelY, accelZ, gpsLat, gpsLong;
	private long startNanos;
	private PrintWriter pwAccelerometer, pwGPS;
	
	private void initializeHandles() {
		accelX = (TextView) findViewById(R.id.textViewXValue);
		accelY = (TextView) findViewById(R.id.textViewYValue);
		accelZ = (TextView) findViewById(R.id.textViewZValue);
		
		gpsLat = (TextView) findViewById(R.id.textViewLatValue);
		gpsLong = (TextView) findViewById(R.id.textViewLongValue);
	}

	private void initializeSensors() {
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		// from:
		// <http://www.vogella.com/tutorials/AndroidLocationAPI/article.html#locationapi>
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		provider = mLocationManager.getBestProvider(crit, true);
		Location location = mLocationManager.getLastKnownLocation(provider);
		if (location != null) {
			Toast.makeText(getApplicationContext(), "GPS acquired", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), "Cannot into GPS", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void disableAccelerometer() {
		mSensorManager.unregisterListener(this, mAccelerometer);
		pwAccelerometer.flush();
		pwAccelerometer.close();
	}
	
	private void enableAccelerometer() throws IOException {
        pwAccelerometer = new PrintWriter(openOutputFile("bzzt-accelerometer-"));
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	private void enableGPS() throws IOException {
		pwGPS = new PrintWriter(openOutputFile("bzzt-gps-"));
		mLocationManager.requestLocationUpdates(provider, 400, 1, this);
	}
	
	private void disableGPS() {
		mLocationManager.removeUpdates(this);
		pwGPS.flush();
		pwGPS.close();
	}
	
	private void appendFile(PrintWriter fos, String s) throws IOException {
		fos.print(s);
	}
	
	private PrintWriter openOutputFile(String prefix) throws IOException {
		PrintWriter fos;
		File f = new File(Environment.getExternalStoragePublicDirectory(DOWNLOAD_SERVICE), prefix + System.currentTimeMillis() + ".txt");
		Toast.makeText(getApplicationContext(), "File path:" + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
		fos = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f))));
		appendFile(fos, "Start stamp (ns): " + startNanos + "\n");
		return fos;
	}
	
	private void updateAccelerometerValues(float x, float y, float z) {
		accelX.setText(Float.toString(x));
		accelY.setText(Float.toString(y));
		accelZ.setText(Float.toString(z));
		try {
			appendFile(pwAccelerometer, SystemClock.elapsedRealtimeNanos() + "," + x + "," + y + "," + z + "\n");
		} catch (IOException e) {
			disableAccelerometer();
			Toast.makeText(getApplicationContext(), "''" + e.toString() + "'' while writing accelerometer file.", Toast.LENGTH_LONG).show();
		}
	}
	
	private void updateGPSValues(double latitude, double longitude) {
		gpsLat.setText("" + latitude);
		gpsLong.setText("" + longitude);
		try {
			appendFile(pwGPS, SystemClock.elapsedRealtimeNanos() + "," + latitude + "," + longitude + "\n");
		} catch (IOException e) {
			disableGPS();
			Toast.makeText(getApplicationContext(), "''" + e.toString() + "'' while writing GPS file.", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initializeHandles();
		initializeSensors();
		
		((ToggleButton)findViewById(R.id.toggleButtonRecording)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					startNanos = SystemClock.elapsedRealtimeNanos();
                    try {
                        enableAccelerometer();
                    } catch (IOException e) {
                    	Toast.makeText(getApplicationContext(), "Exception enabling accelerometer: " + e.toString(), Toast.LENGTH_LONG).show();
                    	((ToggleButton)findViewById(R.id.toggleButtonRecording)).setChecked(false);
                    }
                    
                    try {
						enableGPS();
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), "Exception enabling GPS: " + e.toString(), Toast.LENGTH_LONG).show();
                    	((ToggleButton)findViewById(R.id.toggleButtonRecording)).setChecked(false);
					}
                } else {
					disableAccelerometer();
					disableGPS();
				}
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(((ToggleButton)findViewById(R.id.toggleButtonRecording)).isChecked()) {
			disableAccelerometer();
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		updateAccelerometerValues(event.values[0], event.values[1], event.values[2]);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLocationChanged(Location location) {
		updateGPSValues(location.getLatitude(), location.getLongitude());
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
}
