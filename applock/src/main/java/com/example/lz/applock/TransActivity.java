package com.example.lz.applock;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by lz on 15/10/21.
 */
public class TransActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView view = new TextView(this);
        view.setText("nihao");

        setContentView(view);
    }
}
