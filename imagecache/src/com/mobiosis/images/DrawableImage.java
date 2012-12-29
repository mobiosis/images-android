package com.mobiosis.images;

import java.io.File;

import android.content.Context;
import android.net.Uri;

import com.mobiosis.common.FileHelper;
import com.mobiosis.images.ImageCache.AbstractImage;

/**
 * implementation of image object for fetching drawables
 * @author KRZYSZTOF
 *
 */
public class DrawableImage implements AbstractImage {
	int drawable = 0;

	@Override
	public boolean hasImage() {
		return drawable != 0;
	}

	/**
	 * ImageDrawable is a special case, doesn't have a uri
	 */
	@Override
	public Uri getUri() {
		return null;
	}

	@Override
	public String getPath() {
		return Integer.toString(drawable);
	}
	
	public void setDrawable(int drawable) {
		this.drawable = drawable;
	}

	@Override
	public String getFullPath(Context ctx) {
		return FileHelper.getDrawableDir() + File.pathSeparator + getPath();
	}

}
