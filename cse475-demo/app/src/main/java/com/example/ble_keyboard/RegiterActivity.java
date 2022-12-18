/**
 * @author Nakseung Choi
 * @author Jonathan Do
 * @date 12/1/2022
 * @description Register activity
 * This activity allows a new user to sign up their account in log in activity.
 */
package com.example.ble_keyboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class RegiterActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView alreadyHaveAccount;
    private TextView banner, registerUser;
    private EditText editTextFullname, editTextEmail, editTextPassword, editTextConfirmPassword, editTextEmergencyContact;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regiter);
        getSupportActionBar().hide(); // hide toolbar.

        mAuth = FirebaseAuth.getInstance();

        alreadyHaveAccount = (TextView) findViewById(R.id.alreadyHaveAccount);
        alreadyHaveAccount.setOnClickListener(this);

        banner = (TextView) findViewById(R.id.banner2);
        banner.setOnClickListener(this);

        registerUser = (Button) findViewById(R.id.buttonSignup);
        registerUser.setOnClickListener(this);

        editTextFullname = (EditText) findViewById(R.id.loginEmail);
        editTextEmail  = (EditText) findViewById(R.id.loginPassword);
        editTextEmergencyContact = (EditText) findViewById(R.id.inputEmergencyContact);
        editTextPassword = (EditText) findViewById(R.id.inputPassword);
        editTextConfirmPassword  = (EditText) findViewById(R.id.inputConfirmPassword);

        progressBar = (ProgressBar) findViewById(R.id.loginLoading);


    }

    /**
     * @Note:
     * Using a switch, give a feature to each button or banner
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.banner2:
                startActivity(new Intent(this, RegiterActivity.class));
                break;
            case R.id.buttonSignup:
                registerUser();
                break;
            case R.id.alreadyHaveAccount:
                startActivity(new Intent(this, loginActivity.class));
                break;
        }
    }

    /**
     * @Note: This private function registers users and saves the data in Firebase.
     * 1. if and else if statement check the false cases.
     * 2. if all cases are satisfied, the user typed data will be saved in Firebase.
     * 3. Move to MainActivity.
     */
    private void registerUser() {
        String fullName = editTextFullname.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String emergencyContact = editTextEmergencyContact.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if(fullName.isEmpty()){
            editTextFullname.setError("Full name is required");
            editTextFullname.requestFocus();
            return;
        }else if(email.isEmpty()){
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Invalid email");
            editTextEmail.requestFocus();
            return;
        }else if(emergencyContact.isEmpty()){
            editTextEmergencyContact.setError("Emergency Contact is required");
            editTextEmergencyContact.requestFocus();
            return;
        }else if(password.isEmpty()){
            editTextPassword.setError("Invalid Password");
            editTextPassword.requestFocus();
            return;
        }else if(confirmPassword.isEmpty()){
            editTextConfirmPassword.setError("Re-confirm your password");
            editTextConfirmPassword.requestFocus();
            return;
        }else if(password.length() < 6){
            editTextPassword.setError("Password must be more than 6 characters");
            editTextPassword.requestFocus();
            return;
        }else if(!password.equals(confirmPassword)){
            editTextPassword.setError("Wrong passwords");
            editTextPassword.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            User user = new User(fullName, email, emergencyContact);
                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(RegiterActivity.this, "Account Created.", Toast.LENGTH_SHORT).show();
                                                progressBar.setVisibility(View.GONE);
                                                startActivity(new Intent(RegiterActivity.this, MainActivity.class));
                                            }else{
                                                Toast.makeText(RegiterActivity.this, "Try again.", Toast.LENGTH_SHORT).show();
                                                progressBar.setVisibility(View.GONE);
                                            }
                                        }
                                    });
                        }else{
                            Toast.makeText(RegiterActivity.this, "Try again.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });

    }
}