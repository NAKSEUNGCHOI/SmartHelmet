/**
 * @author Nakseung Choi
 * @date 12/1/2022
 * @description sub activity.
 * A simple UI is employed to visualize the helmet overview.
 */
package com.example.ble_keyboard;

//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
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
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
//import com.jjoe64.graphview.GridLabelRenderer;
//import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class SubActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView progressText;
    private TextView left, right, front, back;
    private TextView text_message;
//    private TextView front_damage, back_damage, left_damage, right_damage;
    private Intent intent;
    private Bundle bundle;
    int i = 100;
//    ImageView front_orange, back_orange, left_orange, right_orange;
    private int tempo[];


    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);
        getSupportActionBar().hide();
        GraphView graph = (GraphView) findViewById(R.id.graph);

        /**
         * @Note:
         * For orange dots.
         */
//        front_orange = findViewById(R.id.front_orange);
//        back_orange = findViewById(R.id.back_orange);
//        left_orange = findViewById(R.id.left_orange);
//        right_orange = findViewById(R.id.right_orange);

        // progress bar and texts
        progressBar = findViewById(R.id.progress_circular);
        progressText = findViewById(R.id.progress_text);
        left = findViewById(R.id.helmet_left);
        right = findViewById(R.id.helmet_right);
        front = findViewById(R.id.helmet_front);
        back = findViewById(R.id.helmet_back);
        text_message = findViewById(R.id.text_message);
        /**
         * @Note:
         * These variables will be used to display the number of damage taken on each side.
         */
//        front_damage = findViewById(R.id.front_damage);
//        back_damage = findViewById(R.id.back_damage);
//        left_damage = findViewById(R.id.left_damage);
//        right_damage = findViewById(R.id.right_damage);

        /**
         * @Note:
         * To get the data from MainActivity, you must receive it with string variable.
         * It is also required to use the same key.
         */
        String durability = getIntent().getStringExtra("progressText");
        String front_data = getIntent().getStringExtra("front");
        String back_data = getIntent().getStringExtra("back");
        String left_data = getIntent().getStringExtra("left");
        String right_data = getIntent().getStringExtra("right");
        Toast.makeText(SubActivity.this, "Here: " + front_data, Toast.LENGTH_LONG).show();

        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                i -= Integer.parseInt(durability);
                if(i >= 0) {
                    progressText.setText("" + i + "%");
                    progressBar.setProgress(i);
                    handler.postDelayed(this, 200);
                }else{
                    handler.removeCallbacks(this);
                }
                front.setText("Front: " + Integer.parseInt(front_data));
                back.setText("Back: " + Integer.parseInt(back_data));
                left.setText("Left: " + Integer.parseInt(left_data));
                right.setText("Right: " + Integer.parseInt(right_data));
                /**
                 * @Task:
                 * You need to do this.
                 */
//                text_message.setText("Last Impact: " + ds1.child("time").getValue() + "");
            }
        }, 200);

        /**
         * @Note:
         * The original plan was to display orange dots at a side that has been taken damage.
         * But, we decided to do it with a coordinate.
         */
//        Animation animation_front = AnimationUtils.loadAnimation(getApplication(), R.anim.fadein);
//        front_orange.startAnimation(animation_front);
//        Animation animation_back = AnimationUtils.loadAnimation(getApplication(), R.anim.fadein);
//        back_orange.startAnimation(animation_back);
//        Animation animation_left = AnimationUtils.loadAnimation(getApplication(), R.anim.fadein);
//        left_orange.startAnimation(animation_left);
//        Animation animation_right = AnimationUtils.loadAnimation(getApplication(), R.anim.fadein);
//        right_orange.startAnimation(animation_right);

         //graph
//        int[] Data= {10,46,53,58,63,67,69,72,75,78,82,85,
//                90,95,99,105,110,115,121,126,132,137,143,
//                148,153,157,162,165,168,170,173,174,175,176,
//                177,177,177,176,176,175,173,172,171,169,168,
//                167,166,164,161,155,147,136,123,111,101,92,84,
//                78,74,70,67,65,64,62,61,61,60,60,59,59,58,58,
//                58,57,57,57,56,56,56,56,56,56,56,55,55,55,55,
//                55,54,54,};

        /**
         * @Note:
         * Graph is going to be employed to display
         *                    at which side the damage has been applied to.
         */
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
        series.appendData(new DataPoint(0, 0),true,90);

        series.setColor(Color.rgb(100,0,0)); //встановити колір кривої
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(50f);
        series.setThickness(100); // Thickness of the dots
//        graph.setTitle("2D Helmet Graphics");
        graph.setTitleTextSize(50);
        graph.setTitleColor(Color.WHITE);

        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(-100);
        graph.getViewport().setMaxX(100);

        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-100);
        graph.getViewport().setMaxY(100);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            public String formatLabel(double value, boolean isValueX){
                if(isValueX){
                    return "";
                }else{
                    return "";
                }
            }
        });
//        grid line
//        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graph.addSeries(series);

        /**
         * @Note:
         * No need to get the data from firebase.
         * Figured out how to retrieve the data with putExtra from MainActivity
         */
//        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
//        ValueEventListener postListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // Get Post object and use the values to update the UI
//                //Post post = dataSnapshot.getValue(Post.class);
//                // ..
//                for (DataSnapshot ds: dataSnapshot.getChildren()) {
//                    Log.d("New Data", ds.getKey() + "," + ds.getChildrenCount() + "," + ds.child("time").getValue() + ", " +  ds.child("value").getValue());
//                    for (DataSnapshot ds1: ds.getChildren()) {
//                        Log.d("New Data", ds1.getKey() + "," + ds1.getChildrenCount() + "," + ds1.child("time").getValue() + ", " +  ds1.child("value").getValue());
//                        front.setText("Front: " + ds1.child("front").getValue() + "");
//                        back.setText("Back: " + ds1.child("back").getValue() + "");
//                        left.setText("Left: " + ds1.child("left").getValue() + "");
//                        right.setText("Right: " + ds1.child("right").getValue() + "");
//                        text_message.setText("Last Impact: " + ds1.child("time").getValue() + "");
//
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                // Getting Post failed, log a message
//                Log.w("Error", "loadPost:onCancelled", databaseError.toException());
//            }
//        };
//        database.addValueEventListener(postListener);



    }

}