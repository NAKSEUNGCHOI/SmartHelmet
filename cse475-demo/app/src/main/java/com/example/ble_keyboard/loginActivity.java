package com.example.ble_keyboard;

import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.auth.api.identity.BeginSignInRequest;
//import com.google.android.gms.auth.api.identity.Identity;
//import com.google.android.gms.auth.api.identity.SignInClient;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import java.util.Arrays;


public class loginActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView register, forgotPassword, banner;
    private EditText editTextEmail, editTextPassword;
    private Button signIn, googleSignIn, facebookSignIn;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ProgressBar progressBar;

    private boolean flag = false;

    private static final int RC_SIGN_IN = 1;
    private static final String TAG = "GoogleAuthentication";
    private static final String TAG1 = "FacebookAuthentication";
    GoogleSignInClient mGoogleSignInClient;
    Dialog dialog;

    private CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();
        FacebookSdk.sdkInitialize(getApplicationContext());

        /**
         * @Note:
         * Stay signed in google until signout.
         */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("150053049889-94c8p835e5mc9h8isra1rpar8krndo41.apps.googleusercontent.com") //default_web_client_id
                .requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        dialog = new Dialog(loginActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wait1);
        dialog.setCanceledOnTouchOutside(false);

        register = findViewById(R.id.textViewSignup);
        register.setOnClickListener(this);

        forgotPassword = findViewById(R.id.forgotPassword);
        forgotPassword.setOnClickListener(this);

        banner = findViewById(R.id.banner1);
        banner.setOnClickListener(this);

        signIn = findViewById(R.id.btnlogin);
        signIn.setOnClickListener(this);

        googleSignIn = findViewById(R.id.btnGoogle);
        googleSignIn.setOnClickListener(this);

        facebookSignIn = findViewById(R.id.btnFacebook);
        facebookSignIn.setOnClickListener(this);

        editTextEmail = findViewById(R.id.loginEmail);
        editTextEmail.setOnClickListener(this);

        editTextPassword = findViewById(R.id.loginPassword);
        editTextPassword.setOnClickListener(this);

        progressBar = findViewById(R.id.loginLoading);
        progressBar.setOnClickListener(this);

        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        Log.d(TAG1, "onSuccess" + loginResult);
                        handleFacebookToken(loginResult.getAccessToken());
                        finish();
                    }

                    @Override
                    public void onCancel() {
                        // App code
                        Log.d(TAG1, "onCancel");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                        Log.d(TAG1, "onError");
                    }
                });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){

                }else{

                }
            }
        };
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(@Nullable AccessToken accessToken, @Nullable AccessToken accessToken1) {
                if(accessToken1 == null){
                    mAuth.signOut();
                }
            }
        };

    }
    /**
     * @Note:
     * Log in page
     * A switch was employed to give features to each button and banner.
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.banner1:
                startActivity(new Intent(this, loginActivity.class));
                break;

            case R.id.textViewSignup:
                startActivity(new Intent(this, RegiterActivity.class));
                break;

            case R.id.btnlogin:
                userLogin();
                break;

            case R.id.btnGoogle:
                googleSignIn();
                break;

            case R.id.btnFacebook:
                LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
                break;

            case R.id.forgotPassword:
                startActivity(new Intent(this, ForgotPassword.class));
                break;


        }
    }

    /**
     * @Note:
     * Google Sign in function.
     */
    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * @Note:
     * It saves user account and password in Firebase.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            dialog.show();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                dialog.dismiss();
            }
        }
    }
    private void handleFacebookToken(AccessToken token){
        Log.d(TAG1, "handleFacebookToken" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Log.d(TAG1, "sign in with credential: successful");
                    FirebaseUser user = mAuth.getCurrentUser();
                    navigateToMain();
                }else{
                    Log.d(TAG1, "Sign in with credential: failure", task.getException());
                    Toast.makeText(loginActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            navigateToMain();
                        }else{
                            //Log.w(TAG, "signInWithCredential:failure", task.getException());
                            dialog.dismiss();
                            Toast.makeText(loginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(loginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        dialog.dismiss();
    }

    private void userLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if(email.isEmpty()){
            editTextEmail.setError("Invalid email");
            editTextPassword.requestFocus();
            return;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Invalid email");
            editTextEmail.requestFocus();
            return;
        }else if(password.isEmpty()){
            editTextPassword.setError("Enter your password");
            editTextPassword.requestFocus();
            return;
        }else if(password.length() < 6){
            editTextPassword.setError("Password must be more than 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    progressBar.setVisibility(View.GONE);
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(user.isEmailVerified()){
                        startActivity(new Intent(loginActivity.this, MainActivity.class));
                    }else{
                        user.sendEmailVerification();
                        Toast.makeText(loginActivity.this, "Verify your email account.", Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(loginActivity.this, "Invalid email or wrong password.", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    protected void onStart(){
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }
    protected void onStop(){
        super.onStop();

        if(authStateListener != null){
            mAuth.removeAuthStateListener(authStateListener);
        }
    }
}