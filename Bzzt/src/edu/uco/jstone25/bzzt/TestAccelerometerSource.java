package edu.uco.jstone25.bzzt;

import java.io.IOException;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class TestAccelerometerSource implements SensorEventListener {

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private LogFile lf;
	
	private void updateAccelerometerValues(float x, float y, float z) {
		try {
			lf.appendFile(x + "," + y + "," + z);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		updateAccelerometerValues(event.values[0], event.values[1], event.values[2]);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public TestAccelerometerSource(Context context) throws IOException {
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		lf = new LogFile("bzzt-accelerometer-");
	}
	
	public void start() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	}
	
	public void stop() {
		mSensorManager.unregisterListener(this, mAccelerometer);
		lf.close();
	}
}
