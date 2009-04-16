package net.solarvistas.android;

import com.jcraft.jsch.*;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class Teledroid extends Activity
{
	TextView textView;
	String txt;
	 
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        textView = (TextView)findViewById(R.id.view_result);
        txt = "Result:\n";
        while(txt.length()<1000){
        	txt+="\tChar: up to " + txt.length()+"\n";
        }
        //textView.setText(txt);
        ImageButton btnTest = (ImageButton)findViewById(R.id.btn_test);
        btnTest.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {	
    			textView.setText(txt); 
    		}});
    }
    
    public void Shell(){
        
        try{
          JSch jsch=new JSch();

          //jsch.setKnownHosts("/home/xzhang19/.ssh/known_hosts");
    
          Session session=jsch.getSession("cloud", "to.zxi.cc", 22);

          session.setPassword("teledroid");

          session.setConfig("StrictHostKeyChecking", "no");

          session.connect(30000);   // making a connection with timeout.

          Channel channel=session.openChannel("exec");
          
          channel.setInputStream(System.in);
          
          channel.setOutputStream(System.out);

          channel.connect(3*1000);
          
        }
        catch(Exception e){
          System.out.println(e);
        }
    }
}
