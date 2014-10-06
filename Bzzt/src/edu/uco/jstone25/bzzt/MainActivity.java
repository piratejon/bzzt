package edu.uco.jstone25.bzzt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		((ToggleButton)findViewById(R.id.toggleButtonRecording)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					startService(new Intent(MainActivity.this, DataCollectionService.class));
                } else {
                	if(stopService(new Intent(MainActivity.this, DataCollectionService.class))) {
                		Toast.makeText(getApplicationContext(), "Stopped service.", Toast.LENGTH_SHORT).show();
                	} else {
                		Toast.makeText(getApplicationContext(), "Failed to stop service.", Toast.LENGTH_SHORT).show();
                	}
				}
			}
		});
	}
}
