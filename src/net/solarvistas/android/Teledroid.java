package net.solarvistas.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Teledroid extends Activity {
    TextView textView;
    EditText cmdInput;
    Connection shell;
    LinearLayout linearView;
    ScrollView scrollView;
    Thread mThread;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        scrollView = (ScrollView)findViewById(R.id.view_canvas);
        linearView = (LinearLayout)findViewById(R.id.layout);
        textView = (TextView) findViewById(R.id.view_result);
        cmdInput = (EditText) findViewById(R.id.command);

        initConnection();
        ImageButton btnTest = (ImageButton) findViewById(R.id.btn_test);
        btnTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	String msg = cmdInput.getText().toString();
				/*if (msg.contains("exit"))
					mThread.destroy(); // Need optimization.*/
                shell.Exec(msg+"\n");
                cmdInput.setText("");
            }
        });
    }

    public void initConnection(){
    	if(shell == null) {
        	shell = new Connection("cloud", "teledroid", "to.zxi.cc", 22);
    		shell.channelSetup();
    		mThread = new Thread(new ServerThread(this, shell));
    		mThread.start();
    	}  
    }

    public static final int BUMP_MSG = 1;

    public Handler myViewUpdateHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case BUMP_MSG:
              	  	viewUpdated((String)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
    
    public void viewUpdated(String s) {
    	textView.append(s);
        scrollView.scrollTo(0, linearView.getMeasuredHeight()-scrollView.getHeight());
    }
}
