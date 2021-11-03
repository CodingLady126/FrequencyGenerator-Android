package com.alexey.tonegenerator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karlotoy.perfectune.instance.PerfectTune;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    AudioManager audioManager;
    Button btnRejuvenate, btnDeStress, btnDetox, btnPain, btnJoy, btnMicro, btnOrgan, btnCustom, btnPlayAndStop, btnSettings, btnLogout;
    IndicatorSeekBar seekBar, volumnSeekBar;
    PerfectTune perfectTune;
    TextView tvVolumn, tvCurFreq;
    boolean bPlaying = false;

    int nCurrentProgramId = -1;
    ArrayList<Program> currentProgram = null;

    Handler handler = new Handler();

    Thread thread;
    PrimeRun p;
    String[] programs = { "Rejuvenate", "De-Stress", "Detox", "Pain", "Joy", "Micro Organisms", "Organ Support", "Custom"};
    ArrayList<Program> Rejuvenate, Destress, Detox, Pain, Joy, MicroOrganisms, OrganSupport, Custom;
    SharedPreferences prefs;

    String TAG = "aa";

    class PrimeRun implements Runnable {
        private Object mPauseLock;
        int progressStatus = 0;
        int totalSeconds = 0;
        int nCurFreqId = 0;
        double nFreqInterval = 0;
        double nCurFreq = 0;
        ArrayList<Program> freqArray;

        int maxFreq = 0;
        int minFreq = 0;

        private boolean mPaused;
        public boolean mFinished;

        PrimeRun(int totalSeconds, ArrayList<Program> freqArray) {
            this.totalSeconds = totalSeconds;
            this.freqArray = freqArray;

            maxFreq = freqArray.get(nCurFreqId).maxFreq;
            minFreq = freqArray.get(nCurFreqId).minFreq;
            maxFreq = maxFreq == -1 ? minFreq : maxFreq;
            minFreq = minFreq == -1 ? maxFreq : minFreq;

            nCurFreq = minFreq;

            nFreqInterval = Float.valueOf((maxFreq - minFreq)) / Float.valueOf(freqArray.get(nCurFreqId).minutes) / Float.valueOf(600) ;

            mPauseLock = new Object();
            mPaused = false;
            mFinished = false;
        }
        /**
         * Call this on pause.
         */
        public void onPause() {
            synchronized (mPauseLock) {
                perfectTune.stopTune();
                mPaused = true;

                handler.post(new Runnable() {
                    public void run() {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            }
        }

        public void onChangeProgress(int progress) {
            progressStatus = progress * 10;

            nCurFreqId = 0;
            maxFreq = freqArray.get(nCurFreqId).maxFreq;
            minFreq = freqArray.get(nCurFreqId).minFreq;
            maxFreq = maxFreq == -1 ? minFreq : maxFreq;
            minFreq = minFreq == -1 ? maxFreq : minFreq;

            nFreqInterval = Float.valueOf((maxFreq - minFreq)) / Float.valueOf(freqArray.get(nCurFreqId).minutes) / Float.valueOf(600) ;

            nCurFreq = minFreq + nFreqInterval * progressStatus;

            int nSumMins = 0;
            for (int i = 0; i < freqArray.size() - 1; i++) {
                nSumMins += freqArray.get(i).minutes;
                if( progressStatus >= nSumMins * 600) {
                    nCurFreqId = i + 1;
                    maxFreq = freqArray.get(nCurFreqId).maxFreq;
                    minFreq = freqArray.get(nCurFreqId).minFreq;
                    maxFreq = maxFreq == -1 ? minFreq : maxFreq;
                    minFreq = minFreq == -1 ? maxFreq : minFreq;

                    nFreqInterval = Float.valueOf((maxFreq - minFreq)) / Float.valueOf(freqArray.get(nCurFreqId).minutes) / Float.valueOf(600) ;

                    nCurFreq = minFreq + nFreqInterval * (progressStatus - nSumMins * 600);
                }
            }
            Log.i("onChangeProgress",  nCurFreqId + ", " +  nFreqInterval + ", " + nCurFreq);
//            perfectTune.stopTune();
            perfectTune.setTuneFreq(nCurFreq);
            //start the tune
//            perfectTune.playTune();
        }

        public void onStop() {
            synchronized (mPauseLock) {
                mPaused = true;
                mFinished = true;

                perfectTune.stopTune();
                handler.post(new Runnable() {
                    public void run() {
                        seekBar.setProgress(0);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            }
        }

        /**
         * Call this on resume.
         */
        public void onResume() {
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();

                if(mFinished) {
                    mFinished = false;
                    progressStatus = 0;
                }

                perfectTune.playTune();
                handler.post(new Runnable() {
                    public void run() {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            }
        }


        public void run() {
            while (!mFinished) {
                synchronized (mPauseLock) {
                    while (mPaused) {
                        try {
                            mPauseLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                progressStatus += 1;

                Log.i("progressStatus", progressStatus + "");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int nSumMins = 0;
                for (int i = 0; i < freqArray.size() - 1; i++) {
                    nSumMins += freqArray.get(i).minutes;
                    if(progressStatus == nSumMins * 600) {
                        nCurFreqId = i + 1;
                        maxFreq = freqArray.get(nCurFreqId).maxFreq;
                        minFreq = freqArray.get(nCurFreqId).minFreq;
                        maxFreq = maxFreq == -1 ? minFreq : maxFreq;
                        minFreq = minFreq == -1 ? maxFreq : minFreq;

                        nFreqInterval = Float.valueOf((maxFreq - minFreq)) / Float.valueOf(freqArray.get(nCurFreqId).minutes) / Float.valueOf(600) ;

                        nCurFreq = minFreq;
                    }
                }
                perfectTune.setTuneFreq(nCurFreq);
//                perfectTune.playTune();
                nCurFreq += nFreqInterval;

                // Update the progress bar
                handler.post(new Runnable() {
                    public void run() {
                        seekBar.setProgress(progressStatus / 10);
                        tvCurFreq.setText("Freq: " + String.format("%.2f", nCurFreq) + " Hz");
                        tvCurFreq.setTextColor(0xFFFFFFFF);

                        tvCurFreq.setVisibility(prefs.getBoolean("isAdmin", false) ? View.VISIBLE : View.GONE);

                        if(!mPaused) {
                            btnPlayAndStop.setBackgroundResource(android.R.drawable.ic_media_pause);
                        } else {
                            btnPlayAndStop.setBackgroundResource(android.R.drawable.ic_media_play);
                        }
                    }
                });

                if(progressStatus == totalSeconds) {
                    mFinished = true;
                    mPaused = true;
                    bPlaying = false;
//                    progressStatus = 0;
                    handler.post(new Runnable() {
                        public void run() {
//                                         seekBar.setProgress(0);
                            btnPlayAndStop.setBackgroundResource(android.R.drawable.ic_media_play);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            tvCurFreq.setText("End of Program");
                            tvCurFreq.setTextColor(0xFFFF0000);
                            tvCurFreq.setVisibility(View.VISIBLE);
                        }
                    });
                    perfectTune.stopTune();
                    Thread.currentThread().interrupt();

                    MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.notify);
                    mp.start();

                    nCurFreqId = 0;
                    maxFreq = freqArray.get(nCurFreqId).maxFreq;
                    minFreq = freqArray.get(nCurFreqId).minFreq;
                    maxFreq = maxFreq == -1 ? minFreq : maxFreq;
                    minFreq = minFreq == -1 ? maxFreq : minFreq;

                    nCurFreq = minFreq;

                    perfectTune.setTuneFreq(nCurFreq);

                    progressStatus = 0;
                }
            }
        }
    }

    public ArrayList<Program> getTasksFromSharedPrefs(String strPrefName) {
        Gson gson = new Gson();
        String json = prefs.getString(strPrefName, "");
        ArrayList<Program> tasks = gson.fromJson(json, new TypeToken<ArrayList<Program>>(){}.getType());
        return tasks;
    }

    private void loadPrograms() {
        Rejuvenate = getTasksFromSharedPrefs("Rejuvernate");
        Destress = getTasksFromSharedPrefs("Destress");
        Detox = getTasksFromSharedPrefs("Detox");
        Pain =  getTasksFromSharedPrefs("Pain");
        Joy =  getTasksFromSharedPrefs("Joy");
        MicroOrganisms =  getTasksFromSharedPrefs("MicroOrganisms");
        OrganSupport =  getTasksFromSharedPrefs("OrganSupport");
        Custom =  getTasksFromSharedPrefs("Custom");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (p != null) {
            Log.i("onPause", "pause");
//            p.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (p != null) {
            Log.i("onStop", "stop");
//            p.onPause();
        }
    }

    public class SettingsContentObserver extends ContentObserver {
        private AudioManager audioManager;

        public SettingsContentObserver(Context context, Handler handler) {
            super(handler);
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            Log.d(TAG, "Volume now " + currentVolume);

            handler.post(new Runnable() {
                public void run() {
                    volumnSeekBar.setProgress(currentVolume * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) );
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);
        perfectTune = new PerfectTune();

        loadPrograms();

        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, new SettingsContentObserver(this, handler));

        btnSettings = (Button) findViewById(R.id.btnSettings);
//        btnSettings.setVisibility(prefs.getBoolean("isAdmin", false) ? View.VISIBLE : View.GONE);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        tvVolumn = (TextView) findViewById(R.id.tvVolumn);
        tvCurFreq = (TextView) findViewById(R.id.tvCurFreq);
        tvCurFreq.setVisibility(prefs.getBoolean("isAdmin", false) ? View.VISIBLE : View.GONE);
        Query userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("Role");
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.i("onDataChange1", snapshot.toString());
                Log.i("onDataChange-value", snapshot.getValue().toString());
                runOnUiThread(new Runnable() {
                    public void run()
                    {
                        boolean isAdmin = snapshot.getValue().toString().equals("admin");
                        if(isAdmin)
                        {
//                            btnSettings.setVisibility(View.VISIBLE);
                            tvCurFreq.setVisibility(View.VISIBLE);
                        } else {
//                            btnSettings.setVisibility(View.GONE);
                            tvCurFreq.setVisibility(View.GONE);
                        }

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("isAdmin", isAdmin);
                        editor.commit();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i("onDataChange2", error.getMessage());
            }
        });

        Query freqRef = FirebaseDatabase.getInstance().getReference().child("Frequencies");
        freqRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SharedPreferences.Editor editor = prefs.edit();
                Gson gson = new Gson();
                ArrayList<String> removedPrograms = new ArrayList<>(Arrays.asList(programs));
                removedPrograms.remove("Custom");
                for (String item: removedPrograms
                ) {
                    editor.remove(item);
                }

                for (DataSnapshot freqSnapshot: dataSnapshot.getChildren()) {
                    Log.i("freqSnapshot", freqSnapshot.getKey() + ":" + freqSnapshot.getValue().toString());
                    String json = gson.toJson(freqSnapshot.getValue());
                    Log.i("jsonData", json);
                    editor.putString(freqSnapshot.getKey(), json);
                }

                editor.commit();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("databaseError", "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        });


        seekBar = (IndicatorSeekBar ) findViewById(R.id.seekBar);
        seekBar.setIndicatorTextFormat("${MAX-PROGRESS}s");

        volumnSeekBar = (IndicatorSeekBar ) findViewById(R.id.seekBarVolumn);
        volumnSeekBar.setIndicatorTextFormat("${PROGRESS}");
        volumnSeekBar.setMin(0);
        volumnSeekBar.setMax(100);
        handler.post(new Runnable() {
            public void run() {
                volumnSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) );
            }
        });

        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                        }
                        loadPrograms();
                        tvCurFreq.setText("0 Hz");
                        tvCurFreq.setTextColor(0xFFFFFFFF);

                        tvCurFreq.setVisibility(prefs.getBoolean("isAdmin", false) ? View.VISIBLE : View.GONE);
                    }
                });


        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bPlaying) {
                    p.onStop();
                    nCurrentProgramId = -1;
                    updateButtonColors();
                    seekBar.setMax(0);
                    seekBar.setMin(0);

                    Thread.currentThread().interrupt();
                    bPlaying = false;
                }

                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//                intent.putExtra(EXTRA_MESSAGE, message);
                someActivityResultLauncher.launch(intent);
            }
        });

        btnPlayAndStop = (Button) findViewById(R.id.btnPlayAndStop);
        btnPlayAndStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bPlaying) {
                    btnPlayAndStop.setBackgroundResource(android.R.drawable.ic_media_play);
                    //stops the tune
                    bPlaying = false;

                    p.onPause();
                } else {
                    if(nCurrentProgramId > -1) {
                        if(!p.mFinished) {
                            btnPlayAndStop.setBackgroundResource(android.R.drawable.ic_media_pause);
                            //stops the tune
                            bPlaying = true;
                            p.onResume();
                        } else {
                            playTone(currentProgram, nCurrentProgramId);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Please select a program to play.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        btnRejuvenate = (Button) findViewById(R.id.btnRejuvernate);
        btnDeStress = (Button) findViewById(R.id.btnDestress);
        btnDetox = (Button) findViewById(R.id.btnDetox);
        btnPain = (Button) findViewById(R.id.btnPain);
        btnJoy = (Button) findViewById(R.id.btnJoy);
        btnMicro = (Button) findViewById(R.id.btnMicro);
        btnOrgan = (Button) findViewById(R.id.btnOrgan);
        btnCustom = (Button) findViewById(R.id.btnCustom);

        btnRejuvenate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(Rejuvenate, 0);
            }
        });
        btnDeStress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(Destress, 1);
            }
        });
        btnDetox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(Detox, 2);
            }
        });
        btnPain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(Pain, 3);
            }
        });
        btnJoy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(Joy, 4);
            }
        });
        btnMicro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(MicroOrganisms, 5);
            }
        });
        btnOrgan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(OrganSupport, 6);
            }
        });
        btnCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPrograms();
                playTone(Custom, 7);
            }
        });

        seekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {

            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
//                Log.i(TAG, seekBar.getProgress() + "");
//                Log.i(TAG, seekParams.progressFloat + "");
////                Log.i(TAG, seekParams.fromUser);
////                //when tick count > 0
//                Log.i(TAG, seekParams.thumbPosition + "");
////                Log.i(TAG, seekParams.tickText);
                if(p != null)
                    p.onChangeProgress(seekBar.getProgress());
            }
        });

        volumnSeekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                tvVolumn.setText(seekParams.progress + "%");
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress() * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100 , 0);
            }
        });
    }

    private void updateButtonColors() {
        btnRejuvenate.setBackgroundColor(0xFF50585A);
        btnDeStress.setBackgroundColor(0xFF50585A);
        btnDetox.setBackgroundColor(0xFF50585A);
        btnPain.setBackgroundColor(0xFF50585A);
        btnJoy.setBackgroundColor(0xFF50585A);
        btnMicro.setBackgroundColor(0xFF50585A);
        btnOrgan.setBackgroundColor(0xFF50585A);
        btnCustom.setBackgroundColor(0xFF50585A);

        switch (nCurrentProgramId) {
            case 0:
                btnRejuvenate.setBackgroundColor(0xFF00BCD4);
                break;
            case 1:
                btnDeStress.setBackgroundColor(0xFF00BCD4);
                break;
            case 2:
                btnDetox.setBackgroundColor(0xFF00BCD4);
                break;
            case 3:
                btnPain.setBackgroundColor(0xFF00BCD4);
                break;
            case 4:
                btnJoy.setBackgroundColor(0xFF00BCD4);
                break;
            case 5:
                btnMicro.setBackgroundColor(0xFF00BCD4);
                break;
            case 6:
                btnOrgan.setBackgroundColor(0xFF00BCD4);
                break;
            case 7:
                btnCustom.setBackgroundColor(0xFF00BCD4);
                break;
        }
    }

    private void playTone(ArrayList<Program> prog, int progId) {
        if(bPlaying && nCurrentProgramId == progId)
            return;
        if(bPlaying) {
            p.onStop();
            if(thread != null)
                thread.interrupt();
            thread = null;

            seekBar.setProgress(0);
        }
        if (prog == null || prog.size() == 0) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("")
                    .setMessage("No data configured for this program \"" + programs[progId] + "\".")

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        
        nCurrentProgramId = progId;
        currentProgram = prog;
        updateButtonColors();

        int totalSeconds = 0;
        if(prog != null)
            for (int i=0; i < prog.size(); i++) {
                totalSeconds += prog.get(i).minutes;
            }
        totalSeconds *= 60;
        seekBar.setMax(totalSeconds);
        seekBar.setProgress(0);

        perfectTune.setTuneFreq(prog.get(0).minFreq);
        //start the tune
        perfectTune.playTune();
        btnPlayAndStop.setBackgroundResource(android.R.drawable.ic_media_pause);
        bPlaying = true;

        p = new PrimeRun(totalSeconds * 10, prog);
        thread = new Thread(p);
        thread.start();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}