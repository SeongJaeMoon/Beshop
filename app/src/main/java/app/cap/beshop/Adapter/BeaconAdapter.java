package app.cap.beshop.Adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import app.cap.beshop.R;

public class BeaconAdapter extends BaseAdapter {
    private ArrayList<Beacon> beacons;
    private LayoutInflater inflater;
    private SharedPreferences preferences;
    Context c;

    private ArrayList<Integer> minorValues = new ArrayList<Integer>();
    private HashMap<Integer, ArrayList<Double>> distances = new HashMap<Integer, ArrayList<Double>>();

    public BeaconAdapter(Context context) {

        this.inflater = LayoutInflater.from(context);
        this.beacons = new ArrayList<Beacon>();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        c = context;
    }

    //이전 목록을 새로운 비콘으로 대체

    public void replaceWith(Collection<Beacon> newBeacons) {
        this.beacons.clear();
        this.beacons.addAll(newBeacons);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public Beacon getItem(int position) {
        return beacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.device_item, null);
        bind(getItem(position), view);
        return view;
    }
    /*
   * 거리를 더하고 비콘과의 거리 측정 값의 평균을 계산
   */
    private double average(int minor, double dist) {
        if (!distances.containsKey(minor)) {
            distances.put(minor, new ArrayList<Double>());
        }
        ArrayList<Double> distanceArray = distances.get(minor);
        distanceArray.add(dist);

        // 마지막으로 측정 된 거리만 기억
        if (distanceArray.size() > 10) {
            distanceArray.remove(0);
        }

        //배열의 모든 요소를 ​​합친 다음 크기로 나누어 평균값 계산
        double total = 0;
        for (double element : distanceArray) {
            total += element;
        }

        double avg = total / distanceArray.size();
        return avg;
    }



    /*
   * 모든 값을 TextViews로 설정
   */
    private void bind(Beacon beacon, View view) {
        int minor = beacon.getMinor();
        Double dist = Utils.computeAccuracy(beacon);

        DecimalFormat df = new DecimalFormat("#0.####");
        double avg_dist = average(minor, dist);
        final TextView uuidTextView = (TextView) view.findViewById(R.id.uuid);
        final TextView majorminorTextView = (TextView) view.findViewById(R.id.majorminor);
        final TextView distanceTextView = (TextView) view.findViewById(R.id.distance);
        final TextView posTextView = (TextView) view.findViewById(R.id.position);
        final TextView rssiTextView = (TextView) view.findViewById(R.id.rssi);
        uuidTextView.setText("UUID: " + beacon.getProximityUUID());
        majorminorTextView.setText("Major (Minor): \t \t" + beacon.getMajor() + " (" + minor + ")");
        distanceTextView.setText("Distance: \t \t \t \t \t \t" + df.format(avg_dist));
        posTextView.setText("Position(x, y, z): \t \t" + preferences.getFloat("x"+minor, -1) + ", " +
                preferences.getFloat("y"+minor, 0) + ", " + preferences.getFloat("z"+minor, 0));
        rssiTextView.setText("RSSI: " + beacon.getRssi() + "\t Measured Power: " + beacon.getMeasuredPower());
    }


}
