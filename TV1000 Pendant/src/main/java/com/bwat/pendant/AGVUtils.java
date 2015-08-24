package com.bwat.pendant;

import android.util.Log;

/**
 * @author Kareem ElFaramawi
 */
public class AGVUtils {
	private static final String tag = "AGV";

	public static void logD(String msg) {
		Log.d(tag, msg);
	}

	public static void logE(String msg) {
		Log.e(tag, msg);
	}
}
