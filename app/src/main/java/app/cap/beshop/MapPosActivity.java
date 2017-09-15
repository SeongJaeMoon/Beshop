package app.cap.beshop;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.Utils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


import app.cap.beshop.payment.compress;

public class MapPosActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;


    //Bluetooth and Beacon Constant.
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private static final String TAG ="MapPosActvitiy";

    private BeaconManager beaconManager;

    private Bitmap workingBitmap;

    private float mapXSize;
    private float mapYSize;
    //관측 거리 계산 모드
    //0: real distance, 1: Average, 2: Median
    public int Mode = 0;
    //관측 거리 계산 모드
    //0: Weight Average, 1: Pythagoras & Eucludian
    public int ModeIndoorAlgorithm = 1;
    public String detect = "no";
    // 지도 / 그림의 크기 (픽셀 단위)

    private int widthPixels;
    private int heightPixels;
    private Boolean reset = false;
    private ImageView mapImage;

    private float[] userPos = new float[]{0, 0, 0};
    private int measurements = 0;

    final ArrayList<Integer> minorValues = new ArrayList<Integer>();
    private HashMap<Integer, ArrayList<Double>> distances = new HashMap<Integer, ArrayList<Double>>();
    private HashMap<Integer, Double> distanceAvg = new HashMap<Integer, Double>();
    private HashMap<Integer, float[]> beaconDist = new HashMap<Integer, float[]>();
    public  Button paymentButton;
    public EditText realPosition;
    public ProgressBar progress_timer;
    public MyCountDownTimer myCountDownTimer;
    public CheckBox checkbox_automatic;
    public Boolean automatic_payment = false;
    public Boolean payStart = false;
    private Boolean recordExperiment = false;

    public FirebaseDatabase database = FirebaseDatabase.getInstance();
    public DatabaseReference myRef = database.getReference();

    @Override
    protected  void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_position);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();
        //firebase
        progress_timer = (ProgressBar)findViewById(R.id.progress_timer);
        checkbox_automatic = (CheckBox) findViewById(R.id.checkbox_automatic);
        realPosition = (EditText) findViewById(R.id.realPosition);
        //미터의지도 크기를 가져옴
        mapXSize = preferences.getFloat("map_x",10);
        mapYSize = preferences.getFloat("map_y",10);

        //지도 위에 지도 크기를 표시
        final TextView mapInfo = (TextView) findViewById(R.id.map_size);
        //mapInfo.setText("\n The Map Size(x,y): \t" + mapXSize +", " + mapYSize +"\n");

        mapImage = (ImageView) findViewById(R.id.map_picture);
        mapImage.setAdjustViewBounds(true);

        //지도의 크기가 조절 된 상태를 ImageView에 설정
        final Bitmap imageRaw = BitmapFactory.decodeResource(getResources(), R.drawable.raster);
        getScreenSizes(imageRaw);
        Bitmap imageScaled = Bitmap.createScaledBitmap(imageRaw, widthPixels, heightPixels, true);
        mapImage.setImageBitmap(imageScaled);

        workingBitmap = Bitmap.createScaledBitmap(imageRaw, widthPixels, heightPixels, true);


        //스캐닝 방식에서 거리와 측정기를 기본값으로 재설정
        final Button resetButton = (Button) findViewById(R.id.reset_pos);
        paymentButton = (Button) findViewById(R.id.btn_payment);

        paymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent paymentIntent = new Intent(MapPosActivity.this, PaymentProcess.class);

                try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                }
                editPref.putBoolean("intent_stop", false);
                editPref.apply();
                startActivity(paymentIntent);

            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset = true;
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset = true;
            }
        });
        final Button changeMapSettings = (Button) findViewById(R.id.map_settings);
        changeMapSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //대화 상자에서 다중 인풋을 위해 레이아웃 인플레이터가 필요
                LayoutInflater dialogInflater = LayoutInflater.from(MapPosActivity.this);

                //어댑터에서 비콘의 minor를 검색
                final View textEntryView = dialogInflater.inflate(R.layout.setting_dialog_map, null);

                final EditText inputXPos = (EditText) textEntryView.findViewById(R.id.pos_x);
                final EditText inputYPos = (EditText) textEntryView.findViewById(R.id.pos_y);

                //입력의 기본값을 현재 값으로 설정
                inputXPos.setText(Float.toString(preferences.getFloat("map_x", 10)), TextView.BufferType.EDITABLE);
                inputYPos.setText(Float.toString(preferences.getFloat("map_y", 10)), TextView.BufferType.EDITABLE);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MapPosActivity.this);
                dialogBuilder.setTitle("설정 변경 : ");
                dialogBuilder.setView(textEntryView);

                //확인 및 취소 버튼 설정
                dialogBuilder.setPositiveButton("완료", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //변경된 x, y 위치 값 저장
                        mapXSize = Float.valueOf(inputXPos.getText().toString());
                        mapYSize = Float.valueOf(inputYPos.getText().toString());
                        editPref.putFloat("map_x", mapXSize);
                        editPref.putFloat("map_y", mapYSize);
                        editPref.commit();
                    }
                });
                dialogBuilder.setNegativeButton("닫기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                dialogBuilder.show();

            }
        });



        //적은 소수점 이하의 거리를 나타 내기 위해서 사용
        final DecimalFormat df = new DecimalFormat("#0.####");

        //파란색 paintBlue에 비콘 표시
        final Paint paintBlue = new Paint();
        paintBlue.setAntiAlias(true);
        paintBlue.setColor(Color.BLUE);
        paintBlue.setAlpha(125);

        final Paint paintRed = new Paint();
        paintRed.setAntiAlias(true);
        paintRed.setColor(Color.RED);
        paintRed.setAlpha(125);
        //비콘매니저 설정


        beaconManager = new BeaconManager(this);
        beaconManager.setForegroundScanPeriod(100,0);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {

            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beaconList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        getSupportActionBar().setSubtitle("찾은 Beacons : " + beaconList.size());
                        mapInfo.setText("맵 크기(x, y): \t " +mapXSize+ ", "+mapYSize + "\n");

                        //버튼을 눌렀을 때 모든 것을 기본값으로 재설정
                        if(reset){
                           resetDistance();
                        }


                        //Canvas에 clean sheet를 사용하기 위해 woringBitmap을 복사
                        final Bitmap mutableBitmap= workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        final Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        // canvas.drawPaint(paint);
                        paint.setColor(Color.BLACK);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextSize(paint.getTextSize() * 8);
                        //각 비콘의 비콘 표시
                        //canvas.drawText("0,0", 2,2, paint);
                        String[] Distancex;
                        int counter = 0;
                        for (int i = 0; i < beaconList.size();i++){
                            Beacon currentBeacon = beaconList.get(i);
                            int minorVal = currentBeacon.getMinor();
                            if(minorVal < 5){
                                counter++;
                            }
                        }
                        Distancex = new String[counter];
                        counter = 0;
                        for (int i = 0; i < beaconList.size();i++){

                            Beacon currentBeacon = beaconList.get(i);
                            int minorVal = currentBeacon.getMinor();
                            if(minorVal < 5){
                            //임의설정값 을 얻고 환경 설정에 넣음

                            if(preferences.getString("secretBeacon"+String.valueOf(minorVal),"").equals("")){
                                getSecretBeacon(String.valueOf(minorVal));
                            }

                            //새로운 비콘이 나타나지 않거나 유효하지 않은 비콘이 표시되는 것을 방지
                           // if(preferences.getFloat("x" + minorVal, -1) < 0){
                              //  continue;
                           // }

                            //비콘이 발견되었는지 확인 if문은 재설정 버튼 때문에 실행
                            if(!minorValues.contains(minorVal)){
                                minorValues.add(minorVal);
                            }
                            if(!distances.containsKey(minorVal)){
                                distances.put(minorVal, new ArrayList<Double>());
                            }
                            addDistance(currentBeacon, minorVal);

                            //그려진 원으로 그림을 만들기위한 사전 정의 된 변수
                                float[] locationBeacon = getLocation(currentBeacon);
                            //비콘 위치에 원을 그리기
                                canvas.drawText(String.valueOf(currentBeacon.getMinor()), locationBeacon[0], locationBeacon[1], paint); //
                                canvas.drawCircle(locationBeacon[0], locationBeacon[1], (float) 0.02* widthPixels, paintBlue);
                                Distancex[counter] = "비콘 거리 "+String.valueOf(minorVal)+" 고객 디바이스 " + String.valueOf(distanceAvg.get(minorVal).floatValue())+"\n";
                                counter++;
                            }

                            }

                            if( Distancex.length>1){

                        Arrays.sort(Distancex);
                        for( int ix = 0; ix < Distancex.length;ix++) {
                            mapInfo.append(Distancex[ix]);
                        }
                            }
                        if(beaconList.size()>2){
                            if(ModeIndoorAlgorithm == 0){
                        userPos = userLocation(beaconList);
                                //record
                            }else{
                                userPos = userLocationNew(beaconList);
                            }
                        mapInfo.append("사용자 위치 (x, y) " + userPos[0] + ", " + userPos[1] + "\n" + "실내 POS 위치 : " +
                                detect +"\n");
                        Log.d("paint","true");
                        canvas.drawCircle(widthPixels *(userPos[0]/mapXSize), heightPixels * (userPos[1]/mapYSize),
                                (float) 0.02 * widthPixels, paintRed);
                        }
                        if(measurements > 20){
                            recordPosition(userPos[0], userPos[1]);
                            measurements = 0;
                        }
                        measurements+=1;
                        //at least 40 measurements before calculating the user position.
//                        if(measurements > 39){
//
//                            //every second a new position measurement
//                            int temp = measurements;
//                            while (temp > 9) {
//
//                                temp -= 10;
//                            }
//
//                            if(temp == 0){
//                                Log.d("tag",String.valueOf(beaconList.size()));
//                                userPos = userLocationNew(beaconList);
//
//                            }
//
//                            mapInfo.append("User Position (x, y) " + userPos[0] + ", " + userPos[1] + "\n" + "Maximum error: " +
//                            userPos[2] +"\n");
//                            Log.d("paint","true");
//                            canvas.drawCircle(widthPixels *(userPos[0]/mapXSize), heightPixels * (userPos[1]/mapYSize),
//                                    (float) 0.03 * widthPixels, paintRed);
//
//                        } else {
//                            mapInfo.append("Time until measurement: " + df.format(4 - measurements * 0.1));
//                        }
//                        measurements +=1;
                        mapImage.setImageBitmap(mutableBitmap);
                    }
                });
            }
        });

    }
    private float[] userLocationNew(List<Beacon> beaconList){
        //get 2 beacon
        int type = 0;
        for(int i =0; i< beaconList.size();i++){
            System.out.println(beaconList.size());
            int minorVal = beaconList.get(i).getMinor();

            if(minorVal < 5){
            type+=minorVal;
            float r;
            try {

                r = distanceAvg.get(minorVal).floatValue();
                Log.d("distx",String.valueOf(r));
            } catch (NullPointerException e) {
                continue;
            }
            float x = preferences.getFloat("x" + minorVal, -1);
            float y = preferences.getFloat("y" + minorVal, 0);
            float z = preferences.getFloat("z" + minorVal, 0);
            if (x >= 0 && y >= 0) {
                // Remove the height difference between the phone and beacon from the distance.
                r = (float) Math.sqrt((r * r) - (z * z));
               // if(!beaconDist.containsKey(minorVal)){

                    beaconDist.put(minorVal, new float[]{x, y, r});
                System.out.println(beaconDist.size());
                //}
            }
            }

        }
        //관찰 된 모든 비콘, 위치 x, y를 반복적으로 표시하고 비콘에서 장치로부터의 거리를 테이블로 저장
        //삼각형 beaconObs[1]과 beaconObs[2]를 찾기 위해 2 개의 관찰 된 비콘을 사용
        if(beaconDist.size()>=3){
            int minor_beacon_1 = 1;
            int minor_beacon_2 = 2;
            int minor_beacon_3 = 3;

            //type == 6 : 1, 2, 3
            //type == 7 : 1, 2, 4
            //type == 8 : 1, 3, 4
            //type == 9 : 2, 3, 4
            System.out.print("타입 = "+type);
        if(type == 6){
            System.out.print("수: 6");
            minor_beacon_1 = 1;
            minor_beacon_2 = 2;
            minor_beacon_3 = 3;
        }else if(type == 7){
            minor_beacon_1 = 1;
            minor_beacon_2 = 2;
            minor_beacon_3 = 4;
        }else if(type == 8) {
            minor_beacon_1 = 3;
            minor_beacon_2 = 4;
            minor_beacon_3 = 1;
        }else if (type == 9){
            minor_beacon_1 = 3;
            minor_beacon_2 = 4;
            minor_beacon_3 = 2;
        }else  if(type == 10){
            minor_beacon_1 = 1;
            minor_beacon_2 = 2;
            minor_beacon_3 = 3;
        }


        //삼각형
        System.out.println(beaconDist);
        float dist_b1_b2 = (float)eucludianDistance(beaconDist.get(minor_beacon_1)[0]
                ,beaconDist.get(minor_beacon_2)[0],beaconDist.get(minor_beacon_1)[1]
                ,beaconDist.get(minor_beacon_2)[1]);

        float dist_b1_dev = beaconDist.get(minor_beacon_1)[2];
        float dist_b2_dev = beaconDist.get(minor_beacon_2)[2];
        float dist_b3_dev = beaconDist.get(minor_beacon_3)[2];
        //demo
        //dist_b1_b2 = 5;
        //dist_b1_dev = 3;
        //dist_b2_dev  =4;
        //dist_b3_dev = 3;

        double d_x = ((Math.abs(Math.pow(dist_b1_dev,2)-Math.pow(dist_b2_dev,2)))-Math.pow(dist_b1_b2,2)) / (-2*dist_b1_b2);

        double d_y = Math.sqrt(Math.abs((Math.pow(dist_b1_dev,2)-Math.pow(d_x,2))));
        //c_x =(abs(d1^2-d2^2)-dist_b1_b2^2) / (-2 * dist_b1_b2)
        //c_y = sqrt(d1^2-c_x^2)
        //지불 영역이 내부인지 아닌지를 알면, 3 개의 비콘을 사용하여 지불 영역이 정사각형이라고 가정
        //b1************
        //*            *
        //*            *
        //*            *
        //b2**********b3

        d_x+=beaconDist.get(minor_beacon_1)[0];
        d_y+=beaconDist.get(minor_beacon_1)[1];
        Log.d("Coordinate: X: ", String.valueOf(d_x) +"|Y: "+String.valueOf(d_y));

        double dist_b3_positive = eucludianDistance((float)d_x,(float)d_y,beaconDist.get(minor_beacon_3)[0],beaconDist.get(minor_beacon_3)[1]);
        double dist_b3_negative = eucludianDistance((float)d_x,(float)d_y*-1 + 2*beaconDist.get(minor_beacon_1)[1],beaconDist.get(minor_beacon_3)[0],beaconDist.get(minor_beacon_3)[1]);
        Log.d("Coordinate: Positive: ", String.valueOf(dist_b3_positive) +"|Negative: "+String.valueOf(dist_b3_negative));
        //Log.d("Coordinate: PositiveDiff: ", Math.abs(dist_b3_negative-dist_b3_dev) +"|NegativeDiff: "+Math.abs(dist_b3_positive -dist_b3_dev));
            if(Math.abs(dist_b3_negative-dist_b3_dev) < Math.abs(dist_b3_positive -dist_b3_dev)){

                d_y = d_y*-1+2*beaconDist.get(minor_beacon_1)[1];
            }

            //d_y = d_y*-1+2*beaconDist.get(minor_beacon_1)[1];
            //d_y = d_y-beaconDist.get(minor_beacon_1)[1];
            if(d_x>=preferences.getFloat("x1",-1) && d_x <= preferences.getFloat("x2",-1) && d_y >=preferences.getFloat("y1",-1)&& d_y <=preferences.getFloat("y3",-1)){
                detect = "yes";
                paymentButton.setEnabled(true);
                startPayment();
        }
        else
            {
            detect = "no";
                detect = "no";
                paymentButton.setEnabled(false);
                if(payStart) {
                    myCountDownTimer.cancel();
                }
                progress_timer.setProgress(100);
                payStart = false;
            paymentButton.setEnabled(false);
        }


        //결과 x가 b2x보다 크고, b3x보다 작고, y가 크고, b1y보다 크고, b2y보다 작은 지 확인
        //if(d_x >= beaconDist.get(minor_beacon_1)[0] && d_x <= )

        return new float[]{(float)d_x, (float)d_y, 1};
        }
        return new float[]{(float)0, (float)0, 1};
    }
//    public float[] findLocationTriangle(){
//
//    }
    private float[] userLocation(List<Beacon> beaconsList) {
        float x_user = 0;
        float y_user = 0;
        float r_user = 1;

        //정확한 비콘, 해당 지역에서 엑세스 된 사람들을 확인
        ArrayList<float[]> circleArray = new ArrayList<float[]>();
        for (int i = 0; i < beaconsList.size(); i++) {

            int minorVal = beaconsList.get(i).getMinor();
            if(minorVal < 5){
            float r;
            try {
                Log.d("minorVal: "+ String.valueOf(minorVal),String.valueOf(distanceAvg.size()));
                r = distanceAvg.get(minorVal).floatValue();

            } catch (NullPointerException e) {
                continue;
            }

            float x = preferences.getFloat("x" + minorVal, -1);
            float y = preferences.getFloat("y" + minorVal, 0);
            float z = preferences.getFloat("z" + minorVal, 0);

            if (x >= 0 && y >= 0) {
                //거리에서 개인 디바이스와 신호 사이의 높이 차이를 제거
                r = (float) Math.sqrt((r * r) - (z * z)); //거리 값
                circleArray.add(new float[]{x, y, r});
                x_user += x;
                y_user += y;
            }
            }
        }

        //2 개 이상의 비콘을 사용할 수있는 경우에만 위치를 계산
        float circleNumber = circleArray.size();
        if (circleNumber < 2) {
            return new float[]{x_user, y_user, r_user};
        }

        //유효한 모든 서클 간의 평균 위치
        x_user = x_user / circleNumber;
        y_user /= circleNumber;

        float prev1Error = 0;
        float currentError = 1000000;

        // 마지막 오류가 현재 오류와 같으면 더 좋은 값이 계산되지 않음
        while (prev1Error != currentError) {
            prev1Error = currentError;
            // Calculate the position up, down, left and right of the current one to calculate its error there.
            ArrayList<float[]> newPositions = new ArrayList<float[]>();
            newPositions.add(new float[]{(float) (x_user + 0.1), y_user});
            newPositions.add(new float[]{(float) (x_user - 0.1), y_user});
            newPositions.add(new float[]{x_user, (float) (y_user + 0.1)});
            newPositions.add(new float[]{x_user, (float) (y_user - 0.1)});

            // 방향의 각 위치에 대해 오류를 계산
            for (float[] direction : newPositions) {
                float error = 0;
                ArrayList<Float> dist = new ArrayList<Float>();

                //각 비콘에 대한 특정 위치의 오류
                for (int i = 0; i < circleNumber; i++) {
                    float[] circlePos = circleArray.get(i);
                    dist.add((float) (Math.sqrt(Math.pow(circlePos[0] - direction[0], 2) +
                            Math.pow(circlePos[1] - direction[1], 2)) - circlePos[2]));
                    error += Math.pow(dist.get(dist.size() - 1), 2);
                }
                error = (float) Math.sqrt(error);

                //오류가 더 작 으면 작은 값을 사용
                if (error < currentError) {
                    Collections.sort(dist);
                    r_user = dist.get(dist.size() - 1);
                    x_user = direction[0];
                    y_user = direction[1];
                    currentError = error;
                }
            }
        }
        if(x_user >= preferences.getFloat("x1", -1) && x_user <= preferences.getFloat("x2", -1) && y_user >= preferences.getFloat("y1", -1) && y_user <= preferences.getFloat("y3", -1)){
            detect = "yes";
            paymentButton.setEnabled(true);
            //타이머 시작
            //연결
            startPayment();


        }else{
            detect = "no";
            detect = "no";
            paymentButton.setEnabled(false);
            if(payStart) {
                myCountDownTimer.cancel();
            }
            progress_timer.setProgress(100);
            payStart = false;
            paymentButton.setEnabled(false);
        }

        return new float[]{x_user, y_user, r_user};
    }

    /*
     *값에 거리를 더하고 여러 측정에 대해 비콘과의 거리를 계산
     */
    private void addDistance(Beacon beacon, int minorVal) {

        //ArrayList에 거리를 추가하고 특정 크기를 유지
        double distance = Utils.computeAccuracy(beacon);
        distance = distance*1.8;
        if(Mode == 0){
        distanceAvg.put(minorVal,distance);
        }
        else{
        ArrayList<Double> distanceToBeacon = distances.get(minorVal);
//        if(distanceToBeacon.size()>50){
//            distances.remove(minorVal);
//        }
        distanceToBeacon.add(distance);
        if (distanceToBeacon.size() > 40)
        {
            distanceToBeacon.remove(0);
        }
            if(Mode == 1)
            {
            average(distanceToBeacon, minorVal);
            }else{
                median(distanceToBeacon, minorVal);
            }
        }
    }

    /*
     * 중앙값을 취하여 표지까지의 거리를 계산
     */
    private void median(ArrayList<Double> distanceToBeacon, int minorVal) {
        Collections.sort(distanceToBeacon);
        int half = (int) (0.5 * distanceToBeacon.size());
        if (distanceToBeacon.size() == 1) {
            half = 0;
        }
        distanceAvg.put(minorVal, distanceToBeacon.get(half));
    }

    /*
     * 평균을 취하여 표지에 대한 거리를 계산
     */
    private void average(ArrayList<Double> distanceToBeacon, int minorVal) {
        double total = 0;

        for (double element : distanceToBeacon) {
            total += element;
        }
        distanceAvg.put(minorVal, total / distanceToBeacon.size());
    }

    /*
     * 그림에서 비콘의 위치를 ​​검색하고 계산
     */
    private float[] getLocation(Beacon beacon) {
        int minorVal = beacon.getMinor();

        // 비콘의 위치 찾기
        float beaconX = preferences.getFloat("x" + minorVal, 0);
        float beaconY = preferences.getFloat("y" + minorVal, 0);
        //개발 전용으로 설정!
        if(beaconX == 0 || beaconY == 0) {
            if (minorVal == 1)
            {
                editPref.putFloat("x1", 4);
                editPref.putFloat("y1", 4);
                editPref.putFloat("z1", 0);
                beaconX = 4;
                beaconY = 4;
            }
            else if (minorVal == 2)
            {

                editPref.putFloat("x2", 6);
                editPref.putFloat("y2", 4);
                editPref.putFloat("z2", 0);
                beaconX = 6;
                beaconY = 4;
            }
            else if (minorVal == 3)
            {
                editPref.putFloat("x3", 4);
                editPref.putFloat("y3", 6);
                editPref.putFloat("z3", 0);
                beaconX = 4;
                beaconY = 6;
            }
            else if (minorVal == 4)
            {
                editPref.putFloat("x4", 6);
                editPref.putFloat("y4", 6);
                editPref.putFloat("z4", 0);
            }
            editPref.commit();
        }


        //비콘 위치와 지도의 비율 계산
        float ratioX = beaconX / mapXSize;
        float ratioY = beaconY / mapYSize;

        // 이동해야하는 픽셀 수를 계산
        float locationPixelsX = (ratioX * widthPixels);
        float locationPixelsY = (ratioY * heightPixels);

        return new float[]{locationPixelsX, locationPixelsY};
    }
    private void getScreenSizes(Bitmap image_raw) {

        //상태 표시 줄의 높이 검색
        int statusbar_height = getStatusBarHeight();

        // 패딩 보정. 각 이미지뷰 위 아래 2픽셀씩
        int padding_height = 0;

        //화면 크기 검색
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int height_screen = metrics.heightPixels - statusbar_height - padding_height;
        int width_screen = (int) (metrics.widthPixels * 0.9);

        //그림 크기 검색 및 비율 계산
        int height_image = image_raw.getHeight();
        int width_image = image_raw.getWidth();
        float ratio = (float) (height_image) / width_image;

        //이미지와 그림 사이의 가장 낮은 비율 너비 / 높이에 따라 크기 조정.
        if (ratio < (float) (height_screen) / width_screen) {
            widthPixels = width_screen;
            heightPixels = (int) (height_image * (float) (width_screen) / width_image);
        } else {
            widthPixels = (int) (width_image * (float) (height_screen) / height_image);
            heightPixels = height_screen;
        }
    }

    private int getStatusBarHeight() {
        int status_bar_height = 0;
        int resource_id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource_id > 0) {
            status_bar_height = getResources().getDimensionPixelSize(resource_id);
        }
        return status_bar_height;
    }

    private double eucludianDistance(float x1, float y1, float x2, float y2){
        return Math.sqrt(Math.pow((x1-x2),2)+ Math.pow((y1-y2),2));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
       // MenuItem refreshItem = menu.findItem(R.id.refresh);
        //refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        return true;
    }



    @Override
    protected void onDestroy() {
        beaconManager.disconnect();

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        connectToService();
    }

    @Override
    protected void onStop() {
        //결과 묻기를 종료
        Boolean intentStop = preferences.getBoolean("intent_stop", true);
        if (intentStop) {
            try {
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            } catch (Exception e) {
            }
        }
        editPref.remove("intent_stop");
        editPref.commit();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //블루투스를 요청
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                getSupportActionBar().setSubtitle("\n" +
                        "블루투스가 사용 설정되지 않았습니다.");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
        //비콘을 검색
        getSupportActionBar().setSubtitle("스캔중...");
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                    getSupportActionBar().setSubtitle("범위 지정을 시작할 수 없습니다.");
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_average:
                ModeIndoorAlgorithm = 0;
                resetDistance();
                return true;
            case R.id.btn_triangle:
                ModeIndoorAlgorithm = 1;
                resetDistance();
                return true;
            case R.id.btn_distance_average:
                Mode = 3;
                resetDistance();
                return true;
            case R.id.btn_distance_median:
                Mode = 2;
                resetDistance();
                return true;
            case R.id.btn_distance_real:
                //mode is 0
                Mode = 0;
                resetDistance();
                return true;
            case android.R.id.home:
                //Main액티비티로
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            default:
                Log.w(TAG,"사용자의 행동이 인지되지 않음");
                //슈퍼클래스 호출하여 처리
                return super.onOptionsItemSelected(item);

        }
    }
    public void resetDistance(){
        measurements = 0;
        distances = new HashMap<Integer, ArrayList<Double>>();
        distanceAvg = new HashMap<Integer, Double>();
        reset = false;
    }

    public void getSecretBeacon(final String minorVal){
        Log.d("masukFirebase",minorVal);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("secretBeacon");
        //파이어베이스에서 값 얻어오기
        final String[] result = {""};
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 메소드가 초기화 값으로 한번 호출되며 이 위치로 다시 호출 됨

                String value = dataSnapshot.child(minorVal).child("secret").getValue(String.class);
                String POS = dataSnapshot.child(minorVal).child("POS_ADDRESS").getValue(String.class);
                editPref.putString("secretBeacon"+minorVal, value);
                editPref.commit();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                //값을 읽는데 오류가 발생
                Log.w("Error", "Failed to read value.", error.toException());
            }
        });

    }

    public void onRadioButtonClicked(View view) {
        //버튼 선택 유무 확인
        boolean checked = ((RadioButton) view).isChecked();

        //선택한 라디오 버튼 확인
        switch(view.getId()) {
            case R.id.radio_indoor_pythagoras:
                Log.i("MapPos","---------------------피타고라스-----------------");
                if (checked)
                    ModeIndoorAlgorithm = 1;
                    resetDistance();
                    Toast.makeText(MapPosActivity.this,"알고리즘이 피타고라스 및 유클리드 거리로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    break;
            case R.id.radio_indoor_average:
                Log.i("MapPos","-----------------------평균----------------------");
                if (checked)
                    ModeIndoorAlgorithm = 0;
                    resetDistance();
                    Toast.makeText(MapPosActivity.this,"알고리즘이 무게 평균으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    break;
        }
    }

    public void onCheckboxClicked(View view) {
        //버튼 선택 유무 확인
        boolean checked = ((CheckBox) view).isChecked();

        // 선택한 라디오 버튼 확인
        switch(view.getId()) {
            case R.id.checkbox_automatic:
                if (checked){
                    if(payStart) {
                        myCountDownTimer.cancel();
                    }
                    paymentButton.setVisibility(View.GONE);
                    automatic_payment = true;
                    progress_timer.setProgress(100);

                    payStart = false;
                }else{
                    if(payStart){
                        myCountDownTimer.cancel();
                    }
                    automatic_payment = false;
                    progress_timer.setProgress(100);
                    payStart = false;
                 paymentButton.setVisibility(View.VISIBLE);
                }

                Toast.makeText(MapPosActivity.this,"알고리즘이 피타고라스 및 유클리드 거리로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.record_experiment:
                if (checked){

                    recordExperiment= true;
                }else{
                    recordExperiment = false;
                }

                Toast.makeText(MapPosActivity.this,"알고리즘이 피타고라스 및 유클리드 거리로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    public void onRadioButtonDistanceClicked(View view) {
        //버튼 선택 유무 확인
        boolean checked = ((RadioButton) view).isChecked();

        //선택한 라디오 버튼 확인
        switch(view.getId()) {
            case R.id.radio_real_distance:
                if (checked)
                    Mode = 0;
                resetDistance();
                Toast.makeText(MapPosActivity.this,"계산 알고리즘이 실제 거리로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.radio_average:
                if (checked)
                    Mode = 1;
                resetDistance();
                Toast.makeText(MapPosActivity.this,"\n" + "계산 알고리즘이 평균으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.radio_median:
                if (checked)
                    Mode = 2;
                resetDistance();
                Toast.makeText(MapPosActivity.this,"계산 알고리즘이 중앙값으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void startPayment(){
        if(!payStart && automatic_payment) {//!결제가 진행 중 && 자동결제 == true
            payStart = true; //결제 == true
            progress_timer.setProgress(100); //프로그래스바 타이머 설정
            myCountDownTimer = new MyCountDownTimer(10000, 500); // 블루투스 연동 타이머 객체
            myCountDownTimer.start(); //타이머 시작
        }
    }

    public void recordPosition(float x, float y){
        if(recordExperiment)
            compress.captureResult(String.valueOf(ModeIndoorAlgorithm),System.currentTimeMillis(),realPosition.getText().toString(),x, y,detect,myRef);
    }

    public class MyCountDownTimer extends CountDownTimer {

        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
           // textCounter.setText(String.valueOf(millisUntilFinished));
            int progress = (int) (millisUntilFinished/100);
            progress_timer.setProgress(progress);
        }

        @Override
        public void onFinish() {
            //textCounter.setText("Task completed");

            payStart = true;
            progress_timer.setProgress(0);
            Intent paymentIntent = new Intent(MapPosActivity.this, PaymentProcess.class);

            try {
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            } catch (Exception e) {
            }
            editPref.putBoolean("intent_stop", false);
            editPref.commit();
            startActivity(paymentIntent);
        }
    }
}
