package com.alexey.tonegenerator;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener;
import com.skydoves.powerspinner.PowerSpinnerView;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    PowerSpinnerView spinner;
    ListView listView;
    String[] programs = { "Rejuvenate", "De-Stress", "Detox", "Pain", "Joy", "Micro Organisms", "Organ Support", "Custom"};
    String[] programsForUser = { "Custom"};

    ArrayList<Program> Rejuvernate, Destress, Detox, Pain, Joy, MicroOrganisms, OrganSupport, Custom, currentProgram;
    ListAdapter adapter;

    Button btnSave;
    ImageButton btnAdd, btnBack;
    EditText etMinFreq, etMaxFreq, etNewMins;

    SharedPreferences prefs;

    DatabaseReference ref;

    private void saveData() {
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(currentProgram); //tasks is an ArrayList instance variable

        switch (spinner.getSelectedIndex()) {
            case 0:
                editor.putString("Rejuvernate", json);
                ref.child("Frequencies").child("Rejuvernate").setValue(currentProgram);
                break;
            case 1:
                editor.putString("Destress", json);
                ref.child("Frequencies").child("Destress").setValue(currentProgram);
                break;
            case 2:
                editor.putString("Detox", json);
                ref.child("Frequencies").child("Detox").setValue(currentProgram);
                break;
            case 3:
                editor.putString("Pain", json);
                ref.child("Frequencies").child("Pain").setValue(currentProgram);
                break;
            case 4:
                editor.putString("Joy", json);
                ref.child("Frequencies").child("Joy").setValue(currentProgram);
                break;
            case 5:
                editor.putString("MicroOrganisms", json);
                ref.child("Frequencies").child("MicroOrganisms").setValue(currentProgram);
                break;
            case 6:
                editor.putString("OrganSupport", json);
                ref.child("Frequencies").child("OrganSupport").setValue(currentProgram);
                break;
            case 7:
                editor.putString("Custom", json);
//                ref.child("Frequencies").child("Custom").setValue(currentProgram);
                break;
            default:
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("")
                        .setMessage("Please select the program to save.")

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return;
        }

        editor.commit();
        //Toast.makeText(SettingsActivity.this, "Saved successfully", Toast.LENGTH_LONG);

//        new AlertDialog.Builder(SettingsActivity.this)
//                .setTitle("")
//                .setMessage("Successfully saved to \"" + programs[spinner.getSelectedIndex()] + "\" program!")
//
//                // Specifying a listener allows you to take an action before dismissing the dialog.
//                // The dialog is automatically dismissed when a dialog button is clicked.
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        // Continue with delete operation
//                    }
//                })
//
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show();
        Toast.makeText(this, "Successfully saved to \"" + programs[spinner.getSelectedIndex()] + "\" program!", Toast.LENGTH_LONG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnAdd = (ImageButton) findViewById(R.id.btnAdd);
        btnBack = (ImageButton) findViewById(R.id.btnBack);
        etMinFreq = (EditText) findViewById(R.id.etMinFreq);
        etMaxFreq = (EditText) findViewById(R.id.etMaxFreq);
        etNewMins = (EditText) findViewById(R.id.etNewMins);

        spinner = (PowerSpinnerView) findViewById(R.id.spinner);
        currentProgram = new ArrayList<>();
        if(!prefs.getBoolean("isAdmin", false)) {
            spinner.selectItemByIndex(7);
            spinner.setEnabled(false);

            Custom = getTasksFromSharedPrefs("Custom");
            if(Custom != null)
                currentProgram.addAll(Custom);
        }

        listView = (ListView) findViewById(R.id.listView);

        adapter = new ListAdapter(this,
                R.layout.listview_row, currentProgram);
        listView.setAdapter(adapter);

        ref = FirebaseDatabase.getInstance().getReference();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.this.finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("getSelectedIndex", spinner.getSelectedIndex() + "");
                if(spinner.getSelectedIndex() < 0) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("")
                            .setMessage("Please select the program to add.")

                            // Specifying a listener allows you to take an action before dismissing the dialog.
                            // The dialog is automatically dismissed when a dialog button is clicked.
                            .setPositiveButton(android.R.string.yes, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                if((etMinFreq.getText().toString().length() > 0 || etMaxFreq.getText().toString().length() > 0) && etNewMins.getText().toString().length() > 0) {
                    int minFreq = etMinFreq.getText().toString().length() > 0 ? Integer.valueOf(etMinFreq.getText().toString()) : -1;
                    int maxFreq = etMaxFreq.getText().toString().length() > 0 ? Integer.valueOf(etMaxFreq.getText().toString()) : -1;
                    currentProgram.add(new Program(minFreq, maxFreq, Integer.valueOf(etNewMins.getText().toString())));
                    adapter.notifyDataSetChanged();

                    etMinFreq.setText("");
                    etMaxFreq.setText("");
                    etNewMins.setText("");
                    etMinFreq.requestFocus();

                    saveData();
                }
            }
        });

        spinner.setOnSpinnerItemSelectedListener(new OnSpinnerItemSelectedListener<String>() {
            @Override public void onItemSelected(int oldIndex, @Nullable String oldItem, int newIndex, String newItem) {
                currentProgram.clear();
                switch (newIndex) {
                    case 0:
                        Rejuvernate = getTasksFromSharedPrefs("Rejuvernate");
                        if(Rejuvernate != null)
                            currentProgram.addAll(Rejuvernate);
                        break;
                    case 1:
                        Destress = getTasksFromSharedPrefs("Destress");
                        if(Destress != null)
                            currentProgram.addAll(Destress);
                        break;
                    case 2:
                        Detox =  getTasksFromSharedPrefs("Detox");
                        if(Detox != null)
                            currentProgram.addAll(Detox);
                        break;
                    case 3:
                        Pain = getTasksFromSharedPrefs("Pain");
                        if(Pain != null)
                            currentProgram.addAll(Pain);
                        break;
                    case 4:
                        Joy = getTasksFromSharedPrefs("Joy");
                        if(Joy != null)
                            currentProgram.addAll(Joy);
                        break;
                    case 5:
                        MicroOrganisms = getTasksFromSharedPrefs("MicroOrganisms");
                        if(MicroOrganisms != null)
                            currentProgram.addAll(MicroOrganisms);
                        break;
                    case 6:
                        OrganSupport = getTasksFromSharedPrefs("OrganSupport");
                        if(OrganSupport != null)
                            currentProgram.addAll(OrganSupport);
                        break;
                    case 7:
                        Custom = getTasksFromSharedPrefs("Custom");
                        if(Custom != null)
                            currentProgram.addAll(Custom);
                        break;
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void removeItem(int nPos) {
        currentProgram.remove(nPos);
        adapter.notifyDataSetChanged();

        saveData();
    }

    public void onChangeItem(int nPos, int nMinFreq, int nMaxFreq, int nMins) {
        currentProgram.get(nPos).minFreq = nMinFreq;
        currentProgram.get(nPos).maxFreq = nMaxFreq;
        currentProgram.get(nPos).minutes = nMins;
//        adapter.notifyDataSetChanged();
        saveData();
    }

    public ArrayList<Program> getTasksFromSharedPrefs(String strPrefName) {
        Gson gson = new Gson();
        String json = prefs.getString(strPrefName, "");
        ArrayList<Program> tasks = gson.fromJson(json, new TypeToken<ArrayList<Program>>(){}.getType());
        return tasks;
    }
}