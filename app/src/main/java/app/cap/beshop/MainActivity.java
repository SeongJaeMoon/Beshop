package app.cap.beshop;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.SystemRequirementsChecker;
import com.bumptech.glide.Glide;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.j256.ormlite.stmt.query.In;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import app.cap.beshop.Adapter.BackPressCloseHandler;
import app.cap.beshop.dataStructures.Feed;
import de.hdodenhof.circleimageview.CircleImageView;



public class MainActivity extends AppCompatActivity {
    BackPressCloseHandler backPressCloseHandler;

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private static final String TAG = "MainActivity";
    private final String LIST_STATE_KEY = "recycler_state";
    FloatingActionButton fab;
    Parcelable mListState;
    private RecyclerView rvMainRecyclerView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private NavigationView navigationView;
    private TextView navigationUserName, navigationUserEmail;
    private CircleImageView navigationUserImage;
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mDatabaseUsersRef;
    private DatabaseReference mDatabaseLikesRef;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private LinearLayoutManager layoutManager;
    private boolean mProcessLike = false;
    private ArrayList<Integer> minorValues = new ArrayList<Integer>();
    private HashMap<Integer, ArrayList<Double>> distances = new HashMap<Integer, ArrayList<Double>>();
    private boolean isNear = false;
    private BeaconManager beaconManager;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;
    private boolean isFirst = true;
    private View view;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backPressCloseHandler = new BackPressCloseHandler(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();

        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNear){
                    startActivity(new Intent(MainActivity.this, PostActivity.class));
                }
                else{
                    Toast.makeText(getApplicationContext(), "이 기능을 실행하기 위해선 비콘이 인식되어야 합니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        mDrawerLayout = (DrawerLayout) findViewById(R.id.mainDrawerLayout);
        navigationView = (NavigationView) findViewById(R.id.mainNavigationView);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                fab.setAlpha(1 - slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                fab.setVisibility(View.GONE);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                fab.setVisibility(View.VISIBLE);
            }
        };
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        View mview = navigationView.getHeaderView(0);
        navigationUserImage = (CircleImageView) mview.findViewById(R.id.xcivUserProfileImage);
        navigationUserName = (TextView) mview.findViewById(R.id.xtvUserNameInNavigation);
        navigationUserEmail = (TextView) mview.findViewById(R.id.xtvUserEmailInNavigation);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("feeds");
        mDatabaseUsersRef = FirebaseDatabase.getInstance().getReference().child("users");
        mDatabaseLikesRef = FirebaseDatabase.getInstance().getReference().child("likes");
        mDatabaseRef.keepSynced(true);
        mDatabaseUsersRef.keepSynced(true);
        mDatabaseLikesRef.keepSynced(true);

        rvMainRecyclerView = (RecyclerView) findViewById(R.id.xrvMainRecyclerView);

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        rvMainRecyclerView.setLayoutManager(layoutManager);

        mAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {

                if (firebaseAuth.getCurrentUser() == null) {
                    Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        };

        mDatabaseUsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String name = dataSnapshot.child(currentUser.getUid()).child("name").getValue(String.class);
                    String email = dataSnapshot.child(currentUser.getUid()).child("email").getValue(String.class);
                    String user_image = dataSnapshot.child(currentUser.getUid()).child("profile_image").getValue(String.class);

                    navigationUserName.setText(name);
                    navigationUserEmail.setText(email);
                    Glide.with(getApplicationContext())
                            .load(user_image)
                            .dontAnimate()
                            .into(navigationUserImage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        try {
            SharedPreferences mPref = getSharedPreferences("isFirst", Activity.MODE_PRIVATE);
            Boolean bfirst = mPref.getBoolean("isFirst", false);
            if (!bfirst) {
                SharedPreferences.Editor editor = mPref.edit();
                editor.putBoolean("isFirst", true).apply();
                Toast.makeText(getApplicationContext(), "안녕하세요! 앱을 사용하기 전에 Betriever 창에서 \n도움말을 읽어주세요! :) ", Toast.LENGTH_LONG).show();
            }
            else if (bfirst)
            {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.nav_beacon:
                        final Intent beaconList = new Intent(MainActivity.this, BeaconListActivity.class);
                        beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                        editPref.putBoolean("intent_stop", false);
                        editPref.apply();
                        startActivity(beaconList);
                        break;
                    case R.id.nav_chatting:
                        if(isNear){
                            startActivity(new Intent(MainActivity.this, ChatActivity.class));
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "이 기능을 실행하기 위해선 비콘이 인식되어야 합니다.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    //case R.id.nav_payment:
                    //    if (cartProduct!=null) {
                    //        Intent payment = new Intent(MainActivity.this, Betriever.class);
                    //        payment.putExtra("cartProduct",cartProduct);
                    //        payment.putExtra("cartPrice", cartPrice);
                    //        beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                    //        editPref.putBoolean("intent_stop", false);
                    //        editPref.commit();
                    //        startActivity(payment);
                        //}
                        //else
                        //{
                         //   Toast.makeText(getApplicationContext(),"선택 된 상품이 없습니다. 상품을 먼저 선택해주세요!",Toast.LENGTH_SHORT).show();
                        //}

                    case R.id.nav_profile:
                        startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
                        break;
                    case R.id.nav_products:
                        if(isNear) {
                            startActivity(new Intent(MainActivity.this, ShopActivity.class));
                        }else {
                            Toast.makeText(getApplicationContext(), "이 기능을 실행하기 위해선 비콘이 인식되어야 합니다.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.nav_beshop:
                        startActivity(new Intent(MainActivity.this, AboutActivity.class));
                        break;
                    //case R.id.nav_settings:
                      //  final Intent mapPosItent = new Intent(MainActivity.this, MapPosActivity.class);
                       // beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                       // editPref.putBoolean("intent_stop", false);
                       // editPref.commit();
                       // startActivity(mapPosItent);
                       // finish();
                       // break;
                    case R.id.nav_logout:
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("로그아웃 하시겠습니까?");
                        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAuth.signOut();
                                Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            }
                        });

                        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();

                }
                return true;
            }
        });


        beaconManager = new BeaconManager(this);
        beaconManager.setForegroundScanPeriod(3000, 0);
        beaconManager.setRangingListener(new BeaconManager.RangingListener()
        {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                //runOnUiThread(new Runnable() {
                    //@Override
                    //public void run() {
                        //for (int i = 0; i < beacons.size(); i++) {
                            //Beacon currentBeacon = beacons.get(i);
                            //Double distance = Utils.computeAccuracy(currentBeacon);
                            //int minor = currentBeacon.getMinor();
                            //비콘이 발견 되었는지 확인
                            //if (!distances.containsKey(minor)) {
                               // minorValues.add(minor);
                              //  distances.put(minor, new ArrayList<Double>());
                            //}
                            //ArrayList<Double> distanceToBeacon = distances.get(minor);
                            //distanceToBeacon.add(distance);
                            //if (distanceToBeacon.size() > 100) {
                          //      distanceToBeacon.remove(0);
                        //    }
                      //      distances.put(minor, distanceToBeacon);
                    //    }
                  //  }
                //});
                    for (int i =0; i <beacons.size(); i++){
                        Beacon currentBeacon = beacons.get(i);
                        Double distance = Utils.computeAccuracy(currentBeacon);
                        Log.w(TAG, "distance:"+String.valueOf(distance));
                        if (distance < 20){
                            isNear = true;
                        }
                        else {
                            isNear = false;
                        }
                    }
            }
        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        mListState = layoutManager.onSaveInstanceState();
        outState.putParcelable(LIST_STATE_KEY, mListState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);

        if (savedInstanceState != null)
            mListState = savedInstanceState.getParcelable(LIST_STATE_KEY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //SystemRequirementsChecker.checkWithDefaultDialogs(this);
        connectToService();
        FirebaseRecyclerAdapter<Feed, FeedsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Feed, FeedsViewHolder>(

                Feed.class,
                R.layout.single_row_main,
                FeedsViewHolder.class,
                mDatabaseRef

        ) {
            @Override
            protected void populateViewHolder(final FeedsViewHolder viewHolder, final Feed model, int position) {

                final String feedU_id = model.getU_id();
                final String feed_key = getRef(position).getKey();
                DatabaseReference dbUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(feedU_id);
                dbUserRef.keepSynced(true);
                dbUserRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        String userName = dataSnapshot.child("name").getValue(String.class);
                        String userProfileImage = dataSnapshot.child("profile_image").getValue(String.class);

                        viewHolder.setUserName(userName);
                        viewHolder.setUserImage(MainActivity.this, userProfileImage);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                viewHolder.setTitle(model.getTitle());
                viewHolder.setDesc(model.getDesc());
                viewHolder.setDate(model.getDate());
                viewHolder.setImage(getApplicationContext(), model.getImage());
                viewHolder.setLike(feed_key);

                viewHolder.tvUserProfileName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, AnotherUserProfileActivity.class);
                        intent.putExtra("feed_uid", feedU_id);
                        startActivity(intent);
                    }
                });

                viewHolder.ivUserImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, AnotherUserProfileActivity.class);
                        intent.putExtra("feed_uid", feedU_id);
                        startActivity(intent);
                    }
                });

                viewHolder.ivImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, ImageViewActivity.class);
                        intent.putExtra("image_uri", model.getImage());
                        startActivity(intent);
                    }
                });

                viewHolder.ibLike.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mProcessLike = true;
                        mDatabaseLikesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                if (mProcessLike) {
                                    if (dataSnapshot.child(feed_key).hasChild(mAuth.getCurrentUser().getUid())) {
                                        viewHolder.ibLike.setImageResource(R.drawable.like_grey);
                                        mDatabaseLikesRef.child(feed_key).child(mAuth.getCurrentUser().getUid()).removeValue();
                                    } else {
                                        viewHolder.ibLike.setImageResource(R.drawable.like_orange);
                                        mDatabaseLikesRef.child(feed_key).child(mAuth.getCurrentUser().getUid()).setValue(getCurrentDateTime());
                                    }
                                    mProcessLike = false;
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }
        };

        rvMainRecyclerView.setAdapter(firebaseRecyclerAdapter);
        mAuth.addAuthStateListener(mAuthStateListener);

    }

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mListState != null) {
            layoutManager.onRestoreInstanceState(mListState);
        }
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if (isNear&&isFirst){
            isFirst=false;
            Intent intent = new Intent(this,popupActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
        Boolean intentStop = preferences.getBoolean("intent_stop", true);
        if (intentStop) {
            try {
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            } catch (Exception e) {
            }
        }
        editPref.remove("intent_stop");
        editPref.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Request Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(MainActivity.this, "No Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        }

        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getCurrentDateTime() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String getTime = sdf.format(date);
        return getTime;
    }

    public static class FeedsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        TextView tvUserProfileName;
        CircleImageView ivUserImage;
        ImageView ivImage;
        ImageButton ibLike;
        DatabaseReference dbLikesRef;
        FirebaseAuth mAuth;
        ArrayList<setLikes>likeValue;

        public FeedsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            dbLikesRef = FirebaseDatabase.getInstance().getReference().child("likes");
            mAuth = FirebaseAuth.getInstance();

            ivUserImage = (CircleImageView) mView.findViewById(R.id.xivUserImageInFeeds);
            tvUserProfileName = (TextView) mView.findViewById(R.id.xtvUserNameInFeeds);
            ivImage = (ImageView) mView.findViewById(R.id.xivImage);
            ibLike = (ImageButton) mView.findViewById(R.id.xibLike);
        }

        public void setTitle(String title) {
            TextView tvTitle = (TextView) mView.findViewById(R.id.xtvTitle);
            tvTitle.setText(title);
        }

        public void setDesc(String desc) {
            TextView tvDesc = (TextView) mView.findViewById(R.id.xtvDesc);
            tvDesc.setText(desc);
        }

        public void setImage(Context context, String image) {
            Glide.with(context).load(image).into(ivImage);
        }

        public void setUserImage(Context context, String userImage) {
            Glide.with(context)
                    .load(userImage)
                    .placeholder(R.mipmap.empty_user)
                    .crossFade()
                    .dontAnimate()
                    .into(ivUserImage);
        }

        public void setUserName(String userProfileName) {
            tvUserProfileName.setText(userProfileName);
        }

        public void setDate(String date) {
            TextView tvDate = (TextView) mView.findViewById(R.id.xtvDate);
            tvDate.setText(date);
        }

        public void setLike(final String post_key) {

            final TextView tvLikesCount = (TextView) mView.findViewById(R.id.tvLikesCount);
            likeValue = new ArrayList<>();

            dbLikesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())) {
                        ibLike.setImageResource(R.drawable.like_orange);
                        tvLikesCount.setTextColor(Color.rgb(252, 176, 48));
                    } else {
                        tvLikesCount.setTextColor(Color.rgb(127, 127, 127));
                        ibLike.setImageResource(R.drawable.like_grey);
                    }
                    long likesCount = dataSnapshot.child(post_key).getChildrenCount();
                    if (likesCount == 0) {
                        tvLikesCount.setVisibility(View.GONE);
                    } else {
                        likeValue.add(new setLikes(ivImage, mAuth.getCurrentUser().getUid(),likesCount));
                        Collections.sort(likeValue, new Comparator<setLikes>() {
                            @Override
                            public int compare(setLikes o1, setLikes o2) {
                                if (o1.getLikes() > o2.getLikes()){
                                    return 1;
                                }
                                else if (o1.getLikes() < o2.getLikes()){
                                    return -1;
                                }else{
                                    return 0;
                                }
                            }
                        });
                        Collections.reverse(likeValue);
                        Log.w(TAG, "Maximum Level:"+String.valueOf(likeValue.get(0)));
                        tvLikesCount.setText(likesCount + "");
                        tvLikesCount.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
        public setLikes getLike(){
            if(likeValue.get(0)!=null){
                return likeValue.get(0);
            }
            return null;
        }
        public class setLikes{
            ImageView imageView;
            String id;
            long like;
            public setLikes(ImageView imageView, String id, long like){
                this.imageView = imageView;
                this.id = id;
                this.like = like;
            }

            public void setImageView(ImageView imageView) {
                this.imageView = imageView;
            }
            public ImageView getImageView(ImageView imageView){
                return imageView;
            }

            public void setId(String id ){
                this.id = id;
            }
            public String getId(){
                return id;
            }
            public void setLikeValue(long like){
                this.like = like;
            }
            public long getLikes(){
                return like;
            }
        }
    }

}