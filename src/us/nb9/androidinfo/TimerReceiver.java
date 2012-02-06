package us.nb9.androidinfo;

import us.nb9.androidinfo.service.AndroidSystemInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class TimerReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerReceiver";
    private static final boolean DEBUG = AlpacaConfig.ALPACA_DEBUG&&true;
    
    static final Object mStartingServiceSync = new Object();
    static PowerManager.WakeLock mStartingServiceLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "Enter onReceive(), context = " + context + "intent = " + intent);
        if (intent == null) return;
        String sAction = intent.getAction();
        if (sAction != null) {
        	if (DEBUG) Log.d(TAG, "sAction = " + sAction);
        	if (sAction.equals(AlpacaApp.ACTION_WAKE_UP)) {
        		Intent intent1 = new Intent(context, AndroidSystemInfo.class);
                intent1.putExtra("init", "init");
                intent1.setAction(sAction);
                beginStartingService(context, intent1);
            }
        }
    }

    /**
     * Acquiring the wake lock before returning to ensure that the service will run.
     */
    public static void beginStartingService(Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "Enter beginStartingService()");
        synchronized (mStartingServiceSync) {
            if (mStartingServiceLock == null) {
                PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                mStartingServiceLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        					"StartingEngineService2_SmsReceiver");
                mStartingServiceLock.setReferenceCounted(false);
            }
            mStartingServiceLock.acquire();
            context.startService(intent);
        }
        if (DEBUG) Log.d(TAG, "Leave beginStartingService()");
    }
    
    /**
     * Called back by the service when it has finished working,
     * releasing the wake lock then phone can go into sleep.
     */
    public static void finishStartingService(Service service, int startId) {
        if (DEBUG) Log.d(TAG, "Enter finishStartingService(), mStartingServiceLock="+mStartingServiceLock+", startId="+startId);
        synchronized (mStartingServiceSync) {
            if (mStartingServiceLock != null) {
                //if (service.stopSelfResult(startId)) {
                if (true) {
                	mStartingServiceLock.release();
                }
            }
        }
        if (DEBUG) Log.d(TAG, "Leave finishStartingService()");
    }
    
    private void startEngine(Context context) {
        Intent intent = new Intent(context, AndroidSystemInfo.class);
        intent.putExtra("init", "init");
        context.startService(intent);
    }
}
