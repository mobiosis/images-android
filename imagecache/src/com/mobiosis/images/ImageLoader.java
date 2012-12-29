package com.mobiosis.images;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.mobiosis.common.FileHelper;
import com.mobiosis.images.ImageCache.AbstractImage;

/**
 * Decode bitmap from the file
 * @author KRZYSZTOF
 *
 */
public class ImageLoader extends AbstractImageTask {
	private final String LOG_TAG = "ImageLoader";

	private AbstractImage 	imageObject;
	private Context				context;
	private ImageCache			imageCache;


	public ImageLoader( Context ctx, ImageCache ic, AbstractImage imageObj) {
		this.context = ctx;
		this.imageObject = imageObj;
		this.imageCache = ic;
	}
	
	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected Boolean doInBackground( Uri... url ) {
		try {
			if ( imageObject instanceof RemoteImage)
				mBitmap = getBitmap( this.context, this.imageObject.getPath() );
			else if (imageObject instanceof DrawableImage) {
				int imageId = Integer.parseInt(imageObject.getPath());
				mBitmap = BitmapFactory.decodeResource(context.getResources(), imageId);
			}
		} catch ( Exception e ) {
			Log.e( LOG_TAG, "Image decode failed", e );
		}
		if (mBitmap != null)
			return true;
		return false;
	}
	
	@Override
	protected void onPostExecute( Boolean success ) {
		if (success)
			imageCache.imageTaskFinished(context, imageObject, this);
	}
	
	/**
	 * decode bitmap from file
	 * @param ctx
	 * @param subPath
	 * @return
	 * @throws FileNotFoundException
	 */
	private static Bitmap getBitmap( Context ctx, String subPath ) 
	throws FileNotFoundException {
		File file = new File( FileHelper.getImageCacheDir(ctx), subPath );

		if ( ! file.exists() )
			throw new FileNotFoundException( "no such image file: " + subPath );
		return BitmapFactory.decodeFile( file.toString() );
	}

}
