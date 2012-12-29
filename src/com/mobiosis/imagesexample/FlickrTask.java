package com.mobiosis.imagesexample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.mobiosis.common.FileHelper;

public class FlickrTask extends AsyncTask<String, Void, ArrayList<String>> {
	public interface FlickrTaskObserver {
		public void setFlickrImages(ArrayList<String> images, int pages);
	}
	
	FlickrTaskObserver mTaskObserver;
	int pages; //additional result
	
	public FlickrTask(FlickrTaskObserver to) {
		mTaskObserver = to;
	}
	
	public void setObserver(FlickrTaskObserver to) {
		mTaskObserver = to;
	}

	@Override
	protected ArrayList<String> doInBackground(String... params) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(params[0]);
		try {
			HttpResponse resp = client.execute(get);
            HttpEntity entity = resp.getEntity();
            InputStream is = entity.getContent();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            FileHelper.copyStream(is, os);
			is.close();
			
			JSONObject json = null;
			json = new JSONObject(new String(os.toByteArray()));
			
			final JSONObject flickr = json.getJSONObject("photos");
			pages = flickr.getInt("pages");
			final JSONArray results = flickr.getJSONArray("photo");
            final ArrayList<String> urls = new ArrayList<String>();
            for (int i = 0; i < results.length(); i++) {
                urls.add(results.getString(i));
            }
            return urls;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	@Override
	protected void onPostExecute(ArrayList<String> result) {
		mTaskObserver.setFlickrImages(result, pages);
	}
	
}
