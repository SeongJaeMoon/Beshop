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
import android.bluetooth.BluetoothSocket;
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
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.SystemRequirementsChecker;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

import app.cap.beshop.dataStructures.CartItem;
import app.cap.beshop.payment.jPake;
import app.cap.beshop.payment.compress;
import app.cap.beshop.payment.Sss;
import com.android.tonyvu.sc.model.Cart;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@TargetApi(Build.VERSION_CODES.M)
public class Betriever extends AppCompatActivity {


    private Button paymentButton;
    private Long TimeofProtocol;
    private Long cursorTime;
    private String pos_address = "BLE";
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("00001111-0000-1000-8000-00805F9B34FB");
    public static final java.util.UUID CHAR_UUID = java.util.UUID.fromString("00002222-0000-1000-8000-00805F9B34FB");
    public TextView connected_device, log_view, info_text;

    public String tampLogs = "Logs:\n";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;
    // Initializes Bluetooth adapter.

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 20000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    private ProgressDialog dialog;
    private List<BluetoothGattService> services;
    private BluetoothGattCharacteristic characteristicData;

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
    private boolean PosIsHere = false;
    private BluetoothSocket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        setContentView(R.layout.activity_betriever);

        info_text = (TextView) findViewById(R.id.info_text_payment);
        connected_device = (TextView) findViewById(R.id.tx_connected);
        log_view = (TextView) findViewById(R.id.tx_logs);
        log_view.setMovementMethod(new ScrollingMovementMethod());
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage("POS 연결 중...");
        dialog.show();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();

        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        paymentButton = (Button) findViewById(R.id.payment_button);

        paymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cursorTime = System.currentTimeMillis();
                try{
                    if(connectedPOS){
                        String payment = "payment";
                        info_text.setText("결제 완료!");
                        log_view.setText("결제가 완료되었습니다.");
                        sendCartList(payment);
                        sendNotification("결제 하였습니다.");
                        paymentButton.setVisibility(View.GONE);
                        scanLeDevice(false);
                        compress.timeSpan(cursorTime, cursorTime, payment, myRef);
                        PosIsHere = true;
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"결제를 진행할 수 없습니다.",Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
            scanLeDevice(true);
        }
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
        try {
            super.onDestroy();
            if (mGatt == null) {
                return;
            }
            mGatt.close();
            mGatt = null;
            scanLeDevice(false);
            dialog.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    mLEScanner.stopScan(mScanCallback);
                    if (!PosIsHere){
                        Toast.makeText(getApplicationContext(), "결제가능 시간 초과, 결제를 진행할 수 없습니다.\n 다음에 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                        dialog.cancel();
                        finish();
                    }
                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
            //Toast.makeText(getApplicationContext(), "결제 가능 시간 초과 \n 결제를 진행 할 수 없습니다.",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
    }

    private void log(final String name,final String Price,final String quantity){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log_view.setText(name + Price + quantity);
            }
        });
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice btDevice = result.getDevice();
            if (btDevice.getName() != null && !connectedPOS) {
                if (btDevice.getName().equals(pos_address)) {
                    connected_device.setText(btDevice.getName());
                    connectToDevice(btDevice);
                    connectedPOS = true;
                    info_text.setText("결제 진행 중...");
                    try {
                        dialog.cancel();
                        String cartList = getIntent().getExtras().getString("cartList");
                        log_view.setText(cartList);
                    }
                     catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.w("ScanResult-Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);
            mGatt.requestMtu(600);
        }
        TimeofProtocol = System.currentTimeMillis();

    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mGatt.discoverServices();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            float time = System.currentTimeMillis();
            services = mGatt.getServices();
            for (BluetoothGattService service : services) {
                if (service.getUuid().equals(SERVICE_UUID)) {
                    characteristicData = service.getCharacteristic(CHAR_UUID);
                    for (BluetoothGattDescriptor descriptor : characteristicData.getDescriptors()) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        mGatt.writeDescriptor(descriptor);

                    }
                    gatt.setCharacteristicNotification(characteristicData, true);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (packetInteration < packetSize) {
                characteristicData.setValue(packets[packetInteration]);
                mGatt.writeCharacteristic(characteristicData);
                packetInteration++;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        }
    };

    public void sendCartList(String string) {
        byte[] buffer = string.getBytes();
        int chunk = 20;
        packetSize = (int) Math.ceil(string.length() / (double) chunk);
        characteristicData.setValue(packetSize.toString().getBytes());
        mGatt.writeCharacteristic(characteristicData);
        mGatt.executeReliableWrite();

        packets = new byte[packetSize][chunk];
        packetInteration = 0;
        Integer start = 0;
        for (int i = 0; i < packets.length; i++) {
            int end = start + chunk;
            if (end > buffer.length) {
                end = buffer.length;
            }
            packets[i] = Arrays.copyOfRange(buffer, start, end);
            start += chunk;
        }
    }

    private void sendTest(){

    }
    private void sendNotification(String message) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.logo1)
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


