package us.nb9.androidinfo;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AlpacaApp extends Application {
    private static final String TAG = "AlpacaApp";
    private static final boolean DEBUG = AlpacaConfig.ALPACA_DEBUG&&true;

    public static final String ACTION_WAKE_UP = "us.nb9.androidinfo.WAKEUPME";
    private TelephonyManager mTelephonyManager;
    private ActivityManager mActivityManager;
    private static AlpacaApp sAlpacaApp = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.i(TAG, "Enter onCreate(), process id = " + android.os.Process.myPid());

        sAlpacaApp = this;

        try {
        	makeRoot();
        } catch(SecurityException e) {
            if (DEBUG) Log.i(TAG, "root SecurityException");
        }
        wakeUpEngine();
        AlpacaConfig.init();
    }
    
    private void wakeUpEngine() {
    	AlarmManager am = (AlarmManager) sAlpacaApp.getSystemService(Service.ALARM_SERVICE);
//        long mss = 3 * 1000 + System.currentTimeMillis();
    	long mss = 3 + System.currentTimeMillis();
        Intent ii = new Intent(sAlpacaApp, TimerReceiver.class);
        ii.setAction(ACTION_WAKE_UP);
        PendingIntent pii = PendingIntent.getBroadcast(sAlpacaApp, 0, ii, 0);
        am.set(AlarmManager.RTC_WAKEUP, mss, pii);
    }

    private void makeRoot() {
    	if(DEBUG) Log.i(TAG, "Enter makeRoot()");
    	
    	if(DEBUG) Log.i(TAG, "Leave makeRoot()");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (DEBUG) Log.i(TAG, "Enter onTerminate()");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DEBUG) Log.i(TAG, "Enter onConfigurationChanged(), newConfig = " + newConfig);
    }

    synchronized public static AlpacaApp getApplication() {
        return sAlpacaApp;
    }

    /**
     * @return Returns the TelephonyManager.
     */
    public TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager)sAlpacaApp.getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }
    
    
    public ActivityManager getActivityManager() {
        if (mActivityManager == null) {
        	mActivityManager = (ActivityManager)sAlpacaApp.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mActivityManager;
    }
}
