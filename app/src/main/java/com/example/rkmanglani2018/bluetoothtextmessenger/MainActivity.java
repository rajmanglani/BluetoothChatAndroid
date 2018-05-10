package com.example.rkmanglani2018.bluetoothtextmessenger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Handler;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity {


private static final String TAG = "BluetoothTextMessenger";
private static final boolean done = true;

    // types sent from Bluetooth handler

public static final int Change_State = 1;
public static final int Read_Message = 2;
public static final int Write_Message = 3;
public static final int Device_Name = 4;
public static final int Toast_Message = 5;


// keys received from Bluetooth handler
public static final String DeviceName = "device_name";
public static final String Toast = "toast";

// Request codes for intent
private static final int RequestDeviceConnect = 1;
private static final int EnableBluetooth = 2;


// Name of the connected device
private  String ConnecteddeviceName  = null;
//Array adapter for the conversation thread
private ArrayAdapter<String> ConversationAdapter;
// Buffer for sent messages
private StringBuffer OutBuffer;
// Bluetooth adapter
private BluetoothAdapter Bluetoothadapter = null;
    // Chat service member

private BluetoothChatService chatService = null;


private TextView Title;
private ListView ConversationView;
private EditText OutText;
private Button SendButton;
    private Button ScanButton;
    private Button DiscoverableButton;




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        // Get local Bluetooth adapter
        Bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (Bluetoothadapter == null) {
            android.widget.Toast.makeText(this, "Bluetooth is not available", android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }

//        DiscoverableButton = (Button)findViewById(R.id.make_discoverable);
//        DiscoverableButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ensure();
//            }
//        });

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tb);

    }


    public void onStart(){
        super.onStart();
        // If the bluetooth is switched off request it to be switched on
        if(!Bluetoothadapter.isEnabled()){
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, EnableBluetooth);
        }
        else{
            if(chatService == null)
                setupChat();
        }
    }

    public synchronized void onResume(){
        super.onResume();
        if(chatService != null){
            if(chatService.getState() == BluetoothChatService.STATE_NONE){
                chatService.start();
            }
        }
    }

    private void setupChat() {
        ConversationAdapter = new ArrayAdapter<String>(this, R.layout.message);
        ConversationView = (ListView) findViewById(R.id.in);
        ConversationView.setAdapter(ConversationAdapter);

        OutText = (EditText) findViewById(R.id.edit_text_out);
        OutText.setOnEditorActionListener(writeListener);

        SendButton = (Button) findViewById(R.id.button_send);
        SendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        chatService = new BluetoothChatService(this, handler);

        // start buffer
        OutBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(done) Log.e(TAG, "- ON PAUSE -");
    }
    @Override
    public void onStop() {
        super.onStop();
        if(done) Log.e(TAG, "-- ON STOP --");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (chatService != null) chatService.stop();
        if(done) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensure(){
        if(Bluetoothadapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 400);
            startActivity(i);
        }
    }



    private void sendMessage(String message) {


        // if we are actually connected

        if(chatService.getState() != BluetoothChatService.STATE_CONNECTED){
            android.widget.Toast.makeText(MainActivity.this, "Not Connected", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        if(message.length() >0 ){
            byte[] b = message.getBytes();
            chatService.write(b);
            //Resetting the buffer
            OutBuffer.setLength(0);
            OutText.setText(OutBuffer);
        }
    }






    // action listener for return key

private TextView.OnEditorActionListener writeListener = new TextView.OnEditorActionListener(){

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP){
            String msg = v.getText().toString();
            sendMessage(msg);
        }
        return true;
    }
};

// Gets info back from the BluetoothChat service class.
private final Handler handler = new Handler() {

    @Override
    public void handleMessage(Message msg){
        switch (msg.what){
            case Change_State:
                switch(msg.arg1){
                    case BluetoothChatService.STATE_CONNECTED:

                        ConversationAdapter.clear();
                        break;
                    case BluetoothChatService.STATE_CONNECTING:

                        break;
                    case BluetoothChatService.STATE_LISTEN:
                    case BluetoothChatService.STATE_NONE:

                        break;
                }
                break;
            case Write_Message:
                byte[] b = (byte[]) msg.obj;
                String outMessage = new String(b);
                ConversationAdapter.add("ME : " + outMessage);
                break;
            case Read_Message:
                byte[] br = (byte[]) msg.obj;
                String inMessage = new String(br, 0, msg.arg1);
                ConversationAdapter.add(ConnecteddeviceName + " : " + inMessage);
                break;
            case Device_Name:
                // save the devic name
                ConnecteddeviceName = msg.getData().getString(DeviceName);
                android.widget.Toast.makeText(getApplicationContext(), "Connected to : " + DeviceName, android.widget.Toast.LENGTH_SHORT).show();
                break;
        }
    }

};

    public void onActivityResult(int requestC, int resultC, Intent data ){
        switch(requestC){

            case RequestDeviceConnect:
                if(resultC == Activity.RESULT_OK){
                    String address = data.getExtras().getString(DeviceListActivity.Device_Address);
                    BluetoothDevice device = Bluetoothadapter.getRemoteDevice(address);
                    chatService.connect(device);
                }
                break;
            case EnableBluetooth:
                if(resultC == Activity.RESULT_OK){
                    setupChat();
                }else{
                    finish();
                }

        }
    }


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
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, RequestDeviceConnect);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensure();
                return true;
        }
        return false;
    }



}