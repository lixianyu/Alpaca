package us.nb9.androidinfo.service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import us.nb9.androidinfo.AlpacaApp;
import us.nb9.androidinfo.AlpacaConfig;
import us.nb9.androidinfo.util.AlpacaUtil;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AndroidSystemInfo extends Service {
	private static final String TAG = "AndroidSystemInfo";
	private static final boolean DEBUG = AlpacaConfig.ALPACA_DEBUG&&true;
    
	private static AndroidSystemInfo mAndroidSystemInfo;
	private static String mNetworkType;
	private static ArrayList <String> mCpuList = null;
	private static ArrayList <Long> mMemList = null;
	private static int mPid;
	private String mStrProcStat, mStrProcPidStat;
	private LineNumberReader mlnrProcStat = null;
	private LineNumberReader mlnrProcPidStat = null;
	private static String mPkgName;
	
    private Looper mServiceLooper;
    private static ServiceHandler mServiceHandler;

    private static final int EVENT_QUIT = 1000;
    private static final int EVENT_STOP_SESSION = 1001;
    private static final int EVENT_DO = 1002;
    private static final int EVENT_CPU_SNAPSHOT = 1003;
    private static final int EVENT_MEM_SNAPSHOT = 1004;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
            if (DEBUG) Log.v(TAG, "Enter ServiceHandler(), looper = " + looper);
        }

        public void handleMessage(Message msg) {
            if (DEBUG) Log.v(TAG, "Enter handleMessage(), msg = " + msg);

            // Normally we would do some work here, like download a file.
            switch (msg.what) {
            case EVENT_QUIT :
                getLooper().quit();
                break;

            case EVENT_STOP_SESSION :
            	stopSelf();
            	break;
            	
            case EVENT_DO :
            	mNetworkType = getNetworkType();
//            	beginCpuUtilizationRate();
//            	beginMemUtilizationRate();
            	getOsBuild();
            	break;
            	
            case EVENT_CPU_SNAPSHOT :
            	cpuShots();
            	break;
            	
            case EVENT_MEM_SNAPSHOT :
            	memShots();
            	break;
            	
            default :
                Log.w(TAG, "msg.what=" + msg.what);
                return;
            }
        }
    }
    
    @Override
    public void onCreate() {
        if (DEBUG) Log.v(TAG, "Enter onCreate()");

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("AndroidSystemInfoService",
                		Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mAndroidSystemInfo = this;
        mNetworkType = "Wifi";
        mCpuList = new ArrayList<String>();
        mMemList = new ArrayList<Long>();
	    if (DEBUG) Log.d(TAG, "Leave onCreate()");
    }
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int mes = -1;
        int arg2 = 0;
        byte[] byUserData = null;

        // Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        if (DEBUG) Log.v(TAG, "onStartCommand(), flags = " + flags + ", startId=" + startId + ", intent=" + intent);

        if (intent == null) {
        	return START_STICKY;
        }
        mes = EVENT_DO;
        
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the
        // job
        Message msg = mServiceHandler.obtainMessage(mes);
        msg.arg1 = startId;
        msg.arg2 = arg2;
        msg.obj = byUserData;
        if (mServiceHandler.getLooper().getThread().isAlive()) { 
        	mServiceHandler.sendMessage(msg);
        }
        // If we get killed, after returning from here, restart
        return START_STICKY;
        //return START_NOT_STICKY;
        //return START_REDELIVER_INTENT;
    }

	@Override
    public void onDestroy() {
        if (DEBUG) Log.v(TAG, "Enter onDestroy()");

        mServiceHandler.sendEmptyMessage(EVENT_QUIT);
    }
	
	private void beginCpuUtilizationRate() {
		if (DEBUG) Log.v(TAG, "Enter beginCpuUtilizationRate()");
		if (DEBUG) Log.d(TAG, "mPid = " + mPid);
		
		try {
			mlnrProcStat = new LineNumberReader( new FileReader( "/proc/stat" ) );
			mStrProcStat = mlnrProcStat.readLine();
			if (DEBUG) Log.d(TAG, "mStrProcStat = " + mStrProcStat);
			
			mlnrProcPidStat = new LineNumberReader( new FileReader( "/proc/"+mPid+"/stat" ) );
			mStrProcPidStat = mlnrProcPidStat.readLine();
			if (DEBUG) Log.d(TAG, "mStrProcPidStat = " + mStrProcPidStat);
			mCpuList.add(mStrProcStat);
			mCpuList.add(mStrProcPidStat);
			mlnrProcStat.close();
			mlnrProcPidStat.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException : " + e);
		} catch (IOException e) {
			Log.e(TAG, "IOException : " + e);
		}
		if (AlpacaConfig.bCPU_SNAPSHOT) {
			mServiceHandler.sendEmptyMessageDelayed(EVENT_CPU_SNAPSHOT, 2000);
		}
		if (DEBUG) Log.v(TAG, "Leave beginCpuUtilizationRate()");
	}
	
	private void beginMemUtilizationRate() {
		if (DEBUG) Log.v(TAG, "Enter beginMemUtilizationRate()");

//		long lSysUsedMem = getSysUsedMemory();
//		if (DEBUG) Log.d(TAG, "lSysUsedMem = " + lSysUsedMem);
		
		long iProcUsedMem = getProcUsedMemory();
		if (DEBUG) Log.d(TAG, "iProcUsedMem = " + iProcUsedMem);

//		mMemList.add(lSysUsedMem);
		mMemList.add(iProcUsedMem);

		if (AlpacaConfig.bMEM_SNAPSHOT) {
			mServiceHandler.sendEmptyMessageDelayed(EVENT_MEM_SNAPSHOT, 1000);
		}
		if (DEBUG) Log.v(TAG, "Leave beginMemUtilizationRate()");
	}
	
	private long getSysAvailMemory() {
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();  
        am.getMemoryInfo(mi);
        long lAvailMem = mi.availMem;//当前系统的可用内存 
        if (DEBUG) Log.i(TAG, "lAvailMem = " + lAvailMem + ", threshold = " + mi.threshold +
        		", lowMemory = " + mi.lowMemory);
        return lAvailMem;
	}
	
	private long getSysTotalMemory() {
		String str1 = "/proc/meminfo";// 系统内存信息文件  
        String str2;  
        String[] arrayOfString;  
        long totalMemory = 0;  
  
        try {  
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);  
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小  
            if (DEBUG) Log.d(TAG, "str2 = " + str2);
            arrayOfString = str2.split("\\s+");  
            for (String num : arrayOfString) {  
                Log.i(str2, num + "\t");  
            }
            totalMemory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
            if (DEBUG) Log.v(TAG, "totalMemory = " + totalMemory);
            localBufferedReader.close();
        } catch (IOException e) {
        	Log.e(TAG, "IOException: " + e);
        }  
//        return Formatter.formatFileSize(getBaseContext(), initial_memory);// Byte转换为KB或者MB，内存大小规格化
        return totalMemory;
	}
	
	private long getSysUsedMemory() {
		long lUsedMem = getSysTotalMemory() - getSysAvailMemory();
		if (DEBUG) Log.d(TAG, "lUsedMem = " + lUsedMem);
		return Math.abs(lUsedMem);
	}
	
	private long getProcUsedMemory() {
		long iMem = 0;
		// 获得该进程占用的内存
		
		int[] myMempid = new int[] { mPid };
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		// 此MemoryInfo位于android.os.Debug.MemoryInfo包中，用来统计进程的内存信息
		Debug.MemoryInfo[] memoryInfo = am.getProcessMemoryInfo(myMempid);
		// 获取进程占内存用信息 kb单位
		iMem = memoryInfo[0].dalvikPrivateDirty;
		return iMem*1024;
	}
	
	/**
	 * 
	 * @param pkgName 程序包名
	 * @return 返回包名为pkgName的进程id
	 */
	private static int getPidForPackage(String pkgName) {
		if (DEBUG) Log.d(TAG, "Enter getPidForPackage(), pkgName = " + pkgName);
		ActivityManager am = AlpacaApp.getApplication().getActivityManager();
		List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
		int pid = 0;
		String[] packageList;
		for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList) {
			// 获得每个进程里运行的应用程序(包),即每个应用程序的包名
			packageList = appProcessInfo.pkgList;
			if (DEBUG) Log.v(TAG, "process id is " + appProcessInfo.pid + ", has " + packageList.length);
			for (String pkg : packageList) {
				Log.i(TAG, "packageName " + pkg + " in process id is --> " + appProcessInfo.pid);
				if (pkgName.equals(pkg)) {
					if (DEBUG) Log.d(TAG, "Got it! --> pid is " + appProcessInfo.pid);
					return appProcessInfo.pid;
				}
			}
		}
		
		return pid;
	}
	
	
	private void cpuShots() {
		try {
			mlnrProcStat = new LineNumberReader( new FileReader( "/proc/stat" ) );
			mStrProcStat = mlnrProcStat.readLine();
			if (DEBUG) Log.i(TAG, "mStrProcStat = " + mStrProcStat);
			
			mlnrProcPidStat = new LineNumberReader( new FileReader( "/proc/"+mPid+"/stat" ) );
			mStrProcPidStat = mlnrProcPidStat.readLine();
			if (DEBUG) Log.i(TAG, "mStrProcPidStat = " + mStrProcPidStat);
			
			mCpuList.add(mStrProcStat);
			mCpuList.add(mStrProcPidStat);
			
			mlnrProcStat.close();
			mlnrProcPidStat.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException : " + e);
		} catch (IOException e) {
			Log.e(TAG, "IOException : " + e);
		}
		mServiceHandler.sendEmptyMessageDelayed(EVENT_CPU_SNAPSHOT, 1000);
	}
	
//	private void memShots() {
//		long lSysUsedMem = getSysUsedMemory();
//		if (DEBUG) Log.i(TAG, "lSysUsedMem = " + lSysUsedMem);
//		long iProcUsedMem = getProcUsedMemory();
//		if (DEBUG) Log.d(TAG, "iProcUsedMem = " + iProcUsedMem);
//		
//		mMemList.add(lSysUsedMem);
//		mMemList.add(iProcUsedMem);
//		mServiceHandler.sendEmptyMessageDelayed(EVENT_MEM_SNAPSHOT, 1000);
//	}
	
	private void memShots() {
		long iProcUsedMem = getProcUsedMemory();
		if (DEBUG) Log.d(TAG, "iProcUsedMem = " + iProcUsedMem);
		
		mMemList.add(iProcUsedMem);
		mServiceHandler.sendEmptyMessageDelayed(EVENT_MEM_SNAPSHOT, 1000);
	}
	
	private String getNetworkType() {
		ConnectivityManager cManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cManager.getActiveNetworkInfo();
		if (DEBUG) Log.i(TAG, "networkInfo = " + networkInfo);
		if (DEBUG) Log.v(TAG, "getType() = " + networkInfo.getType() + ", getTypeName()=" + networkInfo.getTypeName());
		if (DEBUG) Log.v(TAG, "getSubtype() = " + networkInfo.getSubtype() + ", getSubtypeName()=" + networkInfo.getSubtypeName());
		
		String type = "";
		if (networkInfo.getType() == 1) { //Wifi
			type = "Wifi";
		}
		else {
			type = networkInfo.getSubtypeName();
		}
		return type;
	}
	
	public static String getNetType() {
		return mNetworkType;
	}
	
	public static AndroidSystemInfo getInstance() {
		return mAndroidSystemInfo;
	}
	
	public static ArrayList<String> getCpuList() {
		return mCpuList;
	}
	
	public static ArrayList<Long> getMemList() {
		return mMemList;
	}
	
	/**
	 * 获取手机的imei号（imei号是唯一识别手机的号码）
	 * @return
	 */
	public String getIMEI() {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String imei = tm.getDeviceId();
		if (DEBUG) Log.w(TAG, "imei = " + imei);
		return imei;
	}
	
	/**
	 * 获取android id号（android id 是手机系统的唯一号码）
	 * @return
	 */
	public String getAndroidID() {
		String androidID = android.provider.Settings.System.getString(getContentResolver(), "android_id"); 
	    if (DEBUG) Log.i(TAG, "androidID = " + androidID);
	    return androidID;
	}
	
	/**
	 * 得到Wifi的MAC地址
	 * @return
	 */
	public String getLocalMacAddress() {
	    WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    WifiInfo info = wifi.getConnectionInfo();
	    return info.getMacAddress();
  	}
	
	public String getOsBuild() {
		String phoneInfo = "Product: " + android.os.Build.PRODUCT;
        phoneInfo += ", CPU_ABI: " + android.os.Build.CPU_ABI;
        phoneInfo += ", TAGS: " + android.os.Build.TAGS;
        phoneInfo += ", VERSION_CODES.BASE: " + android.os.Build.VERSION_CODES.BASE;
        phoneInfo += ", MODEL: " + android.os.Build.MODEL;
        phoneInfo += ", SDK: " + android.os.Build.VERSION.SDK;
        phoneInfo += ", VERSION.RELEASE: " + android.os.Build.VERSION.RELEASE;
        phoneInfo += ", DEVICE: " + android.os.Build.DEVICE;
        phoneInfo += ", DISPLAY: " + android.os.Build.DISPLAY;
        phoneInfo += ", BRAND: " + android.os.Build.BRAND;
        phoneInfo += ", BOARD: " + android.os.Build.BOARD;
        phoneInfo += ", FINGERPRINT: " + android.os.Build.FINGERPRINT;
        phoneInfo += ", ID: " + android.os.Build.ID;
        phoneInfo += ", MANUFACTURER: " + android.os.Build.MANUFACTURER;
        phoneInfo += ", USER: " + android.os.Build.USER;
        if (DEBUG) Log.d(TAG, "phoneInfo = " + phoneInfo);
        return phoneInfo;
	}
	
	public static boolean start(String pkgName) {
    	if (DEBUG) Log.v(TAG, "Enter start(), pkgName = " + pkgName);
    	if (AlpacaUtil.isServiceWorked("us.nb9.androidinfo.service.AndroidSystemInfo")) {
    		if (DEBUG) Log.i(TAG, "AndroidSystemInfo Service is already running, so just return!");
    		return false;
    	}
    	
    	mPkgName = pkgName;
    	if (mPkgName == null) {
    		mPid = android.os.Process.myPid();
    	}
    	else {
    		mPid = getPidForPackage(mPkgName);
    	}
    	mCpuList = null;
    	mMemList = null;
    	
		Context context = AlpacaApp.getApplication();
		Intent intent = new Intent(context, AndroidSystemInfo.class);
		context.startService(intent);
		
		if (DEBUG) Log.v(TAG, "Leave start()");
		return true;
	}
	
	public static void stop() {
    	if (DEBUG) Log.v(TAG, "Enter stop()");
    	if (!AlpacaUtil.isServiceWorked("us.nb9.androidinfo.service.AndroidSystemInfo")) {
    		if (DEBUG) Log.d(TAG, "AndroidSystemInfo Service is not running, just return.");
    	}
    	mServiceHandler.removeMessages(EVENT_CPU_SNAPSHOT);
    	mServiceHandler.removeMessages(EVENT_MEM_SNAPSHOT);
    	mServiceHandler.sendEmptyMessage(EVENT_STOP_SESSION);
		if (DEBUG) Log.v(TAG, "Leave stop()");
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}