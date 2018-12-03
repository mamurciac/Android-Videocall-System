package agora.io.videocall;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.firebase.database.*;
import java.text.*;
import java.util.*;

public class VideochatManagerActivity extends AppCompatActivity{
    private Button newVideochatButton;
    private DatabaseReference databaseReference;
    private LinearLayout videochatList;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videochat_manager);
        initializeElements();
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
                String gameName = "VC " + dateFormat.format(date);
                databaseReference = FirebaseDatabase.getInstance().getReference();
                databaseReference.child("videochats").child(gameName).child("number_users").setValue(1);
                //databaseReference.child("videochats").child(gameName).child("videochat_duration").child("videochat_start_time").setValue(0);
                //databaseReference.child("videochats").child(gameName).child("videochat_duration").child("videochat_finish_time").setValue(0);
                //databaseReference.child("videochats").child(gameName).child("videochat_duration").child("videochat_duration").setValue(0);

                Intent myIntent = new Intent(VideochatManagerActivity.this, VideochatActivity.class);
                myIntent.putExtra("videochatName", gameName);
                VideochatManagerActivity.this.startActivity(myIntent);
            }
        });
        showGameList();
    }

    private void showGameList(){
        DatabaseReference games = databaseReference.child("videochats");
        games.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){
                videochatList.removeAllViews();
                for(DataSnapshot gameSnapshot: dataSnapshot.getChildren()){
                    int players = gameSnapshot.child("number_users").getValue(Integer.class);
                    final String gameName = gameSnapshot.getKey();
                    if(players < 2){
                        Button btn = new Button(VideochatManagerActivity.this);
                        btn.setText(gameName);
                        videochatList.addView(btn);

                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                databaseReference = FirebaseDatabase.getInstance().getReference();
                                databaseReference.child("videochats").child(gameName).child("number_users").setValue(2);
                                Intent myIntent = new Intent(VideochatManagerActivity.this, VideochatActivity.class);
                                myIntent.putExtra("videochatName", gameName);
                                VideochatManagerActivity.this.startActivity(myIntent);
                            }
                        });
                    }
                    Log.d("asdfg", gameName);
                }
            }

            @Override
            public void onCancelled(DatabaseError error){
                Log.w("NOTIFY", "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    public void onBackPressed(){

    }
}