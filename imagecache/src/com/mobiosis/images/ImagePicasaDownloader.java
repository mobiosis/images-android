package com.mobiosis.images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import com.mobiosis.common.FileHelper;
import com.mobiosis.images.ImageCache.AbstractImage;

/**
 * request the picasa image from the Picasa provider
 * @author KRZYSZTOF
 *
 */
public class ImagePicasaDownloader extends AbstractImageTask {
	private final static String LOG_TAG = "ImageDownloader";

	private AbstractImage imageObject;
	private Context context;
	private ImageCache imageCache;

	public static final Uri IMAGE_PROVIDER_PICASA = Uri.parse("content://com.google.android.gallery3d.provider/picasa/");

	private static final String[] mediaColumns = {
//			ImageColumns.DISPLAY_NAME, ImageColumns.SIZE,
//			ImageColumns.MIME_TYPE, ImageColumns.DATE_TAKEN,
//			ImageColumns.LATITUDE, ImageColumns.LONGITUDE,
			ImageColumns.ORIENTATION };


	public ImagePicasaDownloader(Context ctx, ImageCache ic, AbstractImage imageObj) {
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
			mBitmap = getImage(imageObject.getUri(),
					imageObject.getFullPath(context));
			if (mBitmap != null)
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
	 * get the image description from the database, but fetch the image from the stream
	 * @param uri
	 * @param outputFileName
	 * @return
	 * @throws IOException
	 */
	private Bitmap getImage(Uri uri, String outputFileName) throws IOException {

		Bitmap resizedBitmap = null;
		int rotation = 0;

		ContentResolver cr = context.getContentResolver();
		Cursor cur = null;
		try {
			cur = cr.query(uri, mediaColumns, null, null, null);
			if (cur.moveToFirst()) {
				rotation = cur.getInt(cur.getColumnIndexOrThrow(ImageColumns.ORIENTATION));
			}
		} catch (SQLiteException e) {
			Log.e(LOG_TAG, "error fetching image description", e);
		} finally {
			if (cur != null)
				cur.close();
		}

		InputStream is = cr.openInputStream(uri);
		File tempFile = File.createTempFile(uri.getLastPathSegment(), FileHelper.TEMP_FILE,
				FileHelper.getImageCacheDir(context));
		OutputStream os = new FileOutputStream(tempFile);

		try {
			FileHelper.copyStream(is, os);
			// Avoid out of memory problems by resizing the image before display
			resizedBitmap = ImageHelper.resizeToCache(tempFile, new File(outputFileName), rotation, imageCache.getMaxSize());
		} catch (IOException e) {
			Log.e(LOG_TAG, "error copying file", e);
		} finally {
			is.close();
			os.close();
			tempFile.delete();
		}

		return resizedBitmap;
	}
}
