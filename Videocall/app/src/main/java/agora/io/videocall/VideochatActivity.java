package agora.io.videocall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.firebase.database.*;
import io.agora.rtc.*;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration; //Use it for >= 2.3.0 versions

public class VideochatActivity extends AppCompatActivity{
    private String gameName;
    private DatabaseReference mDatabase;
    private int numberUsers;

    private static final String LOG_TAG = VideochatActivity.class.getSimpleName();
    private static final int PERMISSION_REQ_ID = 22;

    private TextView mTextView;

    //The WRITE_EXTERNAL_STORAGE permission isn't mandatory for Agora RTC SDK, just incase if you wanna save logs to external sdcard
    private static final String[] REQUESTED_PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private RtcEngine rtcEngine;
    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler(){
        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed){
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason){
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    onRemoteUserLeft();
                }
            });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean muted){
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    onRemoteUserVideoMuted(uid, muted);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videochat);
        mTextView = (TextView) findViewById(R.id.quick_tips_when_use_agora_sdk);

        if(checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) && checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) && checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)){
            initAgoraEngineAndJoinChannel();
            Bundle b = getIntent().getExtras();
            gameName = b.getString("videochatName");
            mDatabase = FirebaseDatabase.getInstance().getReference();
            addDataBaseListener();
        }
    }

    private void addDataBaseListener(){
        DatabaseReference referenceNumberUsers = mDatabase.child("videochats").child(gameName).child("number_users");

        referenceNumberUsers.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){
                numberUsers = dataSnapshot.getValue(Integer.class);
                checkWinner();
            }
            @Override
            public void onCancelled(DatabaseError error){
                Log.w("NOTIFI", "Failed to read value.", error.toException());
            }
        });
    }

    void checkWinner(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if(numberUsers == 1){
            mTextView.setText("Esperando a otro Jugador");
        }
    }

    private void initAgoraEngineAndJoinChannel(){
        initializeAgoraEngine();
        setupVideoProfile();
        setupLocalVideo();
        joinChannel();
    }

    public boolean checkSelfPermission(String permission, int requestCode){
        Log.i(LOG_TAG,"checkSelfPermission " + permission + " " + requestCode);
        if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        Log.i(LOG_TAG,"onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch(requestCode){
            case PERMISSION_REQ_ID: {
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED || grantResults[2] != PackageManager.PERMISSION_GRANTED){
                    showLongToast("Need permissions " + Manifest.permission.RECORD_AUDIO + "/" + Manifest.permission.CAMERA + "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    finish();
                    break;
                }

                initAgoraEngineAndJoinChannel();
                break;
            }
        }
    }

    public final void showLongToast(final String msg){
        this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        leaveChannel();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("videochats").child(gameName).child("number_users").setValue(999);
        RtcEngine.destroy();
        rtcEngine = null;
    }

    public void onLocalVideoMuteClicked(View view){
        ImageView imageView = (ImageView) view;
        if(imageView.isSelected()){
            imageView.setSelected(false);
            imageView.clearColorFilter();
        }else{
            imageView.setSelected(true);
            imageView.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        rtcEngine.muteLocalVideoStream(imageView.isSelected());
        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
        surfaceView.setZOrderMediaOverlay(!imageView.isSelected());
        surfaceView.setVisibility(imageView.isSelected() ? View.GONE : View.VISIBLE);
    }

    public void onLocalAudioMuteClicked(View view){
        ImageView imageView = (ImageView) view;
        if(imageView.isSelected()){
            imageView.setSelected(false);
            imageView.clearColorFilter();
        }else{
            imageView.setSelected(true);
            imageView.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }
        rtcEngine.muteLocalAudioStream(imageView.isSelected());
    }

    public void onSwitchCameraClicked(View view){
        rtcEngine.switchCamera();
    }

    public void onEncCallClicked(View view){
        finish();
    }

    private void initializeAgoraEngine(){
        try{
            rtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), rtcEventHandler);
        }catch(Exception e){
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoProfile(){
        rtcEngine.enableVideo();
        //rtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, false); //Use this for < 2.3.0 versions
        rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x360, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15, VideoEncoderConfiguration.STANDARD_BITRATE, VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo(){
        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        rtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT,0));
    }

    private void joinChannel(){
        rtcEngine.joinChannel(null,"demoChannel1","Extra Optional Data",0); //If you don't specify the uid, Agora will generate the uid for you
    }

    private void setupRemoteVideo(int uid){
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);
        if(container.getChildCount() >= 1){
            return;
        }

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        rtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        surfaceView.setTag(uid); //It is for mark purpose
        View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); //It is optional to improve User Interface
        tipMsg.setVisibility(View.GONE);
    }

    private void leaveChannel(){
        rtcEngine.leaveChannel();
    }

    private void onRemoteUserLeft(){
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);
        container.removeAllViews();
        View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); //It is optional to improve User Interface
        tipMsg.setVisibility(View.VISIBLE);
    }

    private void onRemoteUserVideoMuted(int uid, boolean muted){
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);
        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
        Object tag = surfaceView.getTag();
        if(tag != null && (Integer) tag == uid){
            surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
        }
    }
}