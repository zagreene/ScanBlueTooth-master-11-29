package com.example.tien.scanbluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.util.TimingLogger;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import android.speech.tts.TextToSpeech; // TTS functionality
import java.util.Locale;                // TTS functionality

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    public ListView listView;
    public ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    TextToSpeech speaker;   //declare speech object

    int rssiMax = 0;    // used for calculating max RSSI every 5 seconds.
    String maxAddress;  // name of the object with max RSSI
    boolean clicked = false;    // toggle variable

    FileOutputStream outputStream; // filestream object
    File outputFile;    //output file object

    private long startnow;
    private long endnow;
    private static final String MYTAG = "MyActivity";
    TimingLogger timings = new TimingLogger(MYTAG, "MainActivity");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        timings.addSplit("work A");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        // declare and establish speaker object
        speaker = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            //intiaialization of voice function, make sure we are using US English localication settings
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    speaker.setLanguage(Locale.US);
                }
            }
        }); // this is the end of the speaker establishment block

        // create file for output
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String formattedDate = df.format(Calendar.getInstance().getTime());
        String fileName = "outputLog_" + formattedDate + ".txt";
        outputFile = new File(getExternalFilesDir(null), fileName);


        Button boton = (Button) findViewById(R.id.Bscan);
        boton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // toggle bluetooth discovery on off
                if(clicked) // if clicked when toggled on, turn off
                {
                    BTAdapter.cancelDiscovery();
                    clicked = false;
                }
                else    // if clicked when toggled off, turn on
                {
                    rssiMax = 0;
                    startnow = System.nanoTime();
                    BTAdapter.startDiscovery();
                    clicked = true;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // the following code is a write-to-file function so that closes are done in brackets. OS preservation concept?
    private void writeToFile(String data, File file)
    {
        try
        {
            outputStream = new FileOutputStream(file);
            outputStream.write(data.getBytes());
            outputStream.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    // the following code is the primary bluetooth sniffer.
    public final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            timings.addSplit("work B");
            Date currentTime = Calendar.getInstance().getTime();
            String action = intent.getAction();

            endnow = System.nanoTime();

            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                mDeviceList = new ArrayList<BluetoothDevice>();
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
               // String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                TextView rssi_msg = (TextView) findViewById(R.id.textView);
                long timeEnd = (endnow - startnow) / 1000000;

                // Construct line to print.
                // rssi_msg.setText( "\n" + deviceHardwareAddress +  " | " + rssi_msg.getText()  + " | " +  deviceName + " |  "  + rssi + " dBm"   +  " | time process: " + timeEnd+ " milliseconds" );
                //rssi_msg.setText("Device Name " + deviceName + " | " + deviceHardwareAddress + " | " + rssi + " dBm\n" +  " | time process: " + timeEx + " milliseconds");
                String toScreen = "\n| "  + rssi_msg.getText() + currentTime + "\n| Device Name : " + deviceName  + "\n| Device Address : " + deviceHardwareAddress + "\n| Signal Strength : " + rssi + " dBm."  + "\n| time since start : " + timeEnd + " milliseconds.\n\n";

                //output to screen
                rssi_msg.setText(toScreen);

                // this writes to the log file
                writeToFile(toScreen, outputFile);

                // the following code tells you what you are closest to every 5 seconds, based on RSSI strength of devices over 5 second periods.
                // check current strongest RSSI
                if(rssiMax == 0)    // set initial rssiMax value at sniffing start
                {
                    rssiMax = rssi;
                    maxAddress = deviceHardwareAddress;
                }
                else if(rssi <= rssiMax)    // else if current rssi is greater than (less than b/c negative) recorded max rssi, set rssiMax to new max
                {
                    rssiMax = rssi;
                    maxAddress = deviceHardwareAddress;
                }

                // check if 5 seconds have passed, if so, speak to user and reset bluetooth sniffer
                if(((endnow - startnow) / 1000000000 % 10) == 0)
                {
                    // speak to user in override last-spoken mode, ignore requested parameters and unique identifiers.
                    speaker.speak(deviceHardwareAddress + " closest at " + rssiMax + " d B m.", TextToSpeech.QUEUE_FLUSH, null, null);

                    // flush maxed rssi
                    rssiMax = 0;

                    // reset bluetooth sniffer to keep sniffing
                    BTAdapter.cancelDiscovery();
                    BTAdapter.startDiscovery();
                }
            }

            timings.addSplit("work C");
            timings.dumpToLog();
        }
    };

    // the following should restore the tts object; not really important, so it can be ignored if necessary.
    @Override
    public void onPause()
    {
        if(speaker !=null){
            speaker.stop();
            speaker.shutdown();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();


        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);

    }
}
