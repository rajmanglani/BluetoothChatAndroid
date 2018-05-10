package com.example.rkmanglani2018.bluetoothtextmessenger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

/**
 * Appears as a dialog and lists all the devices. It lists paired devices or others that are discoverable.
 * If a device is chosen, its MAC address is sent back to the main activity using an intent.
 * Created by rkmanglani2018 on 1/12/2016.
 */
public class DeviceListActivity extends Activity {

    private static final String Tag = "DeviceListActivity";
    private static final boolean done = true;

    public static String Device_Address = "device_address";

    // member fields

    private BluetoothAdapter bAdapter;
    private ArrayAdapter<String> PairedDeviceAAdapter;
    private ArrayAdapter<String> NewDevicesAAdapter;


    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);
        //Initialize the button

        Button search = (Button) findViewById(R.id.button_scan);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // initialize array adapters
        PairedDeviceAAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        NewDevicesAAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // setup list views.
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(PairedDeviceAAdapter);
        pairedListView.setOnItemClickListener(DeviceClickListener);

        ListView newDevicesListView  = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(NewDevicesAAdapter);
        newDevicesListView.setOnItemClickListener(DeviceClickListener);

        // Register for broadcasts

        IntentFilter f = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(Receiver, f);
        // get the local bluetooth adapter

        bAdapter = BluetoothAdapter.getDefaultAdapter();

        // set of currently paired devices

        Set<BluetoothDevice> paired = bAdapter.getBondedDevices();

        // add paired devices to the array adapter
        if(paired.size() > 0){
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for(BluetoothDevice d : paired){
                PairedDeviceAAdapter.add(d.getName() + "\n" + d.getAddress());
            }

        } else{
            String nodevices = getResources().getText(R.string.none).toString();
            PairedDeviceAAdapter.add(nodevices);
        }


    }

    private void doDiscovery(){
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        if(bAdapter.isDiscovering())
            bAdapter.cancelDiscovery();

        bAdapter.startDiscovery();

    }

    private AdapterView.OnItemClickListener DeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            bAdapter.cancelDiscovery();

            // get the MAC address
            String information = ((TextView) view).getText().toString();
            String mac = information.substring(information.length() - 17);   // last 17 chars are mac
            Intent i = new Intent();
            i.putExtra(Device_Address, mac);
            setResult(Activity.RESULT_OK, i);
            finish();
        }
    };


    // Listens for devices and changes title when discovery is done.

    private final BroadcastReceiver Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String str = intent.getAction();

            // If device found
            if(BluetoothDevice.ACTION_FOUND.equals(str)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If already paired
                if(device.getBondState() != BluetoothDevice.BOND_BONDED)
                    NewDevicesAAdapter.add(device.getName() + "\n" + device.getAddress());

            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(str)){
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select);
                if(NewDevicesAAdapter.getCount() == 0){
                    String noDevices = getResources().getText(R.string.notfound).toString();
                    NewDevicesAAdapter.add(noDevices);
                }
            }
        }
    };
}
