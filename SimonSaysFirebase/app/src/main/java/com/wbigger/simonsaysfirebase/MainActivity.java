package com.wbigger.simonsaysfirebase;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int blueIdle = ContextCompat.getColor(this,android.R.color.holo_blue_light);
        int bluePressed = ContextCompat.getColor(this,android.R.color.holo_blue_dark);

        int bluePressed = ContextCompat.getColor(this,android.R.color.holo_blue_dark);
        int bluePressed = ContextCompat.getColor(this,android.R.color.holo_blue_dark);

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
                switch (value) {
                    case "green": darkenGreenButtonBackground();
                        break;
                    case "blue": darkenBlueButtonBackground();
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
    public void sendBlueButtonEvent(View view) {
        Log.d(TAG,"Blue Button Pressed");
    }

    public void sendGreenButtonEvent(View view) {
        Log.d(TAG,"Green Button Pressed");
    }

    public void darkenBlueButtonBackground() {
        Button button = (Button) findViewById(R.id.buttonBlue);
        button.setBackgroundColor(Color.BLUE);
    }

    public void darkenGreenButtonBackground() {
        Button button = (Button) findViewById(R.id.buttonGreen);

        button.setBackgroundColor();
    }
}
