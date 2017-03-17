package com.wbigger.simonsaysfirebase;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";
    Button mButtons[] = new Button[2];
    Animation mAnimationBlink;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtons[0] = (Button) findViewById(R.id.button0);
        mButtons[1] = (Button) findViewById(R.id.button1);

        mAnimationBlink = AnimationUtils.loadAnimation(this,R.anim.blink_animation);

        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference statusRef = database.getReference("status");
        // Read from the database
        statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is: " + value);
                if (value.equals("error")) {
                    animateButtons();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        DatabaseReference ledRef = database.getReference("led");
        // Read from the database
        ledRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is: " + value);
                resetBackgroundColors();
                switch (value) {
                    case "0":
                        setBackgroundColor(mButtons[0],R.color.b0Active);
                        break;
                    case "1":
                        setBackgroundColor(mButtons[1],R.color.b1Active);
                        break;
                    default:
                        // nothing to do
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }




    /** Called when the user touches the button */
    private void resetBackgroundColors() {
        mButtons[0].setBackgroundColor(ContextCompat.getColor(this,R.color.b0Idle));
        mButtons[1].setBackgroundColor(ContextCompat.getColor(this,R.color.b1Idle));
    }

    public void sendButton0Event(View view) {
        Log.d(TAG,"Button 0 Pressed");
    }

    public void sendButton1Event(View view) {
        Log.d(TAG,"Button 1 Pressed");
    }

    public void setBackgroundColor(Button button, int color) {
        button.setBackgroundColor(ContextCompat.getColor(this,color));
    }

    private void animateButtons() {
        mButtons[0].startAnimation(mAnimationBlink);
    }}
