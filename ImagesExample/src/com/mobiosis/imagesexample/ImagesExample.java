package com.mobiosis.imagesexample;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.mobiosis.images.ImageCache;
import com.mobiosis.images.ImageCache.ViewObserver;
import com.mobiosis.images.RemoteImage;
import com.mobiosis.imagesexample.FlickrTask.FlickrTaskObserver;

public class ImagesExample extends Activity implements ViewObserver, FlickrTaskObserver {
	
	/**
	 * put your flickr api_key (app might work without it, but I can't guarantee it
	 * you can obtain the key at http://www.flickr.com/services/apps/create/apply/
	 */
	final static String api_key = "";
	
	final static String LOG_TAG = "ImagesExample";
	
	class GridAdapter extends BaseAdapter {
		ArrayList<String> list;
		Context context;
		
		public GridAdapter(Context context, ArrayList<String> images) {
			this.context = context;
			this.list = images;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView = null;
			if (convertView != null) {
				imageView = (ImageView)convertView;
			} else {
				imageView = new ImageView(context);
			}
			
			RemoteImage ri = new RemoteImage(list.get(position));
			Bitmap bmp = instance.imageCache.getImage(context, ri);
			if (bmp == null)
				bmp = instance.imageCache.getBrokenImg(context);
			imageView.setImageBitmap(bmp);
			
			//fetch more pages if possible
			if (position == getCount()-1)
				ImagesExample.this.fetchNextPage();
			
			return imageView;
		}
		
		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}

	
	private class ClassInstance {
		ImageCache imageCache = null; 
		FlickrTask imageTask;
		
		public ClassInstance(ViewObserver vo) {
			imageCache = new ImageCache(vo);
		}

		public void setObserver(ImagesExample ImagesExample) {
			imageCache.setObserver(ImagesExample);
			if (imageTask != null)
				imageTask.setObserver(ImagesExample);
		}
	}
	
	//
	int maxSize = -1;
	
	//non-serializable
	private ClassInstance instance;
	//serializable
	private ArrayList<String> mImages = new ArrayList<String>();
	int mPages = 1; //we always fetch at least one page
	Uri mImageUri = Uri.parse("http://www.mobiosis.com/apps/test/flickr.php?page=0&per_page=20&type=q" +
			"&api_key=" + api_key);

	//view handles - no need to save instance state
	GridView gridView;
	GridAdapter gridAdapter;
	ProgressBar progressBar;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		instance = (ClassInstance)getLastNonConfigurationInstance();
		if (instance == null) {
			instance = new ClassInstance(this);
		} else {
			instance.setObserver(this);
		}
		
		if (savedInstanceState != null) {
			mImages = savedInstanceState.getStringArrayList("mImages");
		}
		
		gridView = (GridView) findViewById(R.id.gridView1);
		gridAdapter = new GridAdapter(this, mImages);
		gridView.setAdapter(gridAdapter);
		progressBar = (ProgressBar) findViewById(R.id.progress);
		
		//load images from 
		if (mImages.isEmpty()) {
			fetchNextPage();
		}
		
		reloadImages();
	}
	
	public void setFlickrImages(ArrayList<String> result, int pages) {
		if (result != null && result.size()>0) {
			mImages.addAll(result);
			gridAdapter.notifyDataSetChanged();
			mPages = pages;
		}
		progressBar.setVisibility(View.INVISIBLE);
	}

	@Override
	@Deprecated
	public Object onRetainNonConfigurationInstance() {
		// TODO Auto-generated method stub
		return instance;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		outState.putStringArrayList("mImages", mImages);
	}

	public void onButtonCache(View v) {
		//TODO clean cache
	}
	
	@Override
	public void viewUpdated() {
		reloadImages();
	}

	@Override
	public int maxSize() {
		if (maxSize == -1) {
			DisplayMetrics displayMetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
			maxSize = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
		}
		return maxSize;
	}
	
	void reloadImages() {
		//refresh grid adapter
		gridAdapter.notifyDataSetChanged();
	}
	
	void fetchNextPage() {
		//first check if we're already running the fetch task
		if (instance.imageTask == null || instance.imageTask.getStatus() == AsyncTask.Status.FINISHED) {
			//check which page are we on
			HashMap<String, String> params = getParameters(mImageUri);
			int page = Integer.parseInt(params.get("page"));
			//increment the page and write the path back to the url
			if (page < mPages) {
				params.put("page", Integer.toString(page+1));
				Uri.Builder builder = mImageUri.buildUpon();
				builder.encodedQuery("");
				for (String key: params.keySet())
					builder.appendQueryParameter(key, params.get(key));
				mImageUri = builder.build();
			}
			instance.imageTask = new FlickrTask(this);
			instance.imageTask.execute(mImageUri.toString());
			
			//show progress bar
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	private HashMap<String, String> getParameters(Uri imageUri) {
		HashMap<String, String> params = new HashMap<String, String>();
		String query = imageUri.getQuery();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			String[] param = pair.split("=");
			params.put(param[0], param[1]);
		}
		return params;
	}

}
