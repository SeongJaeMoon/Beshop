package app.cap.beshop;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.estimote.sdk.SystemRequirementsChecker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

import app.cap.beshop.payment.compress;
import app.cap.beshop.payment.jPake;
import app.cap.beshop.payment.Sss;
import app.cap.beshop.Adapter.ProductAdapter;
import app.cap.beshop.dataStructures.Constant;
import app.cap.beshop.Adapter.CartItemAdapter;
import app.cap.beshop.dataStructures.CartItem;
import app.cap.beshop.Adapter.CartItemAdapter;
import app.cap.beshop.dataStructures.CartItem;
import app.cap.beshop.dataStructures.Product;
import com.android.tonyvu.sc.model.Cart;
import com.android.tonyvu.sc.model.Saleable;
import com.android.tonyvu.sc.util.CartHelper;

@TargetApi(Build.VERSION_CODES.M)
public class PaymentProcess extends AppCompatActivity {


    private Button paymentButton;
    private CartItem cartItem;
    private Long TimeofProtocol;
    private Long cursorTime;
    //장치 이름에 판매 시점 주소를 설정
    private String pos_address="";
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("00001111-0000-1000-8000-00805F9B34FB");
    public static final java.util.UUID CHAR_UUID = java.util.UUID.fromString("00002222-0000-1000-8000-00805F9B34FB");
    public TextView connected_device,log_view, info_text;
    public String tampLogs = "결제 기록:\n";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;
    //Bluetooth 어댑터를 초기화
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 100000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
   //private ScanCallback mScanCallback;
    private ProgressDialog dialog;
    private List<BluetoothGattService> services;
    private BluetoothGattCharacteristic characteristicData;
    private final jPake jpake = new jPake(1);
    public byte[] round1;
    public byte[] round2;
    public byte[] round3;
    public byte[] round4;
    public Integer numPackets = 0;
    public byte[] packetData;
    public boolean packetFinish = false;
    public Integer protoclCount = 0;
    public Integer packetSize = 0;
    public Integer packetInteration = 0;
    public boolean connectedPOS = false;
    private static int NOTIFICATION_ID = 0;
    public byte[][] packets;
   public FirebaseDatabase database = FirebaseDatabase.getInstance();
    public DatabaseReference myRef = database.getReference();
    ListView lvCartItems;
    TextView tvTotalPrice;
    final CartItemAdapter cartItemAdapter = new CartItemAdapter(this);
    final Cart cart = CartHelper.getCart();
    @Override
    protected void onCreate(Bundle savedInstanceState){

       super.onCreate(savedInstanceState);
        //BLE체크 활성화 여부
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        setContentView(R.layout.payment);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //키 공유 설정


        lvCartItems = (ListView) findViewById(R.id.tx_list);
        LayoutInflater layoutInflater = getLayoutInflater();

        final TextView tvTotalPrice = (TextView) findViewById(R.id.tx_logs);
        //lvCartItems.addHeaderView(layoutInflater.inflate(R.layout.cart_header, lvCartItems, false));
        //cartItemAdapter.updateCartItems(getCartItems(cart));

        info_text  = (TextView) findViewById(R.id.info_text_payment);
        info_text.setText("결제 동의 정보");
        connected_device = (TextView) findViewById(R.id.tx_connected);
        //log_view = (TextView) findViewById(R.id.tx_logs);
        //log_view.setMovementMethod(new ScrollingMovementMethod());
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);  //다이얼로그 끄기/키기 설정
        dialog.setMessage("Loading");
        dialog.show();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();

        final Sss secretSharing = new Sss(3,4);
        BigInteger PrimeShammir = new BigInteger("268222406735819");
        Sss.SecretShare [] BeaconId = new Sss.SecretShare[4];

        String minor1 = preferences.getString("secretBeacon"+String.valueOf(1),"76067123809422");
        String minor2 = preferences.getString("secretBeacon"+String.valueOf(2),"181564352822156");
        String minor3 = preferences.getString("secretBeacon"+String.valueOf(3),"171726069403495");
        String minor4 = preferences.getString("secretBeacon"+String.valueOf(4),"46552273553439");
        editPref.apply();
        BeaconId[0] = new Sss.SecretShare(0,new BigInteger(minor1));
        BeaconId[1] = new Sss.SecretShare(1,new BigInteger(minor2));
        BeaconId[2] = new Sss.SecretShare(2,new BigInteger(minor3));
        BeaconId[3] = new Sss.SecretShare(3,new BigInteger(minor4));

        final BigInteger result = secretSharing.combine(BeaconId, PrimeShammir);

        jpake.setPassword(result.toString());
        Log.d("password" ,jpake.s2Str);

        mHandler = new Handler();
        //스캔 프로세스 및 지불 영역 입력 여부를 확인, 입력시 지불 버튼을 활성화
        //pos와 연결을 설정
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        Log.i("TAG" , "111111111111111111111111111111111");
        paymentButton = (Button) findViewById(R.id.payment_button);
        paymentButton.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                //sendData(round1);
                cursorTime = System.currentTimeMillis();
                try {
                    updateLogs(round3);
                    sendData(round3);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //작업 표시 줄의 앱 아이콘을 클릭 -> Main액티비티
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            if (Build.VERSION.SDK_INT >= 23) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
        }
            scanLeDevice(true);

    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.d("scan le device", "true");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT>Build.VERSION_CODES.M)
                        {
                        Log.d("sdk","기입");
                        mLEScanner.stopScan(mScanCallback);
                            Log.i("TAG" , "22222222222222222222222222222222");
                    }
                }
            }, SCAN_PERIOD);
                Log.d("sdk","기입");
                mLEScanner.startScan(filters, settings, mScanCallback);
            Log.i("TAG" , "33333333333333333333333333333333333333333333");
        }
        else
            {
                Log.d("sdk","기입");
                mLEScanner.stopScan(mScanCallback);
                Log.i("TAG" , "44444444444444444444444444444444444444444");
             }
          }

    @Override
    protected void onStart(){
        super.onStart();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
    }
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
           //connectToDevice(btDevice);

            if(btDevice.getName()!=null && !connectedPOS){
                Log.i("btName", btDevice.getName());
                if(btDevice.getName().equals(pos_address)) {
                    Log.i("lescan detected", pos_address);
                    connected_device.setText(btDevice.getName());
                    connectToDevice(btDevice);

                    connectedPOS = true;
                    if (!jpake.round1) {
                        try {
                            cursorTime = System.currentTimeMillis();
                            round1 = jpake.jpakeRound1();
                            //파이어베이스 로그
                            compress.timeSpan(TimeofProtocol,cursorTime,"1_JpakeRound1",myRef);
                            cursorTime = System.currentTimeMillis();
                            Log.d("round1", String.valueOf(round1.length));
                            jpake.round1=true;
                            updateLogs(round1);

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (DataFormatException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult-Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private void mScanCallbackfunc(){
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
                mScanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.i("callbackType", String.valueOf(callbackType));
                        Log.i("result", result.toString());
                        Log.i("onLeScan", result.toString());

                        BluetoothDevice btDevice = result.getDevice();
                        if(btDevice.getName()!=null){
                            Log.i("btName", btDevice.getName());
                           if(btDevice.getName().equals(pos_address)){
                                connectToDevice(btDevice);
                                if(!jpake.round1) {
                                    try {

                                       round1 = jpake.jpakeRound1();

                                        Log.d("round1", String.valueOf(round1.length));
                                        updateLogs(round1);
                                        jpake.round1 = true;
                                        sendData(round1);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (DataFormatException e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        }


                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        for (ScanResult sr : results) {
                            Log.i("ScanResult - Results", sr.toString());
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.e("Scan Failed", "Error Code: " + errorCode);
                    }
                };
            }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            if(device.getName()!=null){
                            Log.i("btName", device.getName());

                                if(device.getName().equals(pos_address)) {
                                    Log.i("lescan detected", pos_address);

                                    connectToDevice(device);
                                    if (!jpake.round1) {
                                        try {
                                            round1 = jpake.jpakeRound1();
                                            Log.d("round1", String.valueOf(round1.length));
                                            jpake.round1=true;
                                            updateLogs(round1);

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (DataFormatException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }
                            }

                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);//첫 번째 장치 감지 후 중지
            mGatt.requestMtu(600);
        }
        TimeofProtocol = System.currentTimeMillis();

    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {

                mGatt.discoverServices();
                Log.i("TAG" , "5555555555555555555555555555555555555555555");
               // compress.timeSpan(TimeofProtocol,cursorTime,"2_ServiceDiscovery",myRef);
            }
            //else
            // {
                //if(dialog.isShowing()){
                    //mHandler.post(new Runnable() {
                       //@Override
                        //public void run() {
                            //dialog.hide();
                        //}
                    //});
                //}
            //}
        }

        @Override
        public void onMtuChanged (BluetoothGatt gatt, int mtu, int status){
            Log.d("MTUSIZE", String.valueOf(mtu));
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //System.out.println("SERVICE_SIZE_TIME:"+ (System.currentTimeMillis() - time));
            float time = System.currentTimeMillis();
            services = mGatt.getServices();
            System.out.println("서비스 크기:"+services);
            for(BluetoothGattService service : services){

                if( service.getUuid().equals(SERVICE_UUID)) {

                    characteristicData = service.getCharacteristic(CHAR_UUID);
                    for (BluetoothGattDescriptor descriptor : characteristicData.getDescriptors()) {
                        descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        mGatt.writeDescriptor(descriptor);

                    }
                    gatt.setCharacteristicNotification(characteristicData, true);
                }
            }

            //log to firebase
            compress.timeSpan(TimeofProtocol,cursorTime,"2_ServiceDiscovery",myRef);
            cursorTime = System.currentTimeMillis();

            sendData(round1);
//            if (dialog.isShowing()){
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        dialog.hide();
//                    }
//                });
//            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", "onCharacteristicRead");
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicWrite", "onCharacteristicWrite");

                if (packetInteration < packetSize) {
                    Log.d("packetLength", String.valueOf(packets.length));
                    Log.d("interation", packetInteration.toString());
                    Log.d("packetSize", String.valueOf(packets[packetInteration].length));
                    Log.d("value", new String(packets[packetInteration]));
                    characteristicData.setValue(packets[packetInteration]);
                    mGatt.writeCharacteristic(characteristicData);
                    packetInteration++;
                } else if (jpake.round2 && packetInteration == packetSize && protoclCount == 3) {
                    Log.d("packetLength", String.valueOf(packets.length));
                    Log.d("interation", packetInteration.toString());
                    Log.d("packetSize", String.valueOf(packets[packetInteration].length));
                    Log.d("value", new String(packets[packetInteration]));
                    characteristicData.setValue(packets[packetInteration]);
                    mGatt.writeCharacteristic(characteristicData);
                    packetInteration++;
                    compress.timeSpan(TimeofProtocol,cursorTime,"6_JpakeRound2CtoS",myRef);
                    cursorTime = System.currentTimeMillis();
                }
            if(!jpake.round2&&packetInteration==packetSize){
                compress.timeSpan(TimeofProtocol,cursorTime,"3_JpakeRound1_CtoS",myRef);
                cursorTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i("onCharacteristicRead", characteristic.toString());
            byte[] value=characteristic.getValue();
            //read how many packets
            if(numPackets==0) {
                if(protoclCount==3){
                    // c의 라운2를 서버에서 찾기 시작
                    cursorTime = System.currentTimeMillis();
                }
                packetFinish=false;
                packetData = new byte[0];
                numPackets = Integer.valueOf(new String(value));
            }else{
                    packetData =combineByte(packetData,value);
                    numPackets--;
                    if(numPackets==0){
                        packetFinish=true;
                       // packetData = null;
                    }
            }
            if(packetFinish){
                System.out.println("PacketLength: "+packetData.length);
            try {
                updateLogs(packetData);
                packetData = null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            }
        }
    };
    public byte[] combineByte(byte[] byte1, byte[] byte2){
        byte[] combined = new byte[byte1.length + byte2.length];

        for (int i = 0; i < combined.length; ++i)
        {
            combined[i] = i < byte1.length ? byte1[i] : byte2[i - byte1.length];
        }
        return combined;
    }
    public void updateLogs(byte[] message) throws IOException, DataFormatException {
        byte[] decomp = compress.decompress(message);
        System.out.println(message.length);
        System.out.println(decomp.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(decomp);
        DataInputStream in = new DataInputStream(bais);
        List<String> result = new ArrayList<String>();
        while (in.available() > 0) {
            String element = in.readUTF();
            result.add(element);
            Log.d("hasil",element);
            if(element.equals("1")){
                tampLogs+="\nround 1(X1,X2,ZKP1,ZKP2)";
            }else if(element.equals("2")){
                tampLogs+="\nround 2 Received (X3,X4,ZKP3,ZKP4)";
            }
            if(element.length()>30){
                element = "("+element.length()+")"+element.substring(0,20)+"...";
            }
            tampLogs+="\n"+element;

        }
        if(protoclCount ==1 && !jpake.round2){
            compress.timeSpan(TimeofProtocol,cursorTime,"4_JpakeRound1_StoC",myRef);
            cursorTime = System.currentTimeMillis();
        }
        else if(jpake.round2 && protoclCount==3){
            compress.timeSpan(TimeofProtocol,cursorTime,"6_JpakeRound2StoC",myRef);
            cursorTime = System.currentTimeMillis();
        }
        jpake.updateValue(result);
        protoclCount++;
        if(protoclCount==2 && !jpake.round2){

            //round2 준비
            round2 = jpake.jpakeRound2();

            compress.timeSpan(TimeofProtocol,cursorTime,"5_JpakeRound2",myRef);
            Log.d("protocol", "round 2 running");
            updateLogs(round2);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            jpake.round2=true;
            cursorTime = System.currentTimeMillis();
            sendData(round2);
        }else if(jpake.round2 && protoclCount==4){
            compress.timeSpan(TimeofProtocol,cursorTime,"7_sessionKeyCalculation",myRef);
            compress.timeSpan(TimeofProtocol,TimeofProtocol,"8_SessionKeyConstruction",myRef);

            cursorTime = System.currentTimeMillis();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(jpake.finalSKey.equals("")&&protoclCount == 4 && !jpake.round3 ){
                    tampLogs+="\n------------------- \n최종키: "+jpake.getfinalSKey()+"\n 최종 토큰: "+jpake.getFinalNonce();
                    try {
                        round3 = jpake.pprotocolRound1();
                        jpake.round3 =true;
                        if(dialog.isShowing()){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.hide();
                                    info_text.setText("결제 완료");
                                }
                            });
                        }
                        //updateLogs(round3);
                        //sendData(round3);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }else if (protoclCount == 6 && !jpake.round4){
                    try {
                        round4 = jpake.pprotocolRound2();
                        jpake.round4 =true;
                        updateLogs(round4);
                        sendData(round4);
                        compress.timeSpan(TimeofProtocol,cursorTime,"ProposedProtocol",myRef);
                        sendNotification("결제가 완료되었습니다.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DataFormatException e) {
                        e.printStackTrace();
                    }
                }
                //log_view.setText(tampLogs);
                tvTotalPrice.setText(String.valueOf(cart.getTotalPrice().setScale(2, BigDecimal.ROUND_HALF_UP)));
                lvCartItems.setAdapter(cartItemAdapter);
            }
        });
    }

    public void sendData(byte [] data){
        packetInteration =0;
        int chunksize = 20;
        Log.d("dataLength",String.valueOf(data.length));
        packetSize = (int) Math.ceil( data.length / (double)chunksize);
        Integer tamp = packetSize;
        characteristicData.setValue(packetSize.toString().getBytes());
        // characteristicData.setValue(data);
        mGatt.writeCharacteristic(characteristicData);
        mGatt.executeReliableWrite();
        //chunksize = 540;
        Log.d("dataLength",String.valueOf(data.length));
        packetSize = (int) Math.ceil( data.length / (double)chunksize);
        Log.d("dataLength",String.valueOf(data.length));
        Log.d("packetSize",packetSize.toString());
        packets = new byte[packetSize][chunksize];
        Integer start = 0;
        int x =0;

        if(jpake.round2 && protoclCount == 3){
            //두 번째 프로토콜 버그로 첫 번째 바이트 보내지 않음, 더미 바이트 만들기
            //패킷 크기;
            x=1;
            packets = new byte[packetSize+1][chunksize];
            packets[0] = tamp.toString().getBytes();
        }
        for(int i = x; i < packets.length; i++) {
            int end = start+chunksize;
            if(end>data.length){end = data.length;}
            packets[i] = Arrays.copyOfRange(data,start, end);
            start += chunksize;
        }
        //packets[0] = data;
        //packets will be send in onCharacteristicRead.
    }

//    public void sendData(byte [] data){
//        packetInteration =0;
//        int chunksize = 18;
//        Log.d("dataLength",String.valueOf(data.length));
//        packetSize = (int) Math.ceil( data.length / (double)chunksize);
//        Integer tamp = packetSize;
//        characteristicData.setValue(packetSize.toString().getBytes());
//        // characteristicData.setValue(data);
//        mGatt.writeCharacteristic(characteristicData);
//        mGatt.executeReliableWrite();
//        chunksize = 600;
//        packetSize = (int) Math.ceil( data.length / (double)chunksize);
//        Log.d("packetSize",packetSize.toString());
//        packets = new byte[packetSize][chunksize];
//        Integer start = 0;
//        int x =0;
//
//        if(jpake.round2 && protoclCount == 3){
//            //bugs on second protocol, first byte not send, sowe made dummy bytes
//            //packetSize;
//            x=1;
//            packets = new byte[packetSize+1][chunksize];
//            packets[0] = packetSize.toString().getBytes();
//        }
//        for(int i = x; i < packets.length; i++) {
//            int end = start+chunksize;
//            if(end>data.length){end = data.length;}
//            packets[i] = Arrays.copyOfRange(data,start, end);
//            start += chunksize;
//        }
//
//        //packets[0] = data;
//
//
//
//
//
//
//        //packets will be send in onCharacteristicRead.
//    }

    private void sendNotification(String message){
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.logo2)
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setAutoCancel(true)
                        .setContentText(message);
        Notification note = mBuilder.build();
        note.defaults |= Notification.DEFAULT_VIBRATE;
        note.defaults |= Notification.DEFAULT_SOUND;
        mNotificationManager.notify(NOTIFICATION_ID++, note);
    }
}
