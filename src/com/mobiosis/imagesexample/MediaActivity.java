package com.mobiosis.imagesexample;

import com.mobiosis.images.ImageCache;
import com.mobiosis.images.RemoteImage;
import com.mobiosis.images.ImageCache.ViewObserver;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;

public class MediaActivity extends Activity implements ViewObserver {
	final static int PICK_IMAGE = 1;
	final static int TAKE_PHOTO = 2;
	final static String LOG_TAG = "MediaActivity";
	
	
	String [] mediaColumns =  {
			MediaStore.Images.ImageColumns._ID,
			MediaStore.Images.ImageColumns.TITLE
	};
	
	private class ClassInstance {
		ImageCache imageCache = null; 
		
		public ClassInstance(ViewObserver vo) {
			imageCache = new ImageCache(vo);
		}

		public void setObserver(ViewObserver vo) {
			imageCache.setObserver(vo);
		}
	}
	
	//
	int maxSize = -1;
	
	ImageView imageView;
	//non-serializable
	private ClassInstance instance;
	//serializable
	private Uri mImageUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_media);
		
		instance = (ClassInstance)getLastNonConfigurationInstance();
		if (instance == null) {
			instance = new ClassInstance(this);
		} else {
			instance.setObserver(this);
		}
		
		if (savedInstanceState != null) {
			String imageUri = savedInstanceState.getString("mImageUri");
			if (imageUri != null) {
				mImageUri = Uri.parse(imageUri);
			}
		}
		
		imageView = (ImageView) findViewById(R.id.image1);
		reloadImages();
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
		if (mImageUri != null)
			outState.putString("mImageUri", mImageUri.toString());
	}

	public void onButton1(View v) {
		//load from camera
		ContentValues values = new ContentValues(); 
		values.put( Images.Media.MIME_TYPE, "image/jpeg" );
		Uri uri = getContentResolver().insert( Media.EXTERNAL_CONTENT_URI , values );
		Intent i = new Intent( MediaStore.ACTION_IMAGE_CAPTURE ); 
		i.putExtra( MediaStore.EXTRA_OUTPUT, uri );
		startActivityForResult( i, TAKE_PHOTO );			
	}
	
	public void onButton2(View v) {
		//load from gallery
		Intent intent = new Intent( Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI );
		startActivityForResult( intent, PICK_IMAGE );
	}
	
	public void setImageUri(Uri bitmapUri) {
		mImageUri = bitmapUri;
		reloadImages();
	}

	private void reloadImages() {
		//refresh the ImageView in the second tab
		Bitmap bmp = null;
		if (mImageUri != null) {
			RemoteImage ri = new RemoteImage(mImageUri);
			bmp = instance.imageCache.getImage(this, ri);
			if (bmp == null)
				bmp = instance.imageCache.getBrokenImg(this);
		}
		imageView.setImageBitmap(bmp);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_media, menu);
		return true;
	}

	@Override
	public void viewUpdated() {
		reloadImages();
	}

	@Override
	public int maxSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	   @Override
	    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    	
	    	Uri bitmapUri = null;
			if ( resultCode == RESULT_OK ) {
				switch ( requestCode ) {
				case PICK_IMAGE:
					bitmapUri = intent.getData();
				break;
					
				case TAKE_PHOTO:
				{
					if ( resultCode == RESULT_OK ) {
						if (intent != null)
							bitmapUri = intent.getData();
						if (bitmapUri == null) {
							String latestSort = MediaStore.Images.ImageColumns._ID + " DESC";
							ContentResolver cr = getContentResolver();
							Cursor cur = null;
							 try {
								 cur = cr.query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaColumns, null, null, latestSort );
								 if ( cur.moveToFirst() ) {
									 long imageId = cur.getLong( cur.getColumnIndexOrThrow( MediaStore.Images.ImageColumns._ID ) );
									 bitmapUri = Uri.withAppendedPath( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf( imageId ) );						 
								 }
							 }
							 catch (SQLiteException e) {
								 Log.e(LOG_TAG, "media fetch failure", e);
							 }
							finally {
								if (cur != null)
									cur.close();
							}
						}
					}
				}
				break;
				}
			}
			
			if (bitmapUri != null) {
				Log.v(LOG_TAG, "bitmapUri = " + bitmapUri);
				setImageUri(bitmapUri);
			}
	    }


}
