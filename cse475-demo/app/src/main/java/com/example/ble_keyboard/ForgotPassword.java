/**
 * @author Nakseung Choi
 * @date 12/1/2022
 * @description If the users wish to change their password,
 * this sends an email with a link to the registered email and allows them to change their password.
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
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity implements View.OnClickListener {

    private TextView banner;
    private EditText emailEditText;
    private Button resetPasswordButton;
    private ProgressBar progressBar;

    FirebaseAuth auth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        getSupportActionBar().hide();

        banner = (TextView) findViewById(R.id.forgotBanner);
        banner.setOnClickListener(this);
        resetPasswordButton = (Button) findViewById(R.id.btnReset);
        resetPasswordButton.setOnClickListener(this);

        emailEditText = (EditText) findViewById(R.id.forgotEmail);
        progressBar = (ProgressBar) findViewById(R.id.forgotProgressBar);

        auth = FirebaseAuth.getInstance();

    }
    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.forgotBanner:
                startActivity(new Intent(ForgotPassword.this, loginActivity.class));
                break;
            case R.id.btnReset:
                resetPassword();
                break;
        }
    }

    /**
     * @Note:
     * It sends an email to the user and allows them to reset their password via link.
     */
    private void resetPassword(){
        String email = emailEditText.getText().toString().trim();

        if(email.isEmpty()){
            emailEditText.setError("Invalid email");
            emailEditText.requestFocus();
            return;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailEditText.setError("Invalid email");
            emailEditText.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(ForgotPassword.this, "Check your email to reset your password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }else{
                    Toast.makeText(ForgotPassword.this, "Error. Try again.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}