package com.mobiosis.images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.mobiosis.common.FileHelper;
import com.mobiosis.images.ImageCache.AbstractImage;

/**
 * Request image from the media provider
 * 
 * @author KRZYSZTOF
 *
 */
public class ImageRequester extends AbstractImageTask {
	private final static String LOG_TAG = "ImageRequester";

	private AbstractImage imageObject;
	private Context context;
	private ImageCache imageCache;

	private static final String[] mediaColumns = {
			MediaStore.Images.ImageColumns.ORIENTATION };

	public ImageRequester(Context ctx, ImageCache ic, AbstractImage imageObj) {
		this.context = ctx;
		this.imageObject = imageObj;
		this.imageCache = ic;
	}

	@Override
	protected void onPreExecute() {
	}

	/**
	 * This is the background task run in a separate thread
	 */
	@Override
	protected Boolean doInBackground(Uri... url) {
		try {
			mBitmap = getImage(imageObject.getUri(),
					imageObject.getFullPath(context));
			if (mBitmap != null)
				return true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "Image download failed", e);
		}

		return false;
	}

	/**
	 * This is called when complete on the UI thread
	 */
	@Override
	protected void onPostExecute(Boolean success) {
		// inform the image cache about finished download
		if (success)
			imageCache.imageTaskFinished(context, imageObject, this);
	}


	/**
	 * get the image description from the database, but fetch the image from the file
	 * @param uri
	 * @param outputFileName
	 * @return
	 * @throws IOException
	 */
	private Bitmap getImage(Uri uri, String outputFileName) throws IOException {

		String selection = MediaStore.Images.ImageColumns._ID + "=" + uri.getLastPathSegment();
		Bitmap resizedBitmap = null;
		//String fullPath = null;
		InputStream is = null;
		int rotation = 0;

		ContentResolver cr = context.getContentResolver();
		Cursor cur = null;
		try {
			if (uri.toString().startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())) {
				cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						mediaColumns, selection, null, null);
				if (cur.moveToFirst()) {
					rotation = cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
				}
				is = cr.openInputStream(Uri.parse(uri.toString()));
			} else if (uri.toString().startsWith(ContactsContract.Contacts.CONTENT_URI.toString())) {
		          is = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
			} else {
		          is = cr.openInputStream(uri);
			}
		} catch (SQLiteException e) {
			Log.e(LOG_TAG, "error fetching image description", e);
		} finally {
			if (cur != null)
				cur.close();
		}

		if (is == null)
			return null;
		
		String tempFilePrefix = outputFileName.substring(outputFileName.lastIndexOf(File.separator+1)) + "_";
		File tempFile = File.createTempFile(tempFilePrefix, FileHelper.TEMP_FILE,
				FileHelper.getImageCacheDir(context));
		
		FileOutputStream fos = new FileOutputStream(tempFile);
		try {
			FileHelper.copyStream(is, fos);
		} catch (IOException e) {
			Log.e(LOG_TAG, "file copy error", e);
			tempFile.delete();
		} finally {
			fos.close();
			is.close();
		}
		
		try {
			resizedBitmap = ImageHelper.resizeToCache(tempFile, new File(outputFileName), rotation, imageCache.getMaxSize());
		} catch (IOException e) {
			Log.e(LOG_TAG, "error resizing file", e);
		} finally {
			tempFile.delete();
		}

		return resizedBitmap;
	}
}
