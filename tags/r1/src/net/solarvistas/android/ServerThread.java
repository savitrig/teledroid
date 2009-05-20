package net.solarvistas.android;

import java.io.IOException;
import android.os.Message;

public class ServerThread implements Runnable {
    private static Teledroid mT;
    private static Connection mShell;
    
    public ServerThread(Teledroid t, Connection shell) {
    	mT = t;
    	mShell = shell;
    }
    
    public void run() {
         try {
             while (true) {
            	  try {
        			  String msg;

        			  do {
        				  msg = mShell.fromServer.readLine() + "\n";
	                      Message m = new Message();
	                      m.what = Teledroid.BUMP_MSG;
	                      m.obj = msg;
	                      mT.myViewUpdateHandler.sendMessage(m);
        				  if (msg.equals("logout\n"))
            				  return;
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
