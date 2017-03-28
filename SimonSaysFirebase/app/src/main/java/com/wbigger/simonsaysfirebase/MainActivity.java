package com.wbigger.simonsaysfirebase;

import android.media.AudioManager;
import android.media.MediaPlayer;
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

    private static final int BUTTONS_NUM = 4;
    private final Button mButtons[] = new Button[BUTTONS_NUM];
    private final String mButtonLabels[] = new String[BUTTONS_NUM];
    private final int mButtonTones[] = new int[BUTTONS_NUM];
    private final int mColorsIdle[] = new int[BUTTONS_NUM];
    private final int mColorsActive[] = new int[BUTTONS_NUM];

    private Long mKeyCounter = -1L;

    private static final int STATUS_PLAY = 0;
    private static final int STATUS_LISTENING = 1;
    private static final int STATUS_LOSE = 2;
    private static final int STATUS_WIN = 3;

    private int mStatus = STATUS_LISTENING;

    private Disposable mSubscription;

    private Animation mAnimationLose;
    private Animation mAnimationWin;

    private final ToneGenerator mToneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);


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

        mAnimationLose = AnimationUtils.loadAnimation(this, R.anim.blink_animation);
        mAnimationWin = AnimationUtils.loadAnimation(this, R.anim.happy_animation);

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
                Log.d(TAG, "Firebase status value is: " + value);

                if ((mSubscription != null) && !mSubscription.isDisposed()) {
                    Log.d(TAG, "Disposing subscription");
                    mSubscription.dispose();
                }

                switch (value) {
                    case "win":
                        mStatus = STATUS_WIN;
                        winEvent();
                        break;
                    case "lose":
                        mStatus = STATUS_LOSE;
                        loseEvent();
                        break;
                    case "play":
                        mStatus = STATUS_PLAY;
                        // this is for debug only
                        // in the final version, in "play" should do nothing special
                        // maybe mute the key when pressed
                        Log.d(TAG, "Subscribing play");
                        mSubscription = Observable.interval(1, TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(@NonNull Long aLong) throws Exception {
                                int v = new Random().nextInt(BUTTONS_NUM);
                                Log.d(TAG, "Sending color: " + v);
                                sendLedEventToFirebase(v, aLong);
                            }
                        });
                        //break;
                    case "listening":
                        mStatus = STATUS_LISTENING;
                        mKeyCounter = -1L;
                        break;
                    default:
                        // default to listening
                        mStatus = STATUS_LISTENING;
                }
                Log.d(TAG, "Internal status value is: " + mStatus);
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
                if (mStatus == STATUS_PLAY) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    resetBackgroundColors();
                    try {
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
        if (mStatus == STATUS_LISTENING) {
            for (int idx = 0; idx < BUTTONS_NUM; idx++) {
                View butt = mButtons[idx];
                if (butt == view) {
                    Log.d(TAG, "Button " + mButtonLabels[idx] + " pressed");
                    mKeyCounter++;
                    sendButtonEventToFirebase(idx, mKeyCounter);
                }
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

    private void winEvent() {
        // play sound
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.win);
        mediaPlayer.start();
        // animate buttons
        for (int idx = 0; idx < BUTTONS_NUM; idx++) {
            mButtons[idx].startAnimation(mAnimationWin);
        }
    }

    private void loseEvent() {
        // play sound
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.lose);
        mediaPlayer.start();
        // animate buttons
        for (int idx = 0; idx < BUTTONS_NUM; idx++) {
            mButtons[idx].startAnimation(mAnimationLose);
        }
    }
}
