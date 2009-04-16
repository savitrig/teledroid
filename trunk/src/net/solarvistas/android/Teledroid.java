package net.solarvistas.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.EditText;

public class Teledroid extends Activity {
    TextView textView;
    EditText cmdInput;
    Connection shell;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        textView = (TextView) findViewById(R.id.view_result);
        cmdInput = (EditText) findViewById(R.id.command);

        //initConnection();
        ImageButton btnTest = (ImageButton) findViewById(R.id.btn_test);
        btnTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(shell == null)
            		initConnection();
            	textView.append("$"+cmdInput.getText().toString()+"\n");
                textView.append(shell.Exec(cmdInput.getText().toString()));
                cmdInput.setText("");
            }
        });
    }

    public void initConnection(){
    	shell = new Connection("cloud", "teledroid", "to.zxi.cc", 22);
    }


}
