package net.solarvistas.android;

import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class BackgroundService extends Service {

    private NotificationManager mNM;
    public static Thread scanFilesThread = null; //bcast thread
    public static Thread fileMonitorThread = null; //bcast thread
	public Map<String, Object> mFilesMap = new LinkedHashMap<String, Object>();
    public static Connection ssh;

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        //fileMonitorThread = new Thread(new FileMonitorThread(this));
        //fileMonitorThread.start();
        ScanFilesThread.stopSignal = false;
		scanFilesThread = new Thread(new ScanFilesThread(this));
		scanFilesThread.start();
    }
    
	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
        ScanFilesThread.stopSignal = true;
		mNM.cancel(R.string.local_service_started);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped,
						Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();
	
	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		public BackgroundService getService() {
			return BackgroundService.this;
		}
	}

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, AndroidFileBrowser.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.local_service_started, notification);
    }
}
