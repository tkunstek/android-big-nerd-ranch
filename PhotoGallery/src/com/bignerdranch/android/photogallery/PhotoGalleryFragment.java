package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	GridView mGridView;
	ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;
	private int mPage = 0;
	private boolean loading = false;
	private boolean refreshing = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);
		loadData();
		
		PollService.setServiceAlarm(getActivity(), true);
		
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
			@Override
			public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
				if (isVisible()) {
					imageView.setImageBitmap(thumbnail);
				}
			}
		});
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed");
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailThread.clearQueue();
	}
	
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView)searchItem.getActionView();
			
			// Get the data from our searchable.xml as a SearchableInfo
			SearchManager searchManager = (SearchManager)getActivity()
					.getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
			
			searchView.setSearchableInfo(searchInfo);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_search:
				getActivity().onSearchRequested();
				return true;
			case R.id.menu_item_clear:
				PreferenceManager.getDefaultSharedPreferences(getActivity())
					.edit()
					.putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
					.commit();
				refresh();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		mGridView = (GridView)v.findViewById(R.id.gridView);
		setupAdapter();
		return v;
	}
	
	private class FetchItemsTask extends AsyncTask<Void,Void,ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
			Activity activity = getActivity();
			if (activity == null)
				return new ArrayList<GalleryItem>();
			
			String query = PreferenceManager.getDefaultSharedPreferences(activity)
					.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
					
			if (query != null) {
				return new FlickrFetchr().search(query, 1);
			} else {
				return new FlickrFetchr().fetchItems(mPage);
			}
		}
		
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			if(mItems == null || refreshing) {
				mItems = items;
				refreshing = false;
				setupAdapter();
			} else {
				mItems.addAll(items);
				((BaseAdapter)mGridView.getAdapter()).notifyDataSetChanged();
			}
			loading = false;
		}
	}
	
	void setupAdapter() {
		if (getActivity() == null || mGridView == null) return;
		
		if (mItems != null) {
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		} else {
			mGridView.setAdapter(null);
		}
	}
	
	private void loadData() {
		if(loading)return;
		loading = true;
		mPage++;
		new FetchItemsTask().execute();
	}
	
	public void refresh() {
		if(loading)return;
		loading = true;
		refreshing = true;
		mPage = 1;
		mThumbnailThread.clearQueue();
		new FetchItemsTask().execute();
	}
	
	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.gallery_item, parent, false);
			}
			ImageView imageView = (ImageView)convertView.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.drawable.brian_up_close);
			TextView textView = (TextView)convertView.findViewById(R.id.gallery_item_index);
			textView.setText(String.format("%d/%d", position+1, mItems.size()));
			GalleryItem item = getItem(position);
			Bitmap cacheHit = mThumbnailThread.checkCache(item.getUrl());
			if (cacheHit != null) {
				imageView.setImageBitmap(cacheHit);
			} else {
				mThumbnailThread.queueThumbnail(imageView, item.getUrl());
			}
			
			// pre-load images
			for (int i=Math.max(0, position-10); i< Math.min(mItems.size()-1, position+10); i++) {
				mThumbnailThread.queuePreload(item.getUrl());
			}
			
			// If looking at last item, fetch more items
			if (position == mItems.size() -1)
				loadData();
			
			return convertView;
		}
	}
}
