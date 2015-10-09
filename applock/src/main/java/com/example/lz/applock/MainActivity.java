package com.example.lz.applock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


import haibison.android.lockpattern.LockPatternActivity;
import haibison.android.lockpattern.SimpleLockPatternActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CREATE_PATTERN = 1;

    private Button create_btn;
    private Button verify_btn;
    private Button open_btn;
    char[] savedPattern;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        create_btn = (Button) findViewById(R.id.create_btn);
        create_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                LockPatternActivity.newIntentToCreatePattern(MainActivity.this);
                Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
                        MainActivity.this, LockPatternActivity.class);
                startActivityForResult(intent, REQ_CREATE_PATTERN);
            }
        });

        verify_btn = (Button) findViewById(R.id.verify_btn);
        savedPattern = new char[5];
        verify_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                LockPatternActivity.newIntentToVerifyCaptcha(MainActivity.this);
                Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                        MainActivity.this, LockPatternActivity.class);
                intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                startActivity(intent);
            }
        });

        open_btn = (Button) findViewById(R.id.open_btn);
        open_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SimpleLockPatternActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
