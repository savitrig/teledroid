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

        shell = new Connection("cloud", "teledroid", "to.zxi.cc", 22);
        /*while (txt.length() < 1000) {
            txt += "\tChar: up to " + txt.length() + "\n";
        } */
        ImageButton btnTest = (ImageButton) findViewById(R.id.btn_test);
        btnTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                textView.setText(shell.Exec(cmdInput.getText().toString()));
                cmdInput.setText("");
            }
        });
    }




}
