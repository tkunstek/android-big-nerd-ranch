package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PollService extends IntentService {
	private static final String TAG = "PollService";
//	private static final int POLL_INTERVAL = 1000*60*5; // 5 minutes
	
	public PollService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ConnectivityManager cm = (ConnectivityManager)
				getSystemService(Context.CONNECTIVITY_SERVICE);
		@SuppressWarnings("deprecation")
		boolean isNetworkAvailable = cm.getBackgroundDataSetting() && cm.getActiveNetworkInfo() != null;
		if(!isNetworkAvailable)return;
		
		ArrayList<GalleryItem> items;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String query = prefs.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
		String lastResultId = prefs.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);
				
		if (query != null) {
			items = new FlickrFetchr().search(query, 1);
		} else {
			items = new FlickrFetchr().fetchItems(1);
		}
		
		String resultId = items.get(0).getId();
		
		if (!resultId.equals(lastResultId)) {
			Log.i(TAG, "Got a new result: " + resultId);
			
			Resources r = getResources();
			PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this,PhotoGalleryActivity.class), 0);
			
			Notification notification = new NotificationCompat.Builder(this)
				.setTicker(r.getString(R.string.new_pictures_title))
				.setSmallIcon(android.R.drawable.ic_menu_report_image)
				.setContentTitle(r.getString(R.string.new_pictures_title))
				.setContentText(r.getString(R.string.new_pictures_text))
				.setContentIntent(pi)
				.setAutoCancel(true)
				.build();
			
			NotificationManager notificationManager = (NotificationManager)
					getSystemService(NOTIFICATION_SERVICE);
			
			notificationManager.notify(0, notification);
		}
		
		prefs.edit()
			.putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId)
			.commit();
	}
	
	public static void setServiceAlarm(Context context, boolean isOn) {
		Intent i = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
		
		AlarmManager alarmManager = (AlarmManager)
				context.getSystemService(Context.ALARM_SERVICE);
		
		if (isOn) {
//			alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), POLL_INTERVAL, pi);
			alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
		} else {
			alarmManager.cancel(pi);
			pi.cancel();
		}
	}
	
	public static boolean isServiceAlarmOn(Context context) {
		Intent i = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
		return pi != null;
	}

}
