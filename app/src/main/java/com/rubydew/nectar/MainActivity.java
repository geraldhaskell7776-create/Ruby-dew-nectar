package com.rubydew.nectar;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText("Ruby Dew Nectar");
        text.setTextSize(32);
        text.setTextColor(Color.parseColor("#9B111E"));
        text.setGravity(Gravity.CENTER);
        text.setBackgroundColor(Color.WHITE);

        setContentView(text);
    }
}
