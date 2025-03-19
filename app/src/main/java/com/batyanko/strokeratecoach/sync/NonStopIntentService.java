/*
 * The code below was taken from StackOverflow user Graeme
 * (https://stackoverflow.com/users/726954/graeme)
 * as an answer to a question
 * (https://stackoverflow.com/questions/6841212/can-an-intentservice-run-indefinitely)
 * by StackOverflow user Pathy
 * (https://stackoverflow.com/users/864937/pathy)
 * The code is licensed under CC BY-SA 3.0 ( http://creativecommons.org/licenses/by-sa/3.0/ ).
 *
 * Minor modifications: 2017 Yanko Georgiev
 */

package com.batyanko.strokeratecoach.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

public abstract class NonStopIntentService extends Service {

    private String mName;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    public NonStopIntentService(String name) {
        super();
        mName = name;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    protected abstract void onHandleIntent(Intent intent);

}