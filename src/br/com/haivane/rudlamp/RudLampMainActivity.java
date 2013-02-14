package br.com.haivane.rudlamp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ToggleButton;

public class RudLampMainActivity extends Activity implements OnSeekBarChangeListener {

    private View view = null;
    private SeekBar red = null;
    private SeekBar green = null;
    private SeekBar blue = null;
    private SeekBar strobleSeekBar = null;

    BluetoothAdapter bluetoothAdapter = null;
    BluetoothDevice myDevice = null;
    OutputStream mmOutputStream = null;
    BluetoothSocket mySocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_rud_lamp_main);

	bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	view = (View) findViewById(R.id.rgbView);
	red = (SeekBar) findViewById(R.id.red);
	green = (SeekBar) findViewById(R.id.green);
	blue = (SeekBar) findViewById(R.id.blue);
	strobleSeekBar = (SeekBar) findViewById(R.id.strobleSeekBar);

	red.setOnSeekBarChangeListener(this);
	green.setOnSeekBarChangeListener(this);
	blue.setOnSeekBarChangeListener(this);
	strobleSeekBar.setOnSeekBarChangeListener(this);

	SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

	red.setProgress(defaultSharedPreferences.getInt("red", 0));
	green.setProgress(defaultSharedPreferences.getInt("green", 0));
	blue.setProgress(defaultSharedPreferences.getInt("blue", 0));
	strobleSeekBar.setProgress(defaultSharedPreferences.getInt("stroble", 0));

	view.setBackgroundColor(Color.rgb(red.getProgress(), green.getProgress(), blue.getProgress()));
	view.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View v) {
		sendData();
	    }
	});

	if(!bluetoothAdapter.isEnabled()){
	    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableBluetooth, 0);
	}

	loadPairedDevice();
	conectBT();
    }
    
   
    @Override
    protected void onPause() {
	super.onPause();
	disconnect();
    }


    @Override
    protected void onResume() {
	super.onResume();
	loadPairedDevice();
	conectBT();
    }



    private void conectBT() {
	if (myDevice != null) {
	    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
	    try {
		mySocket = myDevice.createRfcommSocketToServiceRecord(uuid);
		mySocket.connect();
		mmOutputStream = mySocket.getOutputStream();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    private void loadPairedDevice() {
	Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
	if (pairedDevices.size() > 0) {
	    for (BluetoothDevice device : pairedDevices) {
		if (device.getName().equals("BT UART")) {
		    myDevice = device;
		    break;
		}
	    }
	}
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
	editor.putInt("red", red.getProgress());
	editor.putInt("green", green.getProgress());
	editor.putInt("blue", blue.getProgress());
	editor.putInt("stroble", strobleSeekBar.getProgress());
	editor.commit();
	disconnect();
	
    }



    private void disconnect() {
	try {
	    if (mmOutputStream != null) mmOutputStream.close();
	    if (mySocket != null) mySocket.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.activity_rud_lamp_main, menu);
	return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	view.setBackgroundColor(Color.rgb(red.getProgress(), green.getProgress(), blue.getProgress()));
	sendData();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
	// TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
	// TODO Auto-generated method stub

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
	super.onRestoreInstanceState(savedInstanceState);
	red.setProgress(savedInstanceState.getInt("red"));
	green.setProgress(savedInstanceState.getInt("green"));
	blue.setProgress(savedInstanceState.getInt("blue"));
	strobleSeekBar.setProgress(savedInstanceState.getInt("stroble"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
	outState.putInt("red", red.getProgress());
	outState.putInt("green", green.getProgress());
	outState.putInt("blue", blue.getProgress());
	outState.putInt("stroble", strobleSeekBar.getProgress());
    }

    private void sendData() {
	String msg = (255 - red.getProgress()) + "," + (255 - green.getProgress()) + "," + (255 - blue.getProgress());
	if(strobleSeekBar.getProgress() > 10) {
	    msg += ",1," + strobleSeekBar.getProgress();
	} else {
	    msg += ",0,0";
	}
	msg += ">\n";
	Log.d("RudLamp", msg);
	try {
	    if (mmOutputStream != null) {
		mmOutputStream.write(msg.getBytes());
	    } else {
		if (myDevice == null)
		    loadPairedDevice();
		conectBT();
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    mmOutputStream = null;
	    if (myDevice == null)
		loadPairedDevice();
	    conectBT();
	}
    }


}
