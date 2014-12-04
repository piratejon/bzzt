package edu.uco.jstone25.bzzt;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class BrowseModeDialogFragment extends DialogFragment {
	
	private LatLng location;
	
	public interface BrowseModeListener {
		public void onBrowseMenuSelect(int position, LatLng location, DialogFragment df);
	}
	
	BrowseModeListener listener;
	
	private boolean isServiceRunning;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		isServiceRunning = getArguments().getBoolean(MainActivity.BZZT_IS_SERVICE_RUNNING);
		location = getArguments().getParcelable(MainActivity.BROWSE_LONGTOUCH_LOCATION);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			listener = (BrowseModeListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + "must implement PickDepartmentListener");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
			.setTitle(R.string.browse_dialog_title)
			.setItems(
					isServiceRunning ? R.array.browse_dialog_stop_items : R.array.browse_dialog_start_items,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					listener.onBrowseMenuSelect(which, location, BrowseModeDialogFragment.this);
				}
			});
		return builder.create();
	}
}
