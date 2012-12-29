package com.mobiosis.images;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.concurrent.Executor;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.mobiosis.common.FileHelper;
import com.mobiosis.imagecache.R;

/**
 * Implementation of Image cache
 * 
 * @author KRZYSZTOF
 *
 */
public class ImageCache {
	//this is the map with loaded images
	private LruCache<String, Bitmap> mCachedImages;
	//queue for loading images
	private HashMap<String, AbstractImageTask> mRequestQueue;
	//activity for receiving image loaded events
	private ViewObserver mViewObserver;
	
	private final String LOG_TAG = "ImageCache";
	
	/**
	 * Interface for common objects containing image reference
	 * @author KRZYSZTOF
	 *
	 */
	public interface AbstractImage {
		/**
		 * usually checks if the image source is not null or empty
		 * @return
		 */
		public boolean hasImage();
		
		/**
		 * return the Uri of the image
		 * @return
		 */
		public Uri getUri();
		
		/**
		 * get the full path to access the file in the file system
		 * @param context
		 * @return
		 */
		public String getFullPath(Context context);
		
		/**
		 * create the relative path to the file in the file system
		 * each image class will have it's own algorithm to create the name
		 * @return
		 */
		public String getPath();
	}
	
	/**
	 * Interface
	 * @author KRZYSZTOF
	 *
	 */
	public interface ViewObserver {
		public void viewUpdated();
		public int maxSize();
	}
	
	/**
	 * Implement this interface in the task which gets the image from the server
	 * @author KRZYSZTOF
	 *
	 */
	public interface ImageTask {
		public Bitmap getBitmap();
	}
		
	public ImageCache(ViewObserver vo) {
		mViewObserver = vo;
		int cacheSize = 4 * 1024 * 1024; // x MiB
		mCachedImages = new LruCache<String, Bitmap>(cacheSize) {
		       protected int sizeOf(String key, Bitmap value) {
		           return value.getByteCount();
		   }};
		mRequestQueue = new HashMap<String, AbstractImageTask>(10);
	}
	
	public void setObserver(ViewObserver vo) {
		mViewObserver = vo;
	}

	/**
	 * fetches image from the images already in the memory
	 * if not loaded yet, various methods trigger the image downloading/resizing/loading in the background
	 * @param ctx
	 * @param imageObj
	 * @return
	 */
	public Bitmap getImage(Context ctx, AbstractImage imageObj) {
		String name = imageObj.getPath();
		String fullName = imageObj.getFullPath(ctx);
		Uri uri = imageObj.getUri();
		
		//try to fetch images from the mem cache 
		Bitmap bmp = mCachedImages.get(fullName);
		if (bmp != null) {
			return bmp;
		}
		
		//check if the local file cache contains this image
		File diskFile = null;
		try {
			diskFile = new File (FileHelper.getImageCacheDir(ctx), name);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		AbstractImageTask request = null;
		Executor exec = null;
		
		// if no image is present locally (in the application), fetch it from the outside 
		if ((diskFile == null || !diskFile.exists())) {
			
			if (imageObj instanceof RemoteImage) {
				if (uri.toString().substring(0,4).compareToIgnoreCase("http") == 0) {
					//check if we're not downloading the file already and start the new download if needed
					if (!mRequestQueue.containsKey(fullName)) {
						request = new ImageHttpDownloader( ctx, this, imageObj);
						exec = AbstractImageTask.THREAD_POOL_EXECUTOR;
					}
				} else if (uri.toString().startsWith(ImagePicasaDownloader.IMAGE_PROVIDER_PICASA.toString())){
					//check if we're not loading the file already and start the new loader if needed
					if (!mRequestQueue.containsKey(fullName)) {
						//query from the content provider
						request = new ImagePicasaDownloader(ctx, this, imageObj);
						exec = AbstractImageTask.THREAD_POOL_EXECUTOR;
					}
				} else if (uri.toString().startsWith(ContentResolver.SCHEME_CONTENT)) {
					//check if we're not loading the file already and start the new loader if needed
					if (!mRequestQueue.containsKey(fullName)) {
						//query from the content provider
						request = new ImageRequester(ctx, this, imageObj);
						exec = AbstractImageTask.SERIAL_EXECUTOR;
					}
				}
			} else
			if (imageObj instanceof DrawableImage) {
				//load the file from the resources (if not loading yet...)
				if (!mRequestQueue.containsKey(fullName)) {
					request = new ImageLoader(ctx, this, imageObj);
					//try not to clog the device's sd card, read files serially
					//this should be pretty fast though so it could work well even without the executor 
					exec = AbstractImageTask.SERIAL_EXECUTOR;
				}
			}
		} else 		
		if (!mRequestQueue.containsKey(fullName)) {
			//if image is present locally load the file from the disk
			//check if we're not loading the file already and start the new loader if needed
			request = new ImageLoader(ctx, this, imageObj);
			exec = AbstractImageTask.SERIAL_EXECUTOR;
		}
		
		//execute the request
		if (request != null) {
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
				request.execute();
			else
				request.executeOnExecutor(exec);
			mRequestQueue.put(fullName, request);
			Log.d(getClass().getSimpleName(), fullName + " " + request);
		}
		
		//file is being downloaded from the web or fetched from profider or from the disk now...
		return null;
	}

	/**
	 * callback from the AbstractImageTask
	 * @param ctx
	 * @param imageObject
	 * @param imageDownloader
	 */
	public void imageTaskFinished(Context ctx, AbstractImage imageObject, AbstractImageTask imageTask) {
		String fullName = imageObject.getFullPath(ctx);
		//remove imageObj from the download queue and load the file from disk asynchronously...
		Object o = mRequestQueue.remove(fullName);
		if ((Object)imageTask != o){
			Log.d(LOG_TAG, "image task " + imageTask + " caused some leak at " + fullName);
		}
		
		Bitmap bmp = imageTask.getBitmap();
		if (bmp != null)
			mCachedImages.put(fullName, bmp);
		
		mViewObserver.viewUpdated();
	}

	/**
	 * show broken image in the UI
	 * this file will load instantaneously if not loaded yet 
	 * @return
	 */
	public Bitmap getBrokenImg(Context context) {
		//we will show a "broken image" if the image loading failed
		Bitmap brokenBitmap = mCachedImages.get(FileHelper.getDrawableDir() + File.pathSeparator +  Integer.toString(R.drawable.broken));
		if (brokenBitmap == null) {
			brokenBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.broken);
			mCachedImages.put(FileHelper.getDrawableDir() + File.pathSeparator +  Integer.toString(R.drawable.broken), brokenBitmap);
		}
		
		return brokenBitmap;
	}
	public Bitmap getBrokenImg(Context context, int id) {
		//we will show a "broken image" if the image loading failed
		Bitmap brokenBitmap = mCachedImages.get(FileHelper.getDrawableDir() + File.pathSeparator +  Integer.toString(R.drawable.broken));
		if (brokenBitmap == null) {
			brokenBitmap = BitmapFactory.decodeResource(context.getResources(), id);
			mCachedImages.put(FileHelper.getDrawableDir() + File.pathSeparator +  Integer.toString(id), brokenBitmap);
		}
		
		return brokenBitmap;
	}

	public int getMaxSize() {
		return mViewObserver.maxSize();
	}

}
