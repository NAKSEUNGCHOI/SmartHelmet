/**
 * @author Nakseung Choi
 * @author Jonathan Do
 * @date 12/1/2022
 * @description sub activity.
 * A simple UI is employed to visualize the helmet overview.
 */
package com.example.ble_keyboard;

//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
//import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
//import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//import android.widget.Button;
//import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
//import com.jjoe64.graphview.GridLabelRenderer;
//import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.HashMap;
import java.util.Map;

public class SubActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView progressText;
    private TextView left, right, front, back;
    private TextView accelerometer;
    private TextView text_message;
    private int x = 5, y = -5;
    private int i = 100;
    private int j = 100;

    // graph
    private PointsGraphSeries<DataPoint> series;
    private DataPoint [] dp = new DataPoint[0];
    GraphView graph;

    // int buffers for left right front back
    private int left_val = 0, right_val = 0, front_val = 0, back_val = 0;
    private int durability = 100;
    private int maxValue = 0;
    private String uid = "";
    private boolean escape = false;
    private int count = 0;


    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);
        getSupportActionBar().hide();
        graph = (GraphView) findViewById(R.id.graph);
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            public String formatLabel(double value, boolean isValueX){
                if(isValueX){
                    return "";
                }else{
                    return "";
                }
            }
        });
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);

        /**
         * @Note:
         * Get user ID
         */
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // progress bar and texts
        progressBar = findViewById(R.id.progress_circular);
        progressText = findViewById(R.id.progress_text);
        left = findViewById(R.id.helmet_left);
        right = findViewById(R.id.helmet_right);
        front = findViewById(R.id.helmet_front);
        back = findViewById(R.id.helmet_back);
        text_message = findViewById(R.id.text_message);
        accelerometer = findViewById(R.id.accelerometer);

//        front.setVisibility(View.INVISIBLE);
//        back.setVisibility(View.INVISIBLE);
//        left.setVisibility(View.INVISIBLE);
//        right.setVisibility(View.INVISIBLE);


        /**
         * @Note:
         * 1. get data from firebase
         * 2. display them in UI page.
         */
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child("Users_data").child(uid).child("durability").setValue(100);
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                //Post post = dataSnapshot.getValue(Post.class);
                // ..
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    Log.d("New Data", ds.getKey() + "," + ds.getChildrenCount() + "," + ds.child("time").getValue() + ", " +  ds.child("value").getValue());
                    for (DataSnapshot ds1 : ds.getChildren()) {
                        if(ds1.getKey() == uid) {
                            escape = true;
                            for (DataSnapshot ds2 : ds1.getChildren()) {
//                                System.out.println("ds2 getVal: " + ds2.getKey());
                                for (DataSnapshot ds3 : ds2.getChildren()) {
                                    Log.d("New Data", ds3.getKey() + "," + ds3.getChildrenCount() + "," + ds3.child("time").getValue() + ", " + ds3.child("value").getValue());

                                    front.setText("Front: " + ds3.child("front").getValue() + "");
                                    back.setText("Back: " + ds3.child("back").getValue() + "");
                                    left.setText("Left: " + ds3.child("left").getValue() + "");
                                    right.setText("Right: " + ds3.child("right").getValue() + "");

                                    if(ds3.child("accelerometer").getValue() != null) {
                                        int Gs = ds3.child("accelerometer").getValue(Integer.class);
                                        if (Gs > 80)
                                            accelerometer.setText("G's: " + ds3.child("accelerometer").getValue() + " Concussion Risk: HIGH");
                                        else if (Gs > 50 && Gs < 80)
                                            accelerometer.setText("G's: " + ds3.child("accelerometer").getValue() + " Concussion Risk: SOME");
                                        else if (Gs > 30 && Gs < 50)
                                            accelerometer.setText("G's: " + ds3.child("accelerometer").getValue() + " Concussion Risk: LOW");
                                        else if (Gs < 30)
                                            accelerometer.setText("G's: " + ds3.child("accelerometer").getValue() + "");
                                        if (ds3.child("right").getValue() != null && ds3.child("left").getValue() != null) {
                                            right_val = (int) ds3.child("right").getValue(Integer.class);
                                            left_val = (int) ds3.child("left").getValue(Integer.class);
                                            x = right_val - left_val;
                                        }
                                    }
                                    if (ds3.child("front").getValue() != null && ds3.child("back").getValue() != null) {
                                        front_val = (int) ds3.child("front").getValue(Integer.class);
                                        back_val = (int) ds3.child("back").getValue(Integer.class);
                                        y = front_val - back_val;
                                    }

                                    if(ds3.child("durability").exists()) {
                                        i = (int) ds3.child("durability").getValue(Integer.class);
                                        i -= 10;
                                        progressText.setText("" + i + "%");
                                        progressBar.setProgress(i);
                                    }

                                    maxValue = Math.max(Math.max(right_val, left_val), Math.max(front_val, back_val));
//                                    Map<String, Integer> map = new HashMap<>();
//                                    map.put("front", front_val);
//                                    map.put("back", back_val);
//                                    map.put("left", left_val);
//                                    map.put("right", right_val);
//                                    comparator(map, maxValue);

                                    graph.removeAllSeries();
                                    dp = new DataPoint[]{new DataPoint(x, y)};
                                    drawGraph(dp);
                                    text_message.setText("Last Impact: " + ds3.child("time").getValue() + "");
//                                    if(escape && maxValue > 70 && i > 0) {
//                                        durability_update();
//                                        escape = false;
//                                    }
                                }
                            }

                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("Error", "loadPost:onCancelled", databaseError.toException());
            }
        };
        database.addValueEventListener(postListener);



    }
    private void durability_update(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users_data");
        reference.child(uid).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult().exists()){
                        DataSnapshot dataSnapshot = task.getResult();
                        if(dataSnapshot.child("durability").exists()) {
                            i = (int) dataSnapshot.child("durability").getValue(Integer.class);
                            i -= 10;
                            progressText.setText("" + i + "%");
                            progressBar.setProgress(i);
                            reference.child(uid).child("durability").setValue(i);

                        }else{
                            progressText.setText("" + 100 + "%");
                            progressBar.setProgress(100);
                            reference.child(uid).child("durability").setValue(100);
                        }
                    }
                }
            }
        });
    }
    private void comparator(Map<String, Integer> map, Integer result){
        String key = "";
        for(Map.Entry mapElement: map.entrySet()){
            int value = (int)mapElement.getValue();
            if(value == result){
                key = (String)mapElement.getKey();
                break;
            }
        }
        if(key == "front"){
            front.setVisibility(View.VISIBLE);
            back.setVisibility(View.INVISIBLE);
            left.setVisibility(View.INVISIBLE);
            right.setVisibility(View.INVISIBLE);
        }else if(key == "back"){
            front.setVisibility(View.INVISIBLE);
            back.setVisibility(View.VISIBLE);
            left.setVisibility(View.INVISIBLE);
            right.setVisibility(View.INVISIBLE);
        }else if(key == "left"){
            front.setVisibility(View.INVISIBLE);
            back.setVisibility(View.INVISIBLE);
            left.setVisibility(View.VISIBLE);
            right.setVisibility(View.INVISIBLE);
        }else if(key == "right"){
            front.setVisibility(View.INVISIBLE);
            back.setVisibility(View.INVISIBLE);
            left.setVisibility(View.INVISIBLE);
            right.setVisibility(View.VISIBLE);
        }
    }
    /**
     * @Note:
     * display a point at which side the damage has been applied to.
     */
    private void drawGraph(DataPoint [] dp) {
        series = new PointsGraphSeries<DataPoint>(dp);

        series.setSize(50);
        series.setColor(Color.rgb(100,0,0));
        graph.setTitleTextSize(50);
        graph.setTitleColor(Color.WHITE);

        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(-622);
        graph.getViewport().setMaxX(622);

        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-622);
        graph.getViewport().setMaxY(622);

        // remove label format
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            public String formatLabel(double value, boolean isValueX){
                if(isValueX){
                    return "";
                }else{
                    return "";
                }
            }
        });
        // grid line
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graph.addSeries(series);
    }

}