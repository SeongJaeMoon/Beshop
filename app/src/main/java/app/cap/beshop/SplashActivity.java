package app.cap.beshop;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {

    private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final String executeType = getIntent().getStringExtra("executeType");

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Intent intent = null;
                //if (executeType !=null && executeType.equals("beacon_find"))
                //{
                Intent intent = new Intent(SplashActivity.this, LogInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                //}
                //else
                //{
                //intent =new Intent(SplashActivity.this, popupActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                //startActivity(intent);
                //}
                finish();
                //}
            }
        }, 3000);
    }
}
