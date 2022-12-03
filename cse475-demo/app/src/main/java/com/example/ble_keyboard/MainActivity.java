/**
 * @author Nakseung Choi
 * @date 12/1/2022
 * @description Main activity
 * There are several core features and functions added to this activity:
 * 1. Scan and connect BLE devices.
 * 2. Bring the registered emergency contact from firebase and set an emergency contact
 *    to alert (call & text) in emergency situations.
 * 3. Give permission to all features such as BLUETOOTH_ADMIN, FINE LOCATION, INTERNET, SMS, and so on.
 *    (Please check out Android Manifest for details.)
 * 3. Transfer the data from the MCU to Firebase.
 * 4. IF the impact level data received from the MCU goes above the threshold,
 *    dial & text 911 (or emergency contact set by the user.)
 * 5. Send the data from the MCU to the sub activity for UI.<uses-permission android:name="android.permission.BLUETOOTH" />
 */
package com.example.ble_keyboard;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.data.DataBufferSafeParcelable;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;

import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity{

    // Request codes
    final private int REQUEST_CODE_PERMISSION_LOCATION = 0;
    final private int REQUEST_CALL = 1;
    final private int REQUEST_SMS = 2;

    // flag for testing.
    private boolean flag = true;

    // BLE dialog builder
    private AlertDialog.Builder dialogBuilder;
    private BleDeviceAdapter bleDeviceAdapter;
    private BleDevice activeBleDevice;

    // variables employed to transfer to SubActivity to display
    private int front_data = 0, back_data = 0, left_data = 0, right_data = 0;
//    private int front_damage = 0, back_damage = 0, left_damage = 0, right_damage = 0;
    private static int durability = 0;

    // sign-out button
    private Button signOutBtn;

    // variables used to get location and set the emergency contact;
    private double latitude = 0, longitude = 0;
    private String eContact = "";
    private boolean cancel_sms_Flag = false;

    FirebaseAuth mAuth;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().hide();

        /**
         * @Note:
         * Get the current location of the user.
         */
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        /**
         * @Note:
         * 1. Sign out button.
         * 2. Return back to Sign-in page.
         */
        signOutBtn = findViewById(R.id.signoutBtn);
        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
        /**
         * @Note:
         * Call the emergency contact setting function below.
         */
        emergencyContactSetting();
        /**
         * @Note:
         * build dialog for scanning and connecting BLE devices.
         */
        bleDeviceAdapter = new BleDeviceAdapter(MainActivity.this, android.R.layout.select_dialog_singlechoice);
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setIcon(R.drawable.huskylogo);
        dialogBuilder.setTitle("Select a BLE device");

        dialogBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogBuilder.setAdapter(bleDeviceAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BleDevice bleDevice = bleDeviceAdapter.getItem(which);
                AlertDialog.Builder builderInner = new AlertDialog.Builder(MainActivity.this);
                builderInner.setMessage(bleDevice.getName() + ": " + bleDevice.getMac());
                builderInner.setTitle("Your selected BLE device is");
                builderInner.setPositiveButton("Connect and Subscribe", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        connect(bleDevice);
                    }
                });
                builderInner.show();
            }
        });

        /**
         * @Note:
         * This fab turns on scanning nearby BLE devices.
         */
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });

        checkPermissions();

        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

    }

    /**
     * @Note:
     * This function is used to sign out from Google acct.
     */
    void signOut(){
        LoginManager.getInstance().logOut();
        mAuth.signOut();
        Intent i = new Intent(MainActivity.this, loginActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    /**
     * @Note:
     * Disabled.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * @Note:
     * start Scanning nearby BLE devices.
     */
    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                bleDeviceAdapter.clear();
                bleDeviceAdapter.notifyDataSetChanged();
                dialogBuilder.show();
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                bleDeviceAdapter.add(bleDevice);
                bleDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {

            }
        });
    }

    /**
     * @Note:
     * When connected, start sending the data to Firebase as well as to SubActivity.
     */
    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {

            @Override
            public void onStartConnect() {
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Toast.makeText(MainActivity.this, "Failed to connect.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                activeBleDevice = bleDevice;
                Intent intent = new Intent(MainActivity.this, SubActivity.class);
                FloatingActionButton fab2 = findViewById(R.id.fab2);
                fab2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(intent);
                        finish();
                    }
                });

                Toast.makeText(MainActivity.this, "Connected.", Toast.LENGTH_SHORT).show();

                NavHostFragment navHostFragment = (NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                MainFragment fragment = (MainFragment)navHostFragment.getChildFragmentManager().getFragments().get(0);
                fragment.updateDeviceTextView(activeBleDevice.getName() + ", " + activeBleDevice.getMac());

                BluetoothGattCharacteristic notifyCharacteristic = null;

                for (BluetoothGattService bgs: gatt.getServices()) {
                    for (BluetoothGattCharacteristic bgc: bgs.getCharacteristics()) {
                        int property = bgc.getProperties();
                        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            notifyCharacteristic = bgc;
                            break;
                        }
                    }
                }

                BleManager.getInstance().notify(
                        bleDevice,
                        notifyCharacteristic.getService().getUuid().toString(),
                        notifyCharacteristic.getUuid().toString(),
                        new BleNotifyCallback() {

                            @Override
                            public void onCharacteristicChanged(byte[] data) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

                                        /**
                                         * stores data from MCU into variables for clarification.
                                         * threshold in MCU goes above this threshold
                                         * send the whole data and do the calculation here in the app?
                                         */
                                        left_data = data[0];
                                        right_data = data[1];
                                        front_data = data[2];
                                        back_data = data[3];

                                        /**
                                         * @Note:
                                         * Dial an emergency contact & send sms with location info to the contact.
                                         */
                                        if(flag && (front_data > 64 || back_data > 100 || left_data > 100 || right_data > 100)){
                                            durability += 10;
                                            openDialog();
                                            flag = false;
                                        }else{
                                            flag = false;
                                        }

                                        String id = Long.toString(System.currentTimeMillis());
                                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                                        database.child("inputs").child(id).child("time").setValue(date);
                                        database.child("inputs").child(id).child("front").setValue(front_data);
                                        database.child("inputs").child(id).child("back").setValue(back_data);
                                        database.child("inputs").child(id).child("left").setValue(left_data);
                                        database.child("inputs").child(id).child("right").setValue(right_data);

                                        // send data to SubActivity.
                                        intent.putExtra("progressText", Integer.toString(durability));
                                        intent.putExtra("front", Integer.toString(front_data));
                                        intent.putExtra("back", Integer.toString(back_data));
                                        intent.putExtra("left", Integer.toString(left_data));
                                        intent.putExtra("right", Integer.toString(right_data));

                                    }


                                });
                            }

                            @Override
                            public void onNotifySuccess() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "notify success", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onNotifyFailure(final BleException exception) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "notify failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        });

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {

            }
        });
    }
    public void openDialog(){
        getLastLocation();
        cancel_sms_Flag = false;
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Emergency Alert ON!")
                .setMessage("start time")
                .setNegativeButton("Deactivate alert", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        cancel_sms_Flag = true;
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
        TextView message = alert.findViewById(android.R.id.message);
        TextView title = alert.findViewById(android.R.id.title);
        new CountDownTimer(5000, 100){

            @Override
            public void onTick(long millisUntilFinished) {
                alert.setMessage(String.format("%02d", millisUntilFinished / 1000));
                message.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                message.setTextSize(50);
                if((millisUntilFinished / 1000) < 11){
                    message.setTextColor(getColor(R.color.colorRed));
                }
            }

            @Override
            public void onFinish() {
                if(!cancel_sms_Flag) {
                    alert.dismiss();
                    sendSMS();
                    //makePhoneCall();
                }
            }
        }.start();

    }

    /**
     * @Note:
     * 1. Retrieve the current user's emergency contact from Firebase.
     * 2. Set up the emergency contact if an accident happens.
     */
    private void emergencyContactSetting(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(uid).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult().exists()){
                        DataSnapshot dataSnapshot = task.getResult();
                        eContact = String.valueOf(dataSnapshot.child("emergencyContact").getValue());
                        Toast.makeText(MainActivity.this, "e-contact successfully registered.", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, "e-contact registration failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
    /**
     * @Note:
     * Dial an emergency call.
     * 1. When helmet has taken damage stronger than a set threshold,
     *              this function triggers to dial an emergency contact set by the user.
     */
    private void makePhoneCall() {
        String number = eContact; //"4259999810";
        if(number.trim().length() > 0){
            if(ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] {Manifest.permission.CALL_PHONE}, REQUEST_CALL);
            }else if(flag){
                flag = false;
                String dial = "tel:" + number;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }
        }else{
            Toast.makeText(MainActivity.this, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @Note:
     * Send a SMS to an emergency call.
     * 1. When helmet has taken damage stronger than a set threshold,
     *              this function triggers to send a sms to an emergency contact set by the user.
     */
    private void sendSMS(){
        String uri = "https://maps.google.com/?daddr=" + latitude + "," + longitude;
        String message = "Emergency Alert! Please help Nakseung Choi. Location: " + uri;
        String number = eContact;
        System.out.println(uri);
        if(number.trim().length() > 0){
            if(ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_CALL);
            }else if(latitude > 0){
                flag = false;
//                SmsManager mySmsManager = SmsManager.getDefault();
//                mySmsManager.sendTextMessage(number, null, message, null, null);
                Toast.makeText(MainActivity.this, "Text Message Sent.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean getLastLocation(){

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSION_LOCATION);
        }else{
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if(location != null){
                                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                List<Address> addresses = null;
                                try{
                                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
//                                    System.out.println(addresses.get(0).getLatitude()
//                                            + "," + addresses.get(0).getLongitude());
                                    latitude = addresses.get(0).getLatitude();
                                    longitude = addresses.get(0).getLongitude();
//                                            + ": " + addresses.get(0).getAddressLine(0) + " "
//                                            + addresses.get(0).getLocality() + ", "
//                                            + addresses.get(0).getCountryName();
                                }catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
            return true;
        }
        return false;
    }

    /**
     * @Note:
     * This function checks and gives all the permissions utilized in this app.
     */
    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_SHORT).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    /**
     * @Note:
     * 1. Using a switch case, when each requestCode was passed as a parameter,
     *      it checks the permission.
     */
    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;

            case REQUEST_CALL:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    makePhoneCall();
                }else{
                    Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_SMS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    sendSMS();
                }else{
                    Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkGPSIsOpen()) {
                    Toast.makeText(getApplicationContext(), "Location permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Location permission Denied. ", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}