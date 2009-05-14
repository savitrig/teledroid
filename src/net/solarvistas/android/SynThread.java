package net.solarvistas.android;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;

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
		mShell.channelSetup();
        mShell.Exec("dir-print sdcard\n");
        getServerInfo();
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
    
    private void getServerInfo() {
    	try {
            while (true) {
          	  try {
				  do {
					  String msg = mShell.fromServer.readLine();
					  //msgBuilder.append(msg);
					  //Log.d("Server", msg);
					  if (msg.contains("{")){
	                      Message m = new Message();
	                      m.what = ScanFilesThread.BUMP_MSG;
	                      m.obj = msg;
						  mSFT.getServerInfoHandler.sendMessage(m);
						  mShell.channel.disconnect();
						  return;
					  }	  
				  } while (mShell.fromServer.ready());
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
