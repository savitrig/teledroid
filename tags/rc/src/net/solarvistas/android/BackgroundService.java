package net.solarvistas.android;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BackgroundService extends Service {

    private NotificationManager mNM;
    public static Thread scanFilesThread = null; //bcast thread
    public static Thread fileMonitorThread = null; //bcast thread
    public static Connection ssh;

    public enum SyncMode {SCAN, MONITOR, LAZY};
    public final static int SYNC_MODE_SCAN = 1;
    public final static int SYNC_MODE_MONITOR = 2;
    public final static int SYNC_MODE_LAZY = 3;
    public static int mSyncMode = SYNC_MODE_SCAN;
    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
    	
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        
        ScanFilesThread.stopSignal = false;
		scanFilesThread = new Thread(new ScanFilesThread(this), "Scan files thread");
		scanFilesThread.start();
		Log.d("teledroid.BackgroundService", "synchronization service started");
    }
    
	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
        ScanFilesThread.stopSignal = true;
		mNM.cancel(R.string.local_service_started);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped,
						Toast.LENGTH_SHORT).show();
		Log.d("teledroid.BackgroundService", "synchronization service stopped");
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

	public void restartService(){
		ScanFilesThread.stopSignal = true;
		Toast.makeText(this, R.string.local_service_stopped,
				Toast.LENGTH_SHORT).show();
		Log.d("teledroid.BackgroundService", "synchronization service stopped");
		
		//Todo: Notification and delay(PERIOD+)
		
		ScanFilesThread.stopSignal = false;
		scanFilesThread = new Thread(new ScanFilesThread(this));
		scanFilesThread.start();
		Log.d("teledroid.BackgroundService", "synchronization service started");
		
	}
	public void beginSyncNotification(List<SyncAction> syncActions) {
		int[] counts = syncCounts(syncActions);
		
		syncNotification("Syncing " + syncActions.size() + " files",
						 "Sync Started",
						 "Sending: " + counts[0] + " files\nReceiving: " + counts[1] + " files.");
	}

	
	public void finishedSyncNotification(List<SyncAction> syncActions) {
		int[] counts = syncCounts(syncActions);
		syncNotification("Finished syncing " + syncActions.size() + " files",
						 "Sync Finished",
						 "Sent: " + counts[0] + " files\nReceived: " + counts[1]+ " files");
	}
	
	public void syncInterruptedNotification(int successful, int unsuccessful) {
		syncNotification("Sync interrupted",
						 "Sync interrupted",
						 "Successful: " + successful + " files\n Remaining: " + unsuccessful + " files");
	}
	
	private int[] syncCounts(List<SyncAction> syncActions) {
		int sending = 0, receiving = 0;
		for (SyncAction action : syncActions)
			if (action.direction == ScanFilesThread.Direction.ClientToServer) sending++;
			else receiving++;
		return new int[] {sending, receiving};
	}
	
	private void syncNotification(String toolbarText, String title, String fullText) {
		Notification notification = new Notification(R.drawable.uponelevel, toolbarText, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, AndroidFileBrowser.class), 0);
		notification.setLatestEventInfo(this, title, fullText, contentIntent);
		mNM.notify(777, notification);
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
        mNM.notify(777, notification);
    }
}
