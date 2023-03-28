package com.dji.activationDemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Random;


public class PathGenActivity extends AppCompatActivity {
    Button mCountBtn;
    TextView textView1, textView2,textView3,textView4,textView5,textView6,textView7;
    ArrayList<Double> arr_speed= new ArrayList<>();
    ArrayList<Double> arr_time= new ArrayList<>();
    private void initUI() {
        mCountBtn = (Button) findViewById(R.id.btn_count);
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_gen);
        initUI();


        mCountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //SCurveProfiling(0,0,0);

//                writeToFile("sisisisisisisisisisi",getApplicationContext());
//
                saveTextToExternalStorage(1);
            }
        });
    }


    public void SCurveProfiling (double frontback,double leftright,double updown){

        double jmax=12, vmax =2 ,amax=3 ,xf=5*3.14/6;
        double T=(amax/jmax)+(vmax/amax)+(xf/vmax), t1=amax/jmax ,t3=(amax/jmax)+(vmax/amax),t2=t3-t1,t4=T-t3,t5=T-t2,t6=T-t1, t7=T;
        double breakpoints[] = {t1,t2,t3,t4,t5,t6,t7};
        double segments[] = {t1,t2-t1,t3-t2,t4-t3,t5-t4,t6-t5,t7-t6};

//        boolean equality = true;
//        while(equality == true){
//            vmax=+0.5;
//            if(((amax/jmax)+(vmax/amax))<=(((amax/jmax)+(vmax/amax)+(xf/vmax))/2)){
//                equality=true;
//            }
//        }
        int counter=1;
        double time_increment, current_time;

        int now_at=0;
        while(now_at<7){
            double last_segment;
            if(now_at==0){
                last_segment=0;}
            else{
                last_segment = breakpoints[now_at-1];
            }

            current_time = last_segment;
            time_increment = segments[now_at]/400;

            while(breakpoints[now_at]>current_time){
                double v= MotionConstants(current_time,now_at,xf);
                current_time=(time_increment*counter)+last_segment;
                arr_speed.add(v);
                arr_time.add(current_time);
                counter++;
            }
            now_at++;
            counter=1;
        }

    }


    public void saveTextToExternalStorage(int finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
        File myDir = new File(root + "/Drone_Files");
        if(!myDir.exists()){ //todo: check it can work
            myDir.mkdirs();
        }
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".txt";
        File file = new File(myDir, fname);
        if (file.exists()) {
            file.delete();
        }
        String sBody = "asdasdasdasd";

        try {
            FileWriter writer = new FileWriter(file);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Toast.makeText(PathGenActivity.this, "Saved", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            e.printStackTrace();
            Writer writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            String s = writer.toString();
        }

        MediaScannerConnection.scanFile(this, new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }


    public double MotionConstants(double current_time, int which_stage, double xf){

//      double[] array;
        double t = current_time;
        double v=0,jmax=12, vmax =2 ,amax=3;
        double T=(amax/jmax)+(vmax/amax)+(xf/vmax), t1=amax/jmax ,t3=(amax/jmax)+(vmax/amax),t2=t3-t1,t4=T-t3,t5=T-t2,t6=T-t1, t7=T;
        double a1=jmax, a2 =0, a3= -jmax, a4=0, a5=-jmax,a6=0, a7=jmax;

        double b1=0, b2= amax, b3=amax+(jmax*t2),b4=0,b5=jmax*t4,b6=-amax,b7=-amax-(jmax*t6);

        double c1=0,c2=((a1*t1*t1)/2+c1+(b1*t1))-((a2*t1*t1)/2+(b2*t1)),
                c3=((a2*t2*t2)/2+c2+(b2*t2))-((a3*t2*t2)/2+(b3*t2)),
                c4=((a3*t3*t3)/2+c3+(b3*t3))-((a4*t3*t3)/2+(b4*t3)),
                c5=((a4*t4*t4)/2+c4+(b4*t4))-((a5*t4*t4)/2+(b5*t4)),
                c6=((a5*t5*t5)/2+c5+(b5*t5))-((a6*t5*t5)/2+(b6*t5)),
                c7=((a6*t6*t6)/2+c6+(b6*c6))-((a7*t6*t6)/2+(b7*t6));

        //double v1=a1*t1*t1/2+b1*t1+c1,v2=a2*t2*t2/2+b2*t2+c2,v3=a3*t3*t3/2+b3*t3+c3,v4=a4*t4*t4/2+b4*t4+c4,v5=a5*t5*t5/2+b5*t5+c5,v6=a6*t6*t6/2+b6*t6+c6,v7=a7*t7*t7/2+b7*t7+c7;

        switch(which_stage){
            case 1:
                v=a1*t*t/2+b1*t+c1;
                return v;
            case 2:
                v=a2*t*t/2+b2*t+c2;
                break;  //optional
            case 3:
                v=a3*t*t/2+b3*t+c3;
                return v;
            case 4:
                v=a4*t*t/2+b4*t+c4;
                return v;
            case 5:
                v=a5*t*t/2+b5*t+c5;
                return v;
            case 6:
                v=a6*t*t/2+b6*t+c6;
                return v;
            case 7:
                v=a7*t*t/2+b7*t+c7;
                return v;
            default:
                break;

        }
        return v;
    }

}
