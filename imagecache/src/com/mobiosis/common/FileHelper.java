package com.mobiosis.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

/**
 * Filter for temporary files
 * 
 * @author KRZYSZTOF
 * 
 */
class TempFilesFilter implements FilenameFilter {
	public boolean accept(File dir, String s) {
		if (s.endsWith(FileHelper.TEMP_FILE))
			return true;
		return false;
	}
}

/**
 * Collection of various static methods for file manipulation
 * 
 * @author KRZYSZTOF
 * 
 */
public class FileHelper {

	private static final String LOG_TAG = "FileHelper";

	public static final String IMAGES_PATH = "images";
	public static final String DATA_PATH = "data";
	public static final String CONTROLS_PATH = "controls";

	public static final String TEMP_FILE = ".temp";

	private static File imageCacheDir = null;
	private static File dataDir = null;
	private static File controlsDir = null;

	/**
	 * cleaning temp files
	 * 
	 * @param dir
	 */
	private static void deleteTempFiles(File dir) {
		int count = 0;
		File[] files = null;
		try {
			files = dir.listFiles(new TempFilesFilter());
		} catch (SecurityException se) {
			Log.e(LOG_TAG, "Cannot access file " + dir.getAbsolutePath(),
					se);
		}
		for (File file : files) {
			file.delete();
			count++;
		}
		Log.i(LOG_TAG, "Deleted " + count + " temp files");
	}

	/**
	 * Create cache directory within the file system use the most convenient
	 * location (priority for external)
	 * Writing to this path requires the WRITE_EXTERNAL_STORAGE permission.
	 * 
	 * @param ctx
	 * @return
	 * @throws FileNotFoundException
	 */
	public static synchronized File getCacheDir(Context ctx, String subPath)
			throws FileNotFoundException {

		// get external or internal cache dir
		File dir = null;
		try {
			dir = ctx.getExternalCacheDir();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(dir == null) {
			Log.d(LOG_TAG, "External Cache Dir not available");
			dir = ctx.getCacheDir();
		}

		dir = new File(dir, subPath);
		dir.mkdirs();

		if (!dir.exists())
			throw new FileNotFoundException("Could not create image cache directory");

		deleteTempFiles(dir);

		return dir;
	}

	/**
	 * Images dir
	 * @param ctx
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getImageCacheDir(Context ctx)
			throws FileNotFoundException {
		if (imageCacheDir == null) {
			imageCacheDir = getCacheDir(ctx, IMAGES_PATH);
			imageCacheDir.mkdirs();
			Log.i(LOG_TAG, "Created image cache dir: " + imageCacheDir);
		}

		return imageCacheDir;
	}

	/**
	 * data dir
	 * @param ctx
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getDataDir(Context ctx) throws FileNotFoundException {
		if (dataDir == null) {
			dataDir = getCacheDir(ctx, DATA_PATH);
			dataDir.mkdirs();
			Log.i(LOG_TAG, "Data dir: " + dataDir);
		}

		return dataDir;
	}

	public static String getDrawableDir() {
		return DATA_PATH;
	}

	/**
	 * controls dir
	 * @param ctx
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getControlsDir(Context ctx) throws FileNotFoundException {
		if (controlsDir == null) {
			controlsDir = getCacheDir(ctx, CONTROLS_PATH);
			controlsDir.mkdirs();
			Log.i(LOG_TAG, "Controls dir: " + controlsDir);
		}

		return controlsDir;
	}

	/**
	 * file move
	 * 
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static void moveFile(File from, File to) throws IOException {
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}

		if (to.exists())
			Log.i(LOG_TAG, "Files moved from: " + from + " to: " + to);
		else
			throw new IOException("Error moving file");
	}

	/**
	 * file copy
	 * 
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static void copyFile(File from, File to) throws IOException {
		FileInputStream in = new FileInputStream(from);
		FileOutputStream out = new FileOutputStream(to);
		byte[] buf = new byte[1024];
		int i = 0;
		while ((i = in.read(buf)) != -1) {
			out.write(buf, 0, i);
		}
		in.close();
		out.close();
	}
	
	/**
	 * stream copy
	 * 
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public static void copyStream(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = is.read(buffer)) != -1)
			os.write(buffer, 0, len);
		//flush the stream
		os.flush();
	}

}
