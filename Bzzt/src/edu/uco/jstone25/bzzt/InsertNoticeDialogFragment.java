package edu.uco.jstone25.bzzt;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

public class InsertNoticeDialogFragment extends DialogFragment {
	
	private EditText et;
	private LatLng location;
	
	public interface InsertNoticeListener {
		public void onInsertNoticeInsert(LatLng loc, String message, DialogFragment df);
	}
	
	InsertNoticeListener listener;
	
	private boolean isServiceRunning;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		isServiceRunning = getArguments().getBoolean(MainActivity.BZZT_IS_SERVICE_RUNNING);
		location = getArguments().getParcelable(MainActivity.BROWSE_LONGTOUCH_LOCATION);
		et = new EditText(getActivity());
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			listener = (InsertNoticeListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + "must implement PickDepartmentListener");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
			.setTitle(R.string.browse_dialog_title)
			.setMessage("Add alert at " + location.toString() + "?")
			.setView(et)
			.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					listener.onInsertNoticeInsert(location,  et.getText().toString(), InsertNoticeDialogFragment.this);
				}
			})
			.setNegativeButton("no", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					listener.onInsertNoticeInsert(null, "", InsertNoticeDialogFragment.this);
				}
			});
		return builder.create();
	}
}
