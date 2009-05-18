package net.solarvistas.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;


public class FileMonitorThread implements Runnable {

    //Map<String, Long> mFilesMap;
    BackgroundService bs;
    
    Process mPNotify;
	int mRunningStatus;
    String mTarget = "/sdcard/teledroid";
    List<String> mFileChangeList = new ArrayList<String>();

	final String inotify = "/data/inotify ";//data/net.ssolarvistas.android/libs/";
    final ArrayList<String> mask = new ArrayList<String>();

    final int TD_FM_RUNNING = 1;
    final int TD_FM_STOPED = 0;
    final int TD_FM_TERMINATING = -1;
    final int TD_FM_RESTARTING = 2;

    public FileMonitorThread(BackgroundService bs) {
    	this.bs = bs;
        InitiateMonitoredMaskList();
    }

    public void run() {
    	try {
    		String status;
    		BufferedReader input = invokeNotify(mTarget);
            mRunningStatus = TD_FM_RUNNING;
    		while(true){
				if ((status = input.readLine()) != null)
                    if(status.startsWith("[i]"))
                        ProcessINotifyEvent(status);

                switch(mRunningStatus){
                    case TD_FM_RUNNING:
                        break;
                    case TD_FM_RESTARTING:
                        input = invokeNotify(mTarget);
                        mRunningStatus = TD_FM_RUNNING;
                        break;
                    case TD_FM_TERMINATING:
                        mPNotify.destroy();
                        mRunningStatus = TD_FM_STOPED;
                    default:
                        throw new Exception("inotify process stoped.");
                }
    		}
    	} catch (Exception e) {
			Log.d("Teledroid.Monitor", "IOException", e);
    	}
    }

    private void InitiateMonitoredMaskList(){
        mask.add("CREATE");
        mask.add("CLOSE_WRITE");
        mask.add("ATTRIB");
        mask.add("DETELE");
        mask.add("DELETE_SELF");
        mask.add("MODIFY");
        mask.add("MOVE_SELF");
        mask.add("MOVED_FROM");
        mask.add("MOVED_TO");
    }
    
    private void ProcessINotifyEvent(String input) {
        String file, event;
        file = input.substring(3).split(", ")[0];
        event = input.substring(3).split(", ")[1];
        if(mask.contains(event)) {
            if(!mFileChangeList.contains(file)) {
                mFileChangeList.add(file);
                Log.d("teledroid", "added " + file + "into list.");
            }
        }
        Log.d("teledroid", input);
    }

    public void setMRunningStatus(int mRunningStatus) {
        this.mRunningStatus = mRunningStatus;
    }

    public List<String> getMFileChangeList() {
        return mFileChangeList;
    }

    public int getMRunningStatus() {
        return mRunningStatus;
    }

    private BufferedReader invokeNotify(String target) throws Exception {
    	String command = inotify + target;
		mPNotify = Runtime.getRuntime().exec(command);
		Log.d("teledroid", ">> FileMonitor Initiated Process $" + command);
		return new BufferedReader(new InputStreamReader(mPNotify.getInputStream()));
    }

    public void Terminate(){
        setMRunningStatus(TD_FM_TERMINATING);
    }
}
