package net.solarvistas.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Message;

import com.jcraft.jsch.Channel;

public class SynThread implements Runnable {

    Connection mShell;
    String mServerInfo;
    StringBuilder msgBuilder = new StringBuilder();
    String mMSG;
    ScanFilesThread mSFT;
    
    public SynThread(ScanFilesThread sft) {
        this.mSFT = sft;
    }
    
        public void run() {
        //initConnection();
        //mMSG = mShell.ExecCommand("dir-print teledroid_test");
        //Log.d("Server", mMSG);
        mShell = BackgroundService.ssh;
        //Channel c = mShell.channelSetup();
        try {
			getServerInfo(mShell.Exec("dir-print sdcard\n"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //Log.d("Server", msgBuilder.toString());
        }

    private void initConnection(){
        if(mShell == null) {
                mShell = new Connection("teledroid", "Lmssf6R6", "teledroid.rictic.com", 22);
                mShell.connect();
                //mShell = mSFT.mShell;
                mShell.channelSetup();
        }  
    }
    
    private void getServerInfo(Channel c) throws Exception {
    	BufferedReader input = new BufferedReader(new InputStreamReader(c.getInputStream()));
        try {
            while (true) {
                  try {
                      do {
                              String msg = input.readLine();
                              //msgBuilder.append(msg);
                              //Log.d("Server", msg);
                              if (msg.contains("{")){
                              Message m = new Message();
                              m.what = ScanFilesThread.BUMP_MSG;
                              m.obj = msg;
                              mSFT.getServerInfoHandler.sendMessage(m);
                              c.disconnect();
                              return;
	                      }       
	              } while (input.ready());
	              //ServerSocketTest.text.setText((CharSequence) mMsg);  
              } catch(IOException ioException){
                        ioException.printStackTrace();
              } finally {
              }
            }
        } catch (Exception e) {
                        e.printStackTrace();
        }
    }
}