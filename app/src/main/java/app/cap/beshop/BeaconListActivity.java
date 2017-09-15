package app.cap.beshop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.cap.beshop.Adapter.BeaconAdapter;


public class BeaconListActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private BeaconManager beaconManager;
    private BeaconAdapter listViewAdapter;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beaon_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();

        listViewAdapter = new BeaconAdapter(this);
        ListView beaconListView = (ListView) findViewById(R.id.device_list);
        beaconListView.setAdapter(listViewAdapter);
        beaconListView.setClickable(true);
        Log.d("new intent", "success4");
        //beaocn의 값을 변경하는 다이얼로그를 설정
        beaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
                Intent intentSingleBeacon = new Intent(BeaconListActivity.this, SingleBeaconActivity.class);
                intentSingleBeacon.putExtra("getBeacon", listViewAdapter.getItem(position));
                startActivity(intentSingleBeacon);
            }
        });
        //비콘 매니저 설정

        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beaconList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //사용자와 거리를 기준으로 찾은 비콘 정렬

                        getSupportActionBar().setSubtitle("찾은 beacons: " + beaconList.size());
                        List<Beacon> sortedBeaconList = sortBeaconsOnMinor(beaconList);

                        // 새로운 비콘 찾을 시에 리스트뷰어뎁터 동기화
                        listViewAdapter.replaceWith(sortedBeaconList);
                    }
                });

            }
        });
    }

    private List<Beacon> sortBeaconsOnMinor(List<Beacon> beaconList) {
        Map<Integer, Beacon> map1 = new HashMap<Integer, Beacon>();
        List<Integer> minorValueList = new ArrayList<Integer>();
        for (int i = 0; i < beaconList.size(); i++) {
            minorValueList.add(beaconList.get(i).getMinor());
            map1.put(minorValueList.get(i), beaconList.get(i));
        }
        Collections.sort(minorValueList);
        List<Beacon> sortedBeaconList = new ArrayList<Beacon>();
        for(int i = 0; i< beaconList.size();i++){
            sortedBeaconList.add(map1.get(minorValueList.get(i)));
        }
        return sortedBeaconList;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 뒤로 가기 클릭시 홈으로!
                Intent intent = new Intent(this, MainActivity.class);
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                editPref.putBoolean("intent_stop",false);
                editPref.commit();
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onDestroy(){
        beaconManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onStart(){
        super.onStart();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        connectToService();
    }
    @Override
    protected void onStop(){
        Boolean intentStop = preferences.getBoolean("intent_stop",true);
        if(intentStop){
            try{
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            }catch (Exception e){}
        }
        editPref.remove("intent_stop");
        editPref.commit();
        super.onStop();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 블루투스를 요청
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else
                {
                getSupportActionBar().setSubtitle("블루투스가 사용 설정되지 않음");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
        // 비콘을 검색
        getSupportActionBar().setSubtitle("스캔중...");
        listViewAdapter.replaceWith(Collections.<Beacon>emptyList());
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                    getSupportActionBar().setSubtitle("\n" +
                            "범위 지정을 시작할 수 없습니다.");
                }
            }
        });
    }
}
