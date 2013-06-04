package com.bignerdranch.android.criminalintent;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

public class TimePickerFragment extends DialogFragment {
	public static final String EXTRA_TIME = "com.bignerdranch.android.criminalintent.time";
	
	private Date mDate;
	
	public static TimePickerFragment newInstance(Date date)
	{
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_TIME, date);
		
		TimePickerFragment fragment = new TimePickerFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mDate = (Date)getArguments().getSerializable(EXTRA_TIME);
		
		// Create a calendar to get year,month,day
		final Calendar cal = Calendar.getInstance();
		cal.setTime(mDate);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		
		View v = getActivity().getLayoutInflater()
				.inflate(R.layout.dialog_time, null);
		

		TimePicker timePicker = (TimePicker)v.findViewById(R.id.dialog_time_timePicker);
		timePicker.setCurrentHour(hour);
		timePicker.setCurrentHour(minute);
		
		timePicker.setOnTimeChangedListener(new OnTimeChangedListener() {
			
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				final Calendar cal = Calendar.getInstance();
				cal.setTime(mDate);
				int year = cal.get(Calendar.YEAR);
				int monthOfYear = cal.get(Calendar.MONTH);
				int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
				
				mDate = new GregorianCalendar(year, monthOfYear, dayOfMonth, hourOfDay, minute).getTime();
				
				// Update argument to preserve selected value on rotation
				getArguments().putSerializable(EXTRA_TIME, mDate);
			}
		});
		
		return new AlertDialog.Builder(getActivity())
			.setView(v)
			.setTitle(R.string.date_picker_title)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendResult(Activity.RESULT_OK);
				}
			})
			.create();
	}
	
	private void sendResult(int resultCode) {
		if (getTargetFragment() == null)
			return;
		
		Intent i = new Intent();
		i.putExtra(EXTRA_TIME, mDate);
		
		getTargetFragment()
			.onActivityResult(getTargetRequestCode(), resultCode, i);
	}
}
