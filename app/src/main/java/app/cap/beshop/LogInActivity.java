package app.cap.beshop;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.estimote.sdk.SystemRequirementsChecker;

public class LogInActivity extends AppCompatActivity {

    private static final String TAG = "LogInActivity";
    private static int RC_SIGN_IN = 5;
    private TextView bnNewAccount;
    private EditText etEmail, etPassword;
    private Button bnLogIn;
    private SignInButton bnGoogleSignIn;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mDatabaseRef;
    private GoogleApiClient mGoogleApiClient;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)!=PackageManager.PERMISSION_GRANTED){
                int PERMISSION_ALL = 1;
                String[] PERMISSIONS = {Manifest.permission.GET_ACCOUNTS};
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users");
        mDatabaseRef.keepSynced(true);

        etEmail = (EditText) findViewById(R.id.xetLogInEmail);
        etPassword = (EditText) findViewById(R.id.xetLogInPassword);
        bnLogIn = (Button) findViewById(R.id.xbnLogIn);
        bnNewAccount = (TextView) findViewById(R.id.xbnLogInNewAccount);
        bnGoogleSignIn = (SignInButton) findViewById(R.id.xbnGoogleSignIn);
        mProgressDialog = new ProgressDialog(this);

        bnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogIn();
            }
        });

        bnNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogInActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(LogInActivity.this, "로그인의 문제가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        bnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.setMessage("로그인 중...");
                mProgressDialog.show();
                signIn();
                mProgressDialog.dismiss();
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    Intent intent = new Intent(LogInActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    private void doLogIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!email.isEmpty() && !password.isEmpty()) {

            mProgressDialog.setMessage("로그인 중...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();

            mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if (task.isSuccessful()){
                        checkUserExists();
                    }
                    else{
                        Toast.makeText(LogInActivity.this, "정확한 이메일과 비밀번호를 입력해주십시오.", Toast.LENGTH_SHORT).show();
                    }
                    mProgressDialog.dismiss();
                }
            });
        }
    }

    private void checkUserExists() {
        final String user_id = mFirebaseAuth.getCurrentUser().getUid();

        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(user_id)) {

                    mProgressDialog.dismiss();
                    Intent intent = new Intent(LogInActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LogInActivity.this, "입력한 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // ---------------------- Google Sign in -----------------------------//

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mProgressDialog.setMessage("로그인 중...");
        mProgressDialog.show();
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
                Toast.makeText(LogInActivity.this,"구글 로그인 성공",Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(LogInActivity.this,"알 수 없는 문제 발생...",Toast.LENGTH_SHORT).show();
                Log.w(TAG,"Google failed:"+ result +","+result.getStatus()+","+result.getSignInAccount() );
            }
        }
        mProgressDialog.dismiss();
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
        Log.w(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.w(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LogInActivity.this, "구글 인증에 실패했습니다.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            DatabaseReference userDbRef = mDatabaseRef.child(mFirebaseAuth.getCurrentUser().getUid());
                            userDbRef.child("email").setValue(acct.getEmail());
                            userDbRef.child("name").setValue(acct.getDisplayName());
                            userDbRef.child("profile_image").setValue(acct.getPhotoUrl().toString());
                        }

                        // ...
                    }
                });
        mProgressDialog.dismiss();
    }

}
