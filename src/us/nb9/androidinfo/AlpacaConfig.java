package us.nb9.androidinfo;

import android.os.Environment;
import android.util.Log;

public class AlpacaConfig {
	private static final String TAG = "AlpacaConfig";
	
	public static boolean ALPACA_DEBUG = true;
	public static boolean bMEM_SNAPSHOT = false;
	public static boolean bCPU_SNAPSHOT = false;
	
	public enum ENUM_STORE_TYPE{
		SDCARD_TYPE,//当前的存储类型为存储在SD卡下面
		MEMORY_TYPE //当前的存储类型为存储在内存中
	}		
	public static ENUM_STORE_TYPE CURR_STORE_TYPE = ENUM_STORE_TYPE.SDCARD_TYPE;	//当前的存储类型
	
	public static void init() {
		if (ALPACA_DEBUG) Log.v(TAG, "Enter init()");
		//initRadioParams();
		String sExternalStorageState = Environment.getExternalStorageState();
		if (sExternalStorageState.equals(Environment.MEDIA_MOUNTED)) {
			CURR_STORE_TYPE = ENUM_STORE_TYPE.SDCARD_TYPE;
		}
		if (ALPACA_DEBUG) Log.w(TAG, "CURR_STORE_TYPE = " + CURR_STORE_TYPE);
	}
}
