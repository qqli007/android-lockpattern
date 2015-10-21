package com.example.lz.applock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

import haibison.android.lockpattern.LockPatternActivity;
import haibison.android.lockpattern.SimpleLockPatternActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CREATE_PATTERN = 1;

    private Button create_btn;
    private Button verify_btn;
    private Button open_btn;
    private Button open_fe_btn;
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
                Intent intent = new Intent(MainActivity.this, TransActivity.class);
                startActivity(intent);
            }
        });

        open_fe_btn = (Button) findViewById(R.id.open_fe_btn);
        open_fe_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.xunlei.fileexplorer.lockpattern");
                intent.putExtra("app_id", "shou_lei");
                intent.putExtra("action_3rd", "action_open_private_folder");
                startActivity(intent);
            }
        });

        final EditText text1 = (EditText) findViewById(R.id.input_1);
        final EditText text2 = (EditText) findViewById(R.id.input_2);

        Button encrypt = (Button) findViewById(R.id.encrypt_btn);
        encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String temp1 = text1.getText().toString();
                String temp2 = text2.getText().toString();

                ArrayList<String> data = new ArrayList<String>();
                if (!TextUtils.isEmpty(temp1)) {
                    data.add(temp1);
                }

                if (!TextUtils.isEmpty(temp2)) {
                    data.add(temp2);
                }

                Intent intent = new Intent("com.xunlei.fileexplorer.lockpattern");
                intent.putExtra("app_id", "shou_lei");
                intent.putExtra("action_3rd", "action_encrypt");
                intent.putStringArrayListExtra("extra_encrypt_paths", data);
                startActivityForResult(intent, 5);
            }
        });

        Button big = (Button) findViewById(R.id.big_file_btn);
        big.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("xunlei.intent.action.FILE_CLEANUP");
                intent.putExtra("app_id", "shou_lei");
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 5) {
            if (resultCode == RESULT_OK) {
                List<String> result =  data.getStringArrayListExtra("extra_encrypt_paths");
                Log.d("0-0", "result=" + result);
                if (result == null || result.size() <= 0) {
                    Toast.makeText(this, "加密成功", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "加密失败 " + result.get(0), Toast.LENGTH_LONG).show();
                }
            }
        }
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
