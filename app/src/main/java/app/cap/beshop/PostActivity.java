package app.cap.beshop;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.ActionCodeResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PostActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_STORAGE=101;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA=102;
    private static final String TAG = "PostActivity";
    private static final int PICK_IMAGE = 2;
    private static final int PICK_CAMERA = 1;
    private static final int CROP_CAMERA = 3;
    ImageButton ibImage;
    Button bnPost;
    EditText etTitle, etDesc;
    Uri resultUri;
    String absolutepath;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;
    private ProgressDialog mProgressDialog;
    private FirebaseUser mUser;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);


        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                int PERMISSION_ALL = 1;
                String [] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }

        ibImage = (ImageButton) findViewById(R.id.xibImage);
        bnPost = (Button) findViewById(R.id.xbnPost);
        etTitle = (EditText) findViewById(R.id.xetTitle);
        etDesc = (EditText) findViewById(R.id.xetDesc);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("feeds");
        mProgressDialog = new ProgressDialog(this);

        ibImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(PostActivity.this);
                builder.setTitle("업로드할 이미지 선택");
                builder.setPositiveButton("갤러리에서 선택", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which){

                        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        pickImageIntent.setType("image/*");
                        startActivityForResult(pickImageIntent, PICK_IMAGE);
                        }
                    });
                    builder.setNegativeButton("사진 촬영",new DialogInterface.OnClickListener(){
                       @Override
                        public void onClick(DialogInterface dialog, int which) {

                           Intent pickcamIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                           Log.i(TAG,"인텐트 넘기기 성공");
                           String url = "tmp_"+String.valueOf(System.currentTimeMillis())+".jpg";
                           resultUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),url));
                           pickcamIntent.putExtra(MediaStore.EXTRA_OUTPUT, resultUri/*MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())*/);
                           //pickcamIntent.setDataAndType(resultUri, "image/*");
                           //pickcamIntent.putExtra("return-data",true);
                           startActivityForResult(pickcamIntent, PICK_CAMERA);
                       }
                    });
                builder.setNeutralButton("닫기",new DialogInterface.OnClickListener(){
                   @Override
                    public void onClick(DialogInterface dialog, int which){

                   }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
        }
    });

        bnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postToFirebase();
            }
        });
    }

    private void postToFirebase() {

        final String title = etTitle.getText().toString().trim();
        final String desc = etDesc.getText().toString().trim();
        mProgressDialog.setTitle("등록 중입니다....");
        mProgressDialog.setCanceledOnTouchOutside(false);

        if(!title.isEmpty() && !desc.isEmpty() && resultUri!=null) {
            mProgressDialog.show();
            StorageReference imageRef = mStorageRef.child("Betriever_feeeds").child(resultUri.getLastPathSegment());
            imageRef.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                    Toast.makeText(PostActivity.this, "성공적으로 업로드 되었습니다.", Toast.LENGTH_SHORT).show();

                    DatabaseReference mchildRef = mDatabaseRef.push();
                    mchildRef.child("title").setValue(title);
                    mchildRef.child("desc").setValue(desc);
                    mchildRef.child("u_id").setValue(mUser.getUid());
                    String date = getCurrentDateTime();
                    mchildRef.child("date").setValue(date);
                    mchildRef.child("image").setValue(downloadUrl.toString());

                    mProgressDialog.dismiss();

                    Intent intent = new Intent(PostActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    mProgressDialog.dismiss();
                    Toast.makeText(PostActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    mProgressDialog.setMessage((int)progress+"% 등록 중...");
                }
            });

        }
        else
            {
            Toast.makeText(this, "제목과 설명을 모두 입력하고, 사진을 추가하십시오.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(20, 10)
                    .start(this);
        }
        if (requestCode == PICK_CAMERA)
        {
                if (resultCode != RESULT_OK)
                {
                    return;
                }
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/betriever/"+System.currentTimeMillis()+".jpg";
                        ibImage.setImageURI(resultUri);
                        absolutepath = path;

            }
            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    resultUri = result.getUri();
                    ibImage.setImageURI(resultUri);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
            }
        }

    private void storeCrop(Bitmap bitmap, String filePath) {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Betriever";
        File directory = new File(dirPath);

        if (!directory.exists()){
            directory.mkdir();

        File coFile = new File(filePath);
        BufferedOutputStream out = null;

        try {
            coFile.createNewFile();
            out= new BufferedOutputStream(new FileOutputStream(coFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(coFile)));

            out.flush();
            out.close();

            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }

    public String getCurrentDateTime() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String getTime = sdf.format(date);
        return getTime;
    }
}
