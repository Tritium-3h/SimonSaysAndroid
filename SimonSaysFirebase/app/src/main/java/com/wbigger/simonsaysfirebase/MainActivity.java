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
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {



    static final String TAG = "MainActivity";

    static final int BUTTONS_NUM = 4;
    Button mButtons[] = new Button[BUTTONS_NUM];
    int mColorsIdle[] = new int[BUTTONS_NUM];
    int mColorsActive[] = new int[BUTTONS_NUM];

    Long lastCounter = -1L;

    Animation mAnimationBlink;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtons[0] = (Button) findViewById(R.id.button0);
        mButtons[1] = (Button) findViewById(R.id.button1);
        mButtons[2] = (Button) findViewById(R.id.button2);
        mButtons[3] = (Button) findViewById(R.id.button3);

        mColorsIdle[0] = R.color.b0Idle;
        mColorsIdle[1] = R.color.b1Idle;
        mColorsIdle[2] = R.color.b2Idle;
        mColorsIdle[3] = R.color.b3Idle;

        mColorsActive[0] = R.color.b0Active;
        mColorsActive[1] = R.color.b1Active;
        mColorsActive[2] = R.color.b2Active;
        mColorsActive[3] = R.color.b3Active;

        mAnimationBlink = AnimationUtils.loadAnimation(this, R.anim.blink_animation);

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
                Log.d(TAG, "Status value is: " + value);
                switch (value) {
                    case "error":
                        animateButtons();
                        break;
                    case "play":
                        Observable.interval(1,TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(@NonNull Long aLong) throws Exception {
                                int v = new Random().nextInt(BUTTONS_NUM);
                                Log.d(TAG, "Sending color: "+v);
                                sendLedEventToFirebase(v,aLong);
                            }
                        });

                        break;
                    default:
                        //do nothing
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
                resetBackgroundColors();
                try {
                    Long newCounter = Long.valueOf(dataSnapshot.child("counter").getValue(String.class));
                    if (!newCounter.equals(lastCounter)) {
                        String value = dataSnapshot.child("color").getValue(String.class);
                        Log.d(TAG, "Color value is: " + value);
                        int valueInt = Integer.valueOf(value);
                        if ((valueInt >= 0) && (valueInt < BUTTONS_NUM)) {
                            setBackgroundColor(mButtons[valueInt], mColorsActive[valueInt]);
                        } else {
                            Log.w(TAG, "Firebase index is out of bound.");
                        }
                    }
                    lastCounter = newCounter;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Firebase string has to be a number.", e);
                } catch (DatabaseException e) {
                    Log.w(TAG, "Firebase values has to be strings.", e);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }


    /**
     * Called when the user touches the button
     */
    private void resetBackgroundColors() {
        for (int idx = 0; idx < BUTTONS_NUM; idx++) {
            mButtons[idx].setBackgroundColor(ContextCompat.getColor(this, mColorsIdle[idx]));
        }
    }

    public void sendButtonEvent(View view) {
        for (int idx = 0; idx < BUTTONS_NUM; idx++) {
            View butt = mButtons[idx];
            if (butt == view) {
                Log.d(TAG, butt.toString() + " Pressed");
                sendButtonEventToFirebase(idx);
            }
        }

    }

    public void sendButtonEventToFirebase(int btnIdx) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference keyRef = database.getReference("key");
        keyRef.setValue(String.valueOf(btnIdx));

    }

    public void sendLedEventToFirebase(int btnIdx, Long counter) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference keyRef = database.getReference("led");
        keyRef.child("counter").setValue(String.valueOf(counter));
        keyRef.child("color").setValue(String.valueOf(btnIdx));

    }

    public void setBackgroundColor(Button button, int color) {
        button.setBackgroundColor(ContextCompat.getColor(this, color));
    }

    private void animateButtons() {
        for (int idx = 0; idx < BUTTONS_NUM; idx++) {
            mButtons[idx].startAnimation(mAnimationBlink);
        }
    }
}
