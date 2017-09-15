package app.cap.beshop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.connection.DeviceConnection;
import com.estimote.sdk.connection.DeviceConnectionProvider;

import java.util.Locale;


public class SingleBeaconActivity  extends AppCompatActivity {
    private Beacon beacon;
    private DeviceConnectionProvider connection;
    private int power;
    private int interval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_beacon_screen);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editPref = preferences.edit();

        //Intent로 비콘 넘겨받기
        beacon = getIntent().getParcelableExtra("getBeacon");

        //비콘에 대한 읽기 / 쓰기 연결을 설정

        final int minorValue = beacon.getMinor();

        final EditText inputXPos = (EditText) findViewById(R.id.pos_x);
        final EditText inputYPos = (EditText) findViewById(R.id.pos_y);
        final EditText inputZPos = (EditText) findViewById(R.id.pos_z);

        //입력의 기본값을 현재 값으로 설정
        inputXPos.setText(Float.toString(preferences.getFloat("x" + minorValue, 0)), TextView.BufferType.EDITABLE);
        inputYPos.setText(Float.toString(preferences.getFloat("y" + minorValue, 0)), TextView.BufferType.EDITABLE);
        inputZPos.setText(Float.toString(preferences.getFloat("z" + minorValue, 0)), TextView.BufferType.EDITABLE);

        //EditTexts에 입력 된 값은 저장되어, 자동으로 목록으로 돌아감
        final Button saveValues = (Button) findViewById(R.id.confirm_pos);
        saveValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editPref.putFloat("x" + minorValue, Float.valueOf(inputXPos.getText().toString()));
                editPref.putFloat("y" + minorValue, Float.valueOf(inputYPos.getText().toString()));
                editPref.putFloat("z" + minorValue, Float.valueOf(inputZPos.getText().toString()));
                editPref.apply();

                returnToList();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void returnToList() {
        Intent intentSingleBeacon = new Intent(SingleBeaconActivity.this, BeaconListActivity.class);
        startActivity(intentSingleBeacon);
        finish();
    }

    //인증 또는 실패시 비콘 및 해당 이벤트에 대한 연결을 설정
    /*private DeviceConnectionProvider.ConnectionProviderCallback createConnectionCallback() {
        return new DeviceConnectionProvider.ConnectionProviderCallback() {
            @Override
            public void onConnectedToService() {
                Log.d("Connect", "Connected to device");
            }
            @Override
            public void onAuthenticated(final DeviceConnection connection) {
                connection.edit();

            }
        };
    }*/

}
