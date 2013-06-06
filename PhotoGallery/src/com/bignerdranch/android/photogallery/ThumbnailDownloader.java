package com.bignerdranch.android.photogallery;

import android.os.HandlerThread;
import android.util.Log;

public class ThumbnailDownloader<Token> extends HandlerThread {
	public static final String TAG = "ThumbnailDownloader";
	
	public ThumbnailDownloader() {
		super (TAG);
	}
	
	public void queueThumbnail(Token token, String url) {
		Log.i(TAG, "Got an URL: " + url);
	}
}
