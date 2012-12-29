package com.mobiosis.images;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * A collection of static method for image manipulation
 * @author KRZYSZTOF
 *
 */
public class ImageHelper {
	final static String LOG_TAG = "ImageHelper";

	/*
	 * This should be a good balance between cache size and cache quality
	 */
	public static final int    	IMAGE_QUALITY = 90; // percent

	
	/**
	 * resize image to the IMAGE_MAX_SIZE or smaller using the binary scaling down (it's quick!)
	 * rotate the image if needed
	 * the image will be written to the destination file if possible
	 * @param inputFile
	 * @param outputFile
	 * @param maxOutSize
	 * @param rotation
	 * @return
	 * @throws IOException
	 */
	public static Bitmap resizeToCache(File inputFile, final File outputFile, final int rotation, int maxSize)
			throws IOException {
		if ((inputFile == null) || (!inputFile.exists()))
			throw new FileNotFoundException("Input file not found: " + inputFile);

		// Decode only the image size from the input file
		BitmapFactory.Options bitmapOptions1 = new BitmapFactory.Options();
		bitmapOptions1.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(inputFile.toString(), bitmapOptions1);

		// scale the image down if needed
		int maxInSize = bitmapOptions1.outWidth > bitmapOptions1.outHeight ? bitmapOptions1.outWidth
				: bitmapOptions1.outHeight;
		int scale = 1;
		// Keep scaling down by two to avoid unnecessary processing
		while (maxInSize > maxSize) {
			maxInSize /= 2;
			scale *= 2;
		}

		// Decode the input image scaled
		BitmapFactory.Options bitmapOptions2 = new BitmapFactory.Options();
		bitmapOptions2.inSampleSize = (int) scale;
		Bitmap bitmap = BitmapFactory.decodeFile(inputFile.toString(), bitmapOptions2);

		//rotate the image if needed
		if (rotation != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(rotation);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
					matrix, true);
		}

		//make sure destination file exists 
		if (!outputFile.exists()) {
			outputFile.getParentFile().mkdirs();
			outputFile.createNewFile();
		}

		//Log.i(LOG_TAG, "Writing resized image to file: " + outputFile);
		FileOutputStream fout = new FileOutputStream(outputFile);
		if (bitmapOptions2.outMimeType != null) {
			Bitmap.CompressFormat format = bitmapOptions2.outMimeType.contains("image/png")?
					Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
			//it might fail but in the worst case the output file will not be created
			boolean success = bitmap.compress(format, IMAGE_QUALITY, fout);
			fout.flush();
			fout.close();
			if (!success) {
				outputFile.delete();
			}
		}
		return bitmap;
	}
}
