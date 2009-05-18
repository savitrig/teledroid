package net.solarvistas.android;

import java.io.IOException;
import java.io.InputStreamReader;
import android.os.Message;
import android.util.Log;

import com.jcraft.jsch.Channel;

public class SynThread implements Runnable {
    ScanFilesThread scanFiles;
    
    public SynThread(ScanFilesThread sft) {
    	this.scanFiles = sft;
    }
    
	public void run() {
        //mMSG = mShell.ExecCommand("dir-print teledroid_test");
        //Log.d("Server", mMSG);
		
		try {
			getServerInfo(BackgroundService.ssh.Exec("cat a.txt\n"));
		} catch (Exception e) {
			Log.e("teledroid.SynThread.run", "Error getting dir-print info from server");
			e.printStackTrace();
			return;
		}
	}

    private void getServerInfo(Channel channel) throws IOException {
        Log.d("teledroid.SynThread.getServerInfo", "reading for JSON object from dir-print on " + channel);
    	InputStreamReader input = new InputStreamReader(channel.getInputStream());
    	final int BUFSIZE = 4 * 1028;
    	char[] buffer = new char[BUFSIZE];
    	StringBuilder sb = new StringBuilder();
    	while(true) {
    		int amount = input.read(buffer, 0, BUFSIZE);
    		if (amount == -1)
    			break;
    		Log.v("teledroid.SynThread.getServerInfo", "read from channel: " + buffer.toString());
    		sb.append(buffer, 0, amount);
    	}
    	String msg = sb.toString();

    	Log.d("teledroid.SynThread.getServerInfo","JSON found " + msg);
		Message m = new Message();
		m.what = ScanFilesThread.BUMP_MSG;
		m.obj = msg;
		scanFiles.getServerInfoHandler.sendMessage(m);
		channel.disconnect();
    }
}
