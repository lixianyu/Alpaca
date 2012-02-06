package us.nb9.androidinfo;

import android.os.Environment;
import android.util.Log;

public class AlpacaConfig {
	private static final String TAG = "AlpacaConfig";
	
	public static boolean ALPACA_DEBUG = true;
	public static boolean bMEM_SNAPSHOT = false;
	public static boolean bCPU_SNAPSHOT = false;
	
	public enum ENUM_STORE_TYPE{
		SDCARD_TYPE,//��ǰ�Ĵ洢����Ϊ�洢��SD������
		MEMORY_TYPE //��ǰ�Ĵ洢����Ϊ�洢���ڴ���
	}		
	public static ENUM_STORE_TYPE CURR_STORE_TYPE = ENUM_STORE_TYPE.SDCARD_TYPE;	//��ǰ�Ĵ洢����
	
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
