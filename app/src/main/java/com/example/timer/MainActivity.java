package com.example.timer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private long remainingTime = 0;
    private long endTime;
    private CountDownTimer cdTimer;
    private boolean state;
    private boolean trigger = true;
    private CountDownTimer warning_timer;
    private int frag;
    private MediaPlayer alarm ;

    private Button start;
    private Button pause;
    private Button reset;
    private TextView timer;
    private TextView fragments;
    private EditText hours;
    private EditText minuets;
    private EditText seconds;
    private CheckBox alwaysOn;
    private CheckBox high_perception;
    private Switch alarm_trig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUI();
        alarm = MediaPlayer.create(this,R.raw.analog_watch_alarm_daniel_simion);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start_timer();
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });
        alwaysOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alwaysOn.isChecked()) {
                    // preventing screen from going off
                    Toast.makeText(getApplicationContext(), "toggled to always on mode", Toast.LENGTH_SHORT).show();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    // turning screen back to its normal mode
                    Toast.makeText(getApplicationContext(), "toggled to normal mode", Toast.LENGTH_SHORT).show();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
        high_perception.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (high_perception.isChecked()) {
                    fragments.setVisibility(View.VISIBLE);
                } else {
                    fragments.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // method to handle any touches on main layout
        closeKeyBoard();
        if(alarm.isPlaying()){
            alarm.pause();
            alarm.seekTo(0);
        }
        return super.onTouchEvent(event);

    }

    @Override
    public void onStop() {
        super.onStop();
        // bunch of code to save data of running timer even if app is closed
        SharedPreferences pref = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong("rem", remainingTime);
        editor.putInt("frag", frag);
        editor.putLong("end", endTime);
        editor.putBoolean("state", state);
        editor.putBoolean("trig", trigger);
        editor.apply();

        if (cdTimer != null) {
            cdTimer.cancel();
            cdTimer = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // retrieving saved data
        SharedPreferences pref = getSharedPreferences("prefs", MODE_PRIVATE);
        remainingTime = pref.getLong("rem", 0);
        frag = pref.getInt("frag", 0);
        endTime = pref.getLong("end", 0);
        state = pref.getBoolean("state", false);
        trigger = pref.getBoolean("trig", true);
        /*Log.v("state", String.valueOf(state));
        Log.v("remaining from on start",String.valueOf(remainingTime));*/

        if (state) {
            // test if timer is able to continue in background or not
            remainingTime = endTime - System.currentTimeMillis();
            if (remainingTime < 0) {
                trigger = true;
                state = false;
                remainingTime = 0;
                frag = 0;
                displayTimer(frag);
                start.setText(R.string.start);
                start.setEnabled(true);
                pause.setEnabled(false);
                reset.setEnabled(false);
                hours.setEnabled(false);
                minuets.setEnabled(false);
                seconds.setEnabled(false);
                hours.setEnabled(true);
                minuets.setEnabled(true);
                seconds.setEnabled(true);
                hours.setText("");
                minuets.setText("");
                seconds.setText("");
                if (cdTimer != null) {
                    cdTimer.cancel();
                    cdTimer = null;
                }
                if (warning_timer != null) {
                    warning_timer.cancel();
                }
            } else {
                warning_start(12000 - remainingTime);
                start_timer();
            }
        } else {
            displayTimer(frag);
            trigger = true;
            if (warning_timer != null) {
                warning_timer.cancel();
            }
            if (!timer.getText().toString().equals("00:00:00")) {
                start.setText(R.string.resume);
                reset.setEnabled(true);
                hours.setEnabled(false);
                minuets.setEnabled(false);
                seconds.setEnabled(false);
            } else {
                start.setText(R.string.start);
            }
        }
    }

    public void start_timer() {
        //Log.v("start", String.valueOf(remainingTime));
        if (remainingTime == 0) { // it didn't come from another timer
            remainingTime = translateToMilli(); // getting input from user and storing it in remainingTime
            //Log.v("user input","taking user input");
        }
        // testing user input
        if (remainingTime < 1000) {
            Toast.makeText(this, "at least one field mustn't be empty", Toast.LENGTH_SHORT).show();
            //Log.v("remaining", String.valueOf(remainingTime));
        } else {
            state = true;
            pause.setEnabled(true);
            start.setEnabled(false);
            start.setText(R.string.resume);
            reset.setEnabled(false);
            hours.setEnabled(false);
            minuets.setEnabled(false);
            seconds.setEnabled(false);
            hours.setText("");
            minuets.setText("");
            seconds.setText("");
            endTime = System.currentTimeMillis() + remainingTime; // detecting value of end time according to the system
            // initializing timer
            cdTimer = new CountDownTimer(remainingTime, 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                    remainingTime = millisUntilFinished;
                    frag = (int) (remainingTime / 10);
                    frag = getFrag(frag);
                    if (remainingTime <= 11000) {
                        if (trigger) {
                            trigger = false;
                            warning_start(0);
                        }
                    }
                    displayTimer(frag);

                    /*if(remainingTime%1000!=0){
                        Log.v(String.valueOf(remainingTime/1000),String.valueOf(frag));
                    }*/
                }

                @Override
                public void onFinish() {
                    if(alarm_trig.isChecked()){
                        Toast.makeText(getApplicationContext(),"touch anywhere to stop the alarm",Toast.LENGTH_LONG).show();
                        alarm.start();
                    }
                    trigger = true;
                    state = false;
                    remainingTime = 0;
                    frag = 0;
                    pause.setEnabled(false);
                    start.setEnabled(true);
                    start.setText(R.string.start);
                    reset.setEnabled(false);
                    hours.setEnabled(true);
                    minuets.setEnabled(true);
                    seconds.setEnabled(true);
                }
            }.start();
        }
    }

    public void pause() {
        state = false;
        cdTimer.cancel();
        cdTimer = null;
        if (warning_timer != null) {
            warning_timer.cancel();
            trigger = true;
        }
        pause.setEnabled(false);
        start.setEnabled(true);
        reset.setEnabled(true);
    }

    public void reset() {
        if (cdTimer != null) {
            cdTimer.cancel();
            cdTimer = null;
        }
        fragments.setTextColor(getResources().getColor(R.color.black));
        timer.setTextColor(getResources().getColor(R.color.black));
        timer.setText("00:00:00");
        fragments.setText("00");
        state = false;
        trigger = true;
        remainingTime = 0;
        frag = 0;
        reset.setEnabled(false);
        pause.setEnabled(false);
        start.setEnabled(true);
        start.setText(R.string.start);
        hours.setEnabled(true);
        minuets.setEnabled(true);
        seconds.setEnabled(true);
        hours.setText("");
        minuets.setText("");
        seconds.setText("");
    }

    public void setupUI() {
        start = findViewById(R.id.start);
        pause = findViewById(R.id.pause);
        reset = findViewById(R.id.reset);
        timer = findViewById(R.id.timer);
        fragments = findViewById(R.id.fragment);
        alwaysOn = findViewById(R.id.always_on);
        high_perception = findViewById(R.id.high_perception);
        alarm_trig = findViewById(R.id.alarm);
        hours = findViewById(R.id.hour);
        minuets = findViewById(R.id.min);
        seconds = findViewById(R.id.sec);
        // to make password always shown
        hours.setTransformationMethod(null);
        minuets.setTransformationMethod(null);
        seconds.setTransformationMethod(null);
    }

    // helper methods
    private long translateToMilli() {
        // to translate user input to millis
        int hour;
        int minutes;
        int second;

        if (hours.getText().toString().equals("")) {
            hour = 0;
        } else {
            hour = Integer.parseInt(hours.getText().toString());
        }

        if (minuets.getText().toString().equals("")) {
            minutes = 0;
        } else {
            minutes = Integer.parseInt(minuets.getText().toString());
        }

        if (seconds.getText().toString().equals("")) {
            second = 0;
        } else {
            second = Integer.parseInt(seconds.getText().toString());
        }

        long total = hour * 60 * 60 * 1000 + minutes * 60 * 1000 + second * 1000;

        return total;
    }

    private void displayTimer(int frag) {
        // method to display current value of timer
        String timeFormat;

        int hours = (int) remainingTime / 1000 / 60 / 60;

        int minutes;
        if (hours > 0) {
            minutes = (int) remainingTime / 1000 / 60 - hours * 60;
        } else {
            minutes = (int) remainingTime / 1000 / 60;
        }

        int sec = (int) remainingTime / 1000 % 60;

        fragments.setText(String.valueOf(frag));

        timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, sec);

        timer.setText(timeFormat);
    }

    private int getFrag(int num) {
        // method to cut out frag to count from 100 to 0
        int result = 0;
        String temp = String.valueOf(num);
        if (num > 100) {
            temp = temp.substring(temp.length() - 2);
            //System.out.println(temp);
            result = Integer.parseInt(temp);
        }
        return result;
    }

    private void warning_start(long diff) {
        // method to trigger alarm
        if (diff == 0) {
            diff = 9800;
        }
        warning_timer = new CountDownTimer(diff, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (remainingTime == 0) {
                    warning_timer.cancel();
                    reset();
                } else {
                    warning();
                }
                Log.v("warning on tic", String.valueOf(remainingTime));
            }

            @Override
            public void onFinish() {
                fragments.setTextColor(getResources().getColor(R.color.black));
                timer.setTextColor(getResources().getColor(R.color.black));
                Log.v("warning finished", "warning");
                warning_timer.cancel();
            }
        }.start();
    }

    private void warning() {
        // method for swapping alarm colors
        if (timer.getCurrentTextColor() == getResources().getColor(R.color.colorPrimaryDark) ||
                timer.getCurrentTextColor() == getResources().getColor(R.color.black)) {
            timer.setTextColor(getResources().getColor(R.color.warning));
            fragments.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        } else {
            timer.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
            fragments.setTextColor(getResources().getColor(R.color.warning));
        }


    }

    private void closeKeyBoard(){
        //method to hide keyboard
        View view = this.getCurrentFocus();
        if (view != null){
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}