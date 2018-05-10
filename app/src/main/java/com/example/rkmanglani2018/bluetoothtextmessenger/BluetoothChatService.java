package com.example.rkmanglani2018.bluetoothtextmessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by rkmanglani2018 on 1/12/2016.
 */
public class BluetoothChatService {
    private static final String Tag = "BluetoothChatService";
    private static final boolean done = true;

    // Name for socket while creating record

    private static final String Name = "BluetoothChat";
    private static final UUID myUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int state;

    // Indicating current connection states

    public static final int STATE_NONE = 0;  // doing nothing
    public static final int STATE_LISTEN = 1; // listening for connections
    public static final int STATE_CONNECTING = 2;  // initiating an outgoing connection
    public static final int STATE_CONNECTED = 3; // now connected

    public BluetoothChatService(Context c, Handler h){
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        mHandler = h;
    }

    // Set the current state

    private synchronized void setState(int i){
        state = i;

        // Give new state to the handler so that the UI can update it.
        mHandler.obtainMessage(MainActivity.Change_State, i, -1).sendToTarget();
    }

    public synchronized int getState(){
        return state;
    }

    /**
     * Start the chat service. Starts the AcceptThread and listening, triggered by the Activity onResume()
     */
    public synchronized void start(){

        if(done)
            Log.d(Tag, "start");
        // Terminate threads trying to make connection
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // terminate the thread currently running an instance
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // start thread to listen to blutooth connections
        if(mAcceptThread == null){
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);

    }

    /**
     * Start the connectThread to initiate the connection
     */

    public synchronized void connect(BluetoothDevice device){
        if(done)
            Log.d(Tag, " connect to: " + device);
        // terminate the thread trying to make a connection
        if(state == STATE_CONNECTING){
            if(mConnectThread == null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        // terminate any thread currently running an instance

        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // initialize thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){
        if(done)
            Log.d(Tag, "connected");
        // Cancel the thread that completed the connection

        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if(mAcceptThread!=null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // start the thread to manage transmissions

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the device data back to the UI activity using handler

        Message msg = mHandler.obtainMessage(MainActivity.Device_Name);
        Bundle b = new Bundle();
        b.putString(MainActivity.DeviceName, device.getName());
        msg.setData(b);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    /**
     * Stops all threads
     */
    public synchronized void stop(){
        if(done)
            Log.d(Tag, "stop");
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if(mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] outBuffer){
        ConnectedThread ct;
        // Synchronize copy of connected thread
        synchronized (this){
            if(state != STATE_CONNECTED)
                return;
            ct = mConnectedThread;
        }
        ct.write(outBuffer);
    }

    /**
     * Connection attempt failed and notify the UI
     */

    public void connectionFailed(){
        setState(STATE_LISTEN);

        Message msg = mHandler.obtainMessage(MainActivity.Toast_Message);
        Bundle b = new Bundle();
        b.putString(MainActivity.Toast, "Unable to connect to the device");
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI
     */

    public void connectionLost(){
        setState(STATE_LISTEN);

        Message msg = mHandler.obtainMessage(MainActivity.Toast_Message);
        Bundle b = new Bundle();
        b.putString(MainActivity.Toast, " Connection was lost ");
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread listens for connections until its connected or canceled.
     */
    private class AcceptThread extends Thread{

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket temp = null;

            try{
                temp = mAdapter.listenUsingRfcommWithServiceRecord(Name, myUUID);
            } catch(IOException e){
                Log.e(Tag, "listen() failed", e);
            }
            mmServerSocket = temp;
        }

        public void run(){
            if(done)
                Log.d(Tag, " Begin mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            while(state != STATE_CONNECTED){
                try{
                    // will return successful connection or exception
                    socket = mmServerSocket.accept();
                } catch (IOException e){
                    Log.e(Tag, "accept() failed", e);
                    break;
                }

                // If the connection gets accepted

                if(socket != null){
                    synchronized(BluetoothChatService.this){
                        switch(state){
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // normal situation, start connecting
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_CONNECTED:
                                // either not ready or already connected.
                                try{
                                    socket.close();
                                } catch(IOException e){
                                    Log.e(Tag, "Could not close the socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if(done)
                Log.i(Tag, "end mAccepted thread");
        }

        public void cancel(){
            if(done)
                Log.d(Tag, "cancel" + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(Tag, "close() of server side failed", e);
            }
        }

    }

    /**
     * This class runs while making a connection with the other device : it either fails or connects
     */

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            mmDevice = device;
            BluetoothSocket temp = null;
            try {
                temp = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                Log.e(Tag, "connect() failed", e);
            }
            mmSocket = temp;
        }

        public void run(){
            Log.i(Tag, "Begin mConnectThread()");
            setName("ConnectThread");

            // Cancel discovery since it slows down connection

            mAdapter.cancelDiscovery();

            //make connection to the bluetooth socket

            try {
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // close the socket
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(Tag, "unable to close the socket", e1);
                }
                //start service over to listen
                BluetoothChatService.this.start();
                return;
            }
            // reset the connection thread

            synchronized (BluetoothChatService.this){
                mConnectThread = null;
            }
            //start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(Tag, "close() failed", e);
            }
        }
    }

    /**
     * This thread runs after connecting with a device. handles all the transmissions.
     */
    private class ConnectedThread extends Thread{

        private final BluetoothSocket mmSocket;
        private final InputStream  mmIn;
        private final OutputStream mmOut;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(Tag, "create connected thread");
            mmSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;
            // Get the bluetooth input and output stream.
            try{
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e){
                Log.e(Tag, "temp sockets not created", e);
            }

            mmIn = tempIn;
            mmOut = tempOut;

        }

        public void run(){
            Log.i(Tag, "Begin mConnectedThread()");
            byte[] buffer = new byte[1024];
            int bytes;
            // keep listening to input stream while connected
            while(true){
                try {
                    bytes = mmIn.read(buffer);
                    // send bytes to the UI activity

                    mHandler.obtainMessage(MainActivity.Read_Message, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(Tag, "disconnected", e);
                    connectionLost();
                    break;
                }
            }

        }
        public void write(byte[] buffer){
            try {
                mmOut.write(buffer);
                mHandler.obtainMessage(MainActivity.Write_Message, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(Tag, "Exception during writing to the socket", e);
            }

        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(Tag, "close() failed", e);
            }
        }
    }


}
