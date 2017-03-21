package com.wbigger.simonsaysfirebase;

import android.media.AudioManager;
import android.media.ToneGenerator;
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

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {


    static final String TAG = "MainActivity";

    static final int BUTTONS_NUM = 4;
    Button mButtons[] = new Button[BUTTONS_NUM];
    String mButtonLabels[] = new String[BUTTONS_NUM];
    int mButtonTones[] = new int[BUTTONS_NUM];
    int mColorsIdle[] = new int[BUTTONS_NUM];
    int mColorsActive[] = new int[BUTTONS_NUM];

    Long mKeyCounter = -1L;

    private Disposable subscription;

    Animation mAnimationBlink;

    ToneGenerator mToneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtons[0] = (Button) findViewById(R.id.button0);
        mButtons[1] = (Button) findViewById(R.id.button1);
        mButtons[2] = (Button) findViewById(R.id.button2);
        mButtons[3] = (Button) findViewById(R.id.button3);

        mButtonLabels[0] = getString(R.string.b0Label);
        mButtonLabels[1] = getString(R.string.b1Label);
        mButtonLabels[2] = getString(R.string.b2Label);
        mButtonLabels[3] = getString(R.string.b3Label);

        mButtonTones[0] = ToneGenerator.TONE_DTMF_0;
        mButtonTones[1] = ToneGenerator.TONE_DTMF_1;
        mButtonTones[2] = ToneGenerator.TONE_DTMF_2;
        mButtonTones[3] = ToneGenerator.TONE_DTMF_3;

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

                if ((subscription!=null)&& !subscription.isDisposed()) {
                    subscription.dispose();
                }

                switch (value) {
                    case "error":
                        animateButtons();
                        break;
                    case "play":
                        // this is for debug only
                        // in the final version, in "play" should do nothing special
                        // maybe mute the key when pressed
                        subscription = Observable.interval(1, TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(@NonNull Long aLong) throws Exception {
                                int v = new Random().nextInt(BUTTONS_NUM);
                                Log.d(TAG, "Sending color: " + v);
                                sendLedEventToFirebase(v, aLong);
                            }
                        });
                    case "listening":
                        mKeyCounter = -1L;
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
                    String value = dataSnapshot.child("color").getValue(String.class);
                    Log.d(TAG, "Color value is: " + value);
                    // find the index of the button with the desired label
                    int btnIdx = java.util.Arrays.asList(mButtonLabels).indexOf(value);
                    if ((btnIdx >= 0) && (btnIdx < BUTTONS_NUM)) {
                        setBackgroundColor(mButtons[btnIdx], mColorsActive[btnIdx]);
                        playSound(btnIdx);
                    } else {
                        Log.w(TAG, "Firebase index is out of bound.");
                    }

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
                mKeyCounter++;
                sendButtonEventToFirebase(idx,mKeyCounter);
            }
        }

    }

    public void sendButtonEventToFirebase(int btnIdx,Long counter) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference keyRef = database.getReference("key");
        keyRef.setValue(new SimonEvent(mButtonLabels[btnIdx],counter));

        playSound(btnIdx);
    }

    public void sendLedEventToFirebase(int btnIdx, Long counter) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ledRef = database.getReference("led");
        ledRef.setValue(new SimonEvent(mButtonLabels[btnIdx],counter));
    }

    public void setBackgroundColor(Button button, int color) {
        button.setBackgroundColor(ContextCompat.getColor(this, color));
    }

    private void playSound(int btnIdx) {
        mToneGen.startTone(mButtonTones[btnIdx], 200);
    }

    private void animateButtons() {
        for (int idx = 0; idx < BUTTONS_NUM; idx++) {
            mButtons[idx].startAnimation(mAnimationBlink);
        }
    }
}
