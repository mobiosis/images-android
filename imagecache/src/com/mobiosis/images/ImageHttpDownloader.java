package com.mobiosis.images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.mobiosis.common.FileHelper;
import com.mobiosis.images.ImageCache.AbstractImage;

/**
 * Download the image from the internet
 * 
 * @author KRZYSZTOF
 * 
 */
public class ImageHttpDownloader extends AbstractImageTask {
	private final static String LOG_TAG = "ImageHttpDownloader";

	private AbstractImage imageObject;
	private Context context;
	private ImageCache imageCache;


	public ImageHttpDownloader(Context ctx, ImageCache ic, AbstractImage imageObj) {
		this.context = ctx;
		this.imageObject = imageObj;
		this.imageCache = ic;
	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected Boolean doInBackground(Uri... url) {
		try {
			mBitmap = download(this.context, imageObject.getUri(), imageObject.getPath());
			return true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "Image download failed", e);
		}

		return false;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		// inform the image cache about finished download
		if (success)
			imageCache.imageTaskFinished(context, imageObject, this);
	}

	/**
	 * Download an image from the web and save it to the image folder
	 * 
	 * @param ctx
	 * @param url
	 * @param subPath
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public Bitmap download(Context ctx, Uri url, String subPath)
			throws IOException, IllegalArgumentException {
		Log.d(LOG_TAG, "download " + url + " to " + subPath);

		if (url == null)
			return null;
		
		Bitmap bitmap = null;
		URL remoteUrl = new URL(url.toString());

		//open the connection
		HttpURLConnection conn = (HttpURLConnection) remoteUrl.openConnection();
		conn.setDoInput(true);
		conn.connect();
		
		
		InputStream is = conn.getInputStream();

		//download the image to the .temp file and once succeeded, rename it
		File tempFile = File.createTempFile(url.getLastPathSegment(), FileHelper.TEMP_FILE,
				FileHelper.getImageCacheDir(ctx));
		
		FileOutputStream fos = new FileOutputStream(tempFile);
		try {
			FileHelper.copyStream(is, fos);
		} catch (IOException e) {
			Log.e(LOG_TAG, "file copy error", e);
			fos.close();
			tempFile.delete();
		} finally {
			fos.close();
			is.close();
		}

		File outFile = new File(FileHelper.getImageCacheDir(ctx), subPath);
		try {
			// Avoid out of memory problems by resizing the image before display
			bitmap = ImageHelper.resizeToCache(tempFile, outFile, 0, imageCache.getMaxSize());
		} catch (IOException e) {
			Log.e(LOG_TAG, "error copying file", e);
		} finally {
			tempFile.delete();
		}
		
		return bitmap;
	}
}
