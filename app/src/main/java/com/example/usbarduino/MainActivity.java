package com.example.usbarduino;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View.OnClickListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static com.example.usbarduino.LogWriter.sDate;
import static com.example.usbarduino.LogWriter.timestamp;
import static java.lang.Integer.*;


public class MainActivity extends Activity {
    //public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    public final String ACTION_USB_PERMISSION = "com.example.usbarduino.USB_PERMISSION";
    Button breakfastButton, lunchButton, dinerButton, inbetweenButton, startButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    public boolean btnStartStopState = false;
    private boolean serialPortConnected;

    //buttons
    public boolean btnBreakfast = false;
    public boolean btnLunch = false;

    //boolean StorageAvailable = false;
    public String fileName = "usbtest.txt";
    public static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/readwrite/" ;
    FileWriter writer;
    BufferedWriter out;
    File root;
    public File testFile;
    public String logIntervalString;
    public Integer logIntervalInt = 1;

    //GraphView
    private GraphView mGraph;
    public int timeXvalue = 0;
    public int testValue = 50;
    public int timeXvalueMin = 0;
    public Timer autoUpdate;
    public double IRvalue;
    public String IRstrvalue = "0";
    public long timeStamp;
    public long timeStampOld;
    public String foodType = "nofood";
    //public String timeStampLog = "01-01-2018 00:00:00";
    public LineGraphSeries<DataPoint> mSeriesLineIRvalue = new LineGraphSeries<>(new DataPoint[]{new DataPoint(0,0)});
   //public PointsGraphSeries<DataPoint> mSeriesLineIRvalue = new PointsGraphSeries<>(new DataPoint[]{new DataPoint(0,0)});

    private String filename = "SampleFile.txt";
    private String filepath = "MyFileStorage";
    File myExternalFile;
    String myData = "";


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                //tvAppend(textView, data);  //tijdelijk uit wegens timestamp
                tvAppend(data);  //tijdelijk uit wegens timestamp
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        connection = usbManager.openDevice(device);
                        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                        if (serialPort != null) {
                            if (serialPort.open()) { //Set Serial Connection Parameters.
                                //setUiEnabled(true);
                                serialPortConnected = true;
                                serialPort.setBaudRate(9600);
                                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                serialPort.read(mCallback);
                                Toast.makeText(getBaseContext(), "Serial Connection Opened!", Toast.LENGTH_LONG).show();
                                //tvAppend(textView,"Serial Connection Opened!\n");

                            } else {
                                Log.d("SERIAL", "PORT NOT OPEN");
                            }
                        } else {
                            Log.d("SERIAL", "PORT IS NULL");
                        }
                    } else {
                        Log.d("SERIAL", "PERM NOT GRANTED");
                    }
                } else if (intent.getAction().equals(ACTION_USB_DEVICE_ATTACHED)) {
                    onClickStart(startButton);
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    onClickStop(stopButton);

                }
            }

            ;
        };


    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        //breakfastButton = (Button) findViewById(R.id.buttonBreakfast);
        //lunchButton = (Button) findViewById(R.id.buttonLunch);
        dinerButton = (Button) findViewById(R.id.buttonDiner);
        inbetweenButton = (Button) findViewById(R.id.buttonInbetween);
        startButton = (Button) findViewById(R.id.buttonStart);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        //textView.setMovementMethod(new ScrollingMovementMethod());
        //setUiEnabled(false);
        serialPortConnected = false;

        //Broadcast Receiver voor serial
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        Calendar calendar = Calendar.getInstance();
        Date d1 = calendar.getTime();
        Date firstDate = new Date();
        Date lastDate = new Date();

        //Settings for Graphs
        mGraph = (GraphView) findViewById(R.id.graph);
        mSeriesLineIRvalue = new LineGraphSeries <DataPoint>();

        mGraph.setBackgroundColor(Color.WHITE);
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinY(0);
        mGraph.getViewport().setMaxY(1000);
        mGraph.getViewport().setScalableY(true);
        //mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(60);
        mGraph.getViewport().setScalable(true);
        mGraph.getViewport().setScrollable(true);
        mGraph.setTitle("IR_value");
        mSeriesLineIRvalue.setDrawDataPoints(true);
        mSeriesLineIRvalue.setDataPointsRadius(10);
        mSeriesLineIRvalue.setThickness(8);
        mSeriesLineIRvalue.setColor(Color.RED);
        mGraph.addSeries(mSeriesLineIRvalue);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(this, "error_permission_map", Toast.LENGTH_LONG).show();
        }

        }


        /*
    public void setUiEnabled(boolean bool) {
        breakfastButton.setEnabled(!bool);
        lunchButton.setEnabled(!bool);
        dinerButton.setEnabled(!bool);
        inbetweenButton.setEnabled(!bool);
        startButton.setEnabled(!bool);
        stopButton.setEnabled(!bool);
    }
*/

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    public void addListenerOnButtonBreakfast(){
        breakfastButton = (Button) findViewById(R.id.buttonBreakfast);
        breakfastButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnBreakfast == false)                {
                    btnBreakfast = true;
                    Toast.makeText(getBaseContext(), "breakfast", Toast.LENGTH_LONG).show();
                    foodType = "breakfast";
                    breakfastButton.setBackgroundColor(Color.YELLOW);
                }
                else
                {
                    btnBreakfast = false;
                    foodType = "nofood";
                    breakfastButton.setBackgroundColor(Color.BLACK);
                }
            }
        });
    }

    public void addListenerOnButtonLunch(){
        lunchButton = (Button) findViewById(R.id.buttonLunch);
        lunchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnLunch == false)                {
                    btnLunch = true;
                    Toast.makeText(getBaseContext(), "lunch", Toast.LENGTH_LONG).show();
                    foodType = "lunch";
                    lunchButton.setBackgroundColor(Color.GREEN);
                }
                else
                {
                    btnLunch = false;
                    foodType = "nofood";
                    lunchButton.setBackgroundColor(Color.BLACK);
                }
            }
        });
    }

    //@Override
    public void onStart() {
        super.onStart();
        addListenerOnButtonBreakfast();
        addListenerOnButtonLunch();
    }



    //@Override
    public void onResume() {
        super.onResume();
        updatePreferences();
        //Toast.makeText(this, "LogInterval: " + logIntervalString, Toast.LENGTH_SHORT).show();
    }

    //@Override
    public void onPause() {
        super.onPause();
        //unregisterReceiver(broadcastReceiver);
    }

    //@Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serialPort.close();
        unregisterReceiver(broadcastReceiver);
        UsbService.SERVICE_CONNECTED = false;
    }






    private Activity getActivity() {
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //return true;
        return super.onCreateOptionsMenu(menu);
    }


    /** The selected menu item is executed */
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent;
        switch (item.getItemId())
        {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                Toast.makeText(getBaseContext(), "USB broadcast", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, BroadcastReceiver.class));
                //startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, Prefs.class));
                updatePreferences();
                break;
            case R.id.menu_exit:
                //dialogBox(); write file Y/N
                finish();
                break;
            default: break;
        }
        return true;
    }

    //@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void onClickStart(View view) {
            IntentFilter filter = new IntentFilter();
           /*
            filter.addAction(ACTION_USB_PERMISSION);
            filter.addAction(ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            */
            registerReceiver(broadcastReceiver, filter);

            HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
            if (!usbDevices.isEmpty()) {
                boolean keep = true;
                for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                    device = entry.getValue();
                    int deviceVID = device.getVendorId();
                    if (deviceVID == 0x1a86)//Arduino Vendor ID, Nano CH340
                    {
                        PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(device, pi);
                        keep = false;
                    } else {
                        connection = null;
                        device = null;
                    }

                    if (!keep)
                        break;
                }
            }
    }


    public void updatePreferences() {
        SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor myPreferenceEditor = myPreference.edit();
        logIntervalString = myPreference.getString("logInterval", logIntervalString);
        myPreferenceEditor.putString("logInterval", logIntervalString).commit();//test
        logIntervalInt = Integer.parseInt(logIntervalString);

        if(myPreference.getBoolean("tts", false) == false){
            Toast.makeText(this, "GEEN TTS", Toast.LENGTH_SHORT).show();
            //mTts.speak("TTS uitgeschakeld", TextToSpeech.QUEUE_ADD, null);
            //mTts.stop();
            //mTts.shutdown();
        }
        if(myPreference.getBoolean("tts", false) == true){
            Toast.makeText(this, "WEL TTS", Toast.LENGTH_SHORT).show();
            //mTts.speak("TTS ingeschakeld", TextToSpeech.QUEUE_ADD, null);
        }
    }


    public void onClickStop(View view) {
    //public void serialStop() {
        //if (serialPort.open()) serialPort.close();
        //unregisterReceiver(broadcastReceiver);
        finish();
        // serialPort.close();
        //setUiEnabled(false);
        //tvAppend(textView,"\nSerial Connection Closed! \n");
    }



    public void onClickInbetween(View view) {
        foodType = "inbetween";
        Toast.makeText(getBaseContext(), "inbetween", Toast.LENGTH_LONG).show();
        readTestData();
    }

    public void readTestData() {
            autoUpdate = new Timer();
            autoUpdate.schedule(new TimerTask() {
                @Override
                public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    testValue += 20;
                    mSeriesLineIRvalue.appendData(new DataPoint(timeXvalue,testValue) ,false, 60); //false aneder negatieve waarde x-as
                    timeXvalue++;
                    if (timeXvalue >= 62) {
                        timeXvalue = 0; //reset na minuut
                        timeXvalueMin++;
                        mSeriesLineIRvalue.resetData(new DataPoint[] {
                                new DataPoint(timeXvalue, testValue)
                        });
                        timeXvalue++; //anders 2 waarden op nul
                    }
                    if(testValue > 500) testValue = 50;
                    if(timeXvalueMin >= logIntervalInt){
                        //Toast.makeText(getApplicationContext(), "Log min: " + String.valueOf(timeXvalueMin), Toast.LENGTH_LONG).show();
                        timeXvalueMin = 0;
                    }
                    //LogWriter.write_info( foodType + ";" + timeXvalue + ";" + testValue + "\n");
                }
            });
                }
            }, 0, 1000); // updates each 1 secs
    }

   public void onClickBreakfast(View view) {
        foodType = "breakfast";
       Toast.makeText(getBaseContext(), "breakfast", Toast.LENGTH_LONG).show();
    }

    public void onClickLunch(View view) {
        foodType = "lunch";
        Toast.makeText(getBaseContext(), "lunch", Toast.LENGTH_LONG).show();
    }

    public void onClickDiner(View view) {
        foodType = "diner";
        Toast.makeText(getBaseContext(), "diner", Toast.LENGTH_LONG).show();
    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    public static boolean isNumeric(String strNum) {
        try {
            Integer i = Integer.parseInt(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    //public void tvAppend(TextView tv, CharSequence text) {
    public void tvAppend(final String text) {
        //final TextView ftv = tv;
        //final CharSequence ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IRstrvalue = text.toString();
                Toast.makeText(getBaseContext(), IRstrvalue, Toast.LENGTH_LONG).show();
                IRvalue = Double.valueOf(IRstrvalue);
                //if(IRvalue >= 0 && IRvalue < 1000) {
                    mSeriesLineIRvalue.appendData(new DataPoint(timeXvalue++, (int) IRvalue), false, 60);
                    timeXvalue++;
                    if (timeXvalue >= 62) {
                        timeXvalue = 0; //reset na minuut
                        timeXvalueMin++;
                        mSeriesLineIRvalue.resetData(new DataPoint[]{
                                new DataPoint(timeXvalue, IRvalue)
                        });
                        timeXvalue++; //anders 2 waarden op nul
                    }
                   if(timeXvalueMin >= logIntervalInt){
                       Toast.makeText(getApplicationContext(), "Log min: " + String.valueOf(timeXvalueMin), Toast.LENGTH_SHORT).show();
                       timeXvalueMin = 0;
                   }
                    LogWriter.write_info( foodType + ";"  + IRvalue + "\n");
                //}
            }
        });
    }
}


