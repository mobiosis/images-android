package com.mobiosis.images;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.mobiosis.common.FileHelper;
import com.mobiosis.images.ImageCache.AbstractImage;


/**
 * Image defined by the Uri it points to
 * it might be an image from a content provider or an image on the web
 * @author KRZYSZTOF
 *
 */
public class RemoteImage implements AbstractImage{
	private Uri imageUrl = null;
	
	public RemoteImage(String uriString) {
        this.imageUrl = null;
        if ( TextUtils.isEmpty( uriString ) )
            return;

        Uri uri = Uri.parse(uriString);
		Uri.Builder b = uri.buildUpon();
        this.imageUrl = b.build();
	}
	
	public RemoteImage(Uri uri) {
		Uri.Builder b = uri.buildUpon();
    	this.imageUrl = b.build();
	}

	@Override
	public boolean hasImage() {
    	return imageUrl != null;
	}

	@Override
	public Uri getUri() {
		return imageUrl;
	}

	@Override
	public String getPath() {
		if (imageUrl == null)
			return null;
		
		return Integer.toString(imageUrl.hashCode());
	}

	@Override
	public String getFullPath(Context ctx) {
		try {
			return FileHelper.getImageCacheDir(ctx) + File.separator + "ri" + File.separator +getPath();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}


}
