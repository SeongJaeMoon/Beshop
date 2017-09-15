package app.cap.beshop;

import android.app.ActivityManager;
import android.app.Application;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

public class Beshop extends Application{

    private static final String TAG = Beshop.class.getName();

    BeaconManager beaconManager;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!FirebaseApp.getApps(this).isEmpty()) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        }
        beaconManager = new BeaconManager(getApplicationContext());
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startMonitoring(new Region("monitored region",
                        UUID.fromString("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"),
                        30001,
                        10138));
            }
        });

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List list) {
                //showNotification("들어옴", "비콘 연결됨" + list.get(0));
                //이미 앱이 실행중이면 Notification만 준다
                //if (isAppRunning(getApplicationContext())){
                  //  Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   // intent.putExtra("executeType", "beacon_find");
                    //getApplicationContext().startActivity(intent);
                //}
                //else
                //{
                showNotification("Betriever", "비콘을 찾았습니다.");
            }

            @Override
            public void onExitedRegion(Region region) {
                showNotification("Betriever", "비콘을 찾을 수 없습니다.");
            }
        });
    }
    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(
                this, 0, new Intent[]{notifyIntent} , PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(title) .setContentText(message)
                .setSmallIcon(R.drawable.logo1)
                .setTicker("Betriever")
                .setAutoCancel(true) .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_HIGH) .build();
        notification.defaults |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    private boolean isAppRunning(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++){
            if(procInfos.get(i).processName.equals(context.getPackageName())){
                return true;
            }
        }
        return false;
    }
}