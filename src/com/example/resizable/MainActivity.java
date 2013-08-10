package com.example.resizable;

import java.io.File;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setBackgroundColor(0x66ffffff);

        ResizableImageView rv = (ResizableImageView) findViewById(R.id.scalableImageView1);

        File sdcard = Environment.getExternalStorageDirectory();
        rv.setImageSource(Uri.fromFile(new File(sdcard, "chihya-gyu.png")));
        rv.setTextView(tv);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
