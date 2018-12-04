package agora.io.videocall;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.*;
import android.view.View;
import android.widget.*;
import com.google.firebase.database.*;
import com.instacart.library.truetime.extensionrx.TrueTimeRx;
import java.text.*;
import java.util.*;
import rx.Observable;
import rx.schedulers.Schedulers;

public class VideochatManagerActivity extends AppCompatActivity{
    private Observable<Date> dateObservable;
    private long startVideochatTime;

    private Button newVideochatButton;
    private DatabaseReference databaseReference;
    private LinearLayout videochatList;

    private static final String LOG_TAG = VideochatManagerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videochat_manager);
        initializeElements();

        //Peticiones a servidores NTP para obtener la hora
        List<String> ntpHosts = Arrays.asList("co.pool.ntp.org", "time.google.com");
        dateObservable = TrueTimeRx.build().initialize(ntpHosts).subscribeOn(Schedulers.io());
    }

    private void initializeElements(){
        newVideochatButton = findViewById(R.id.new_online_game);
        videochatList = (LinearLayout) findViewById(R.id.ll_btns);
        databaseReference = FirebaseDatabase.getInstance().getReference();

        newVideochatButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Date date = new Date();
                DateFormat dateFormat = new SimpleDateFormat("dd_MM_yy_hh_mm_ss");
                String videochatName = "VC " + dateFormat.format(date);
                databaseReference = FirebaseDatabase.getInstance().getReference();
                databaseReference.child("videochats").child(videochatName).child("number_users").setValue(1);
                startVideochatTime = getTimeNow(videochatName);

                Intent myIntent = new Intent(VideochatManagerActivity.this, VideochatActivity.class);
                myIntent.putExtra("videochatName", videochatName);
                myIntent.putExtra("startVideochatDate", startVideochatTime);
                VideochatManagerActivity.this.startActivity(myIntent);
            }
        });
        showGameList();
    }

    private void showGameList(){
        DatabaseReference videochats = databaseReference.child("videochats");
        videochats.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){
                videochatList.removeAllViews();
                for(DataSnapshot gameSnapshot: dataSnapshot.getChildren()){
                    int numberUsers = gameSnapshot.child("number_users").getValue(Integer.class);
                    final String videochatName = gameSnapshot.getKey();
                    if(numberUsers == 1){
                        Resources resources = getResources();
                        float leftMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,13, resources.getDisplayMetrics());
                        float topMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,13, resources.getDisplayMetrics());
                        float rightMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,13, resources.getDisplayMetrics());
                        float bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,13, resources.getDisplayMetrics());

                        Button videochatButton = new Button(VideochatManagerActivity.this);
                        Drawable drawableResourceId = getResources().getDrawable(R.drawable.blue_button);
                        videochatButton.setBackground(drawableResourceId);
                        videochatButton.setTextColor(Color.WHITE);
                        LinearLayout.LayoutParams buttonLayout = new LinearLayout.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
                        buttonLayout.setMargins(Math.round(leftMargin), Math.round(topMargin), Math.round(rightMargin), Math.round(bottomMargin));
                        videochatButton.setLayoutParams(buttonLayout);
                        videochatButton.setText(videochatName);
                        videochatList.addView(videochatButton);

                        videochatButton.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View view){
                                databaseReference = FirebaseDatabase.getInstance().getReference();
                                databaseReference.child("videochats").child(videochatName).child("number_users").setValue(2);
                                Intent myIntent = new Intent(VideochatManagerActivity.this, VideochatActivity.class);
                                myIntent.putExtra("videochatName", videochatName);
                                VideochatManagerActivity.this.startActivity(myIntent);
                            }
                        });
                    }
                    Log.d("asdfg", videochatName);
                }
            }

            @Override
            public void onCancelled(DatabaseError error){
                Log.w("NOTIFY", "Failed to read value.", error.toException());
            }
        });
    }

    private long getTimeNow(String videochatName){
        dateObservable.subscribe(date -> {
            databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("videochats").child(videochatName).child("videochat_duration").child("videochat_start_time").setValue(date.toString());
            Log.v(LOG_TAG, "TrueTime was initialized and we have a time: " + date);
        }, throwable -> {
            throwable.printStackTrace();
        });
        return dateObservable.toBlocking().first().getTime();
    }

    @Override
    public void onBackPressed(){

    }
}