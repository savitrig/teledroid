package net.solarvistas.android;

import java.io.BufferedReader;
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
			getServerInfo(BackgroundService.ssh.Exec("dir-print sdcard\n"));
		} catch (Exception e) {
			Log.e("teledroid.SynThread.run", "Error getting dir-print info from server");
			e.printStackTrace();
			return;
		}
	}

    private void getServerInfo(Channel channel) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(channel
								.getInputStream()));
    	while (true) {
    		String msg = input.readLine();
			
			if (msg.contains("{")){
				Message m = new Message();
				m.what = ScanFilesThread.BUMP_MSG;
				m.obj = msg;
				scanFiles.getServerInfoHandler.sendMessage(m);
				channel.disconnect();
				return;
			}	  
    	}
       
    }
}
