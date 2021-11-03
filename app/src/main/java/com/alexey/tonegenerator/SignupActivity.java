package com.alexey.tonegenerator;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    private FirebaseAuth mAuth;

    Button btnSignup, btnBack;
    EditText etEmail, etPassword, etPasswordConfirm;
    ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        btnSignup = (Button) findViewById(R.id.btnSignup);
        btnBack = (Button) findViewById(R.id.btnBack);
        etEmail = (EditText) findViewById(R.id.username);
        etPassword = (EditText) findViewById(R.id.password);
        etPasswordConfirm = (EditText) findViewById(R.id.passwordConfrim);
        loading = (ProgressBar) findViewById(R.id.loading);

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();

                String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

                if (!email.matches(emailPattern))
                {
                    Toast.makeText(getApplicationContext(),"Invalid email address", Toast.LENGTH_SHORT).show();
                    return;
                }

                String password = etPassword.getText().toString();
                if(password.isEmpty()) {
                    Toast.makeText(getApplicationContext(),"Please input password", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(password.length() < 6) {
                    Toast.makeText(getApplicationContext(),"Password should have more than 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                String passwordConfrim = etPasswordConfirm.getText().toString();
                if(!password.equals(passwordConfrim)) {
                    Toast.makeText(getApplicationContext(),"Password does not match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                loading.setVisibility(View.VISIBLE);
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                loading.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                                        Map<String, String> map = new HashMap<>();
                                        map.put("Email", user.getEmail());
                                        map.put("Password", etPassword.getText().toString());
                                        map.put("Role", "user");
                                        ref.child("Users").child(user.getUid()).setValue(map);

                                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
//                                    updateUI(user);
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.i("createUserfailure", task.getException().getLocalizedMessage());
                                    Toast.makeText(SignupActivity.this, "Authentication failed. " + task.getException().getLocalizedMessage(),
                                            Toast.LENGTH_SHORT).show();
//                                    updateUI(null);
                                }
                            }
                        });

            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        Log.i("onAuthStateChanged", "onAuthStateChanged");
    }
}
