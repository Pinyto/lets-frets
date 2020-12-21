package de.tudarmstadt.tk.smartguitarcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import de.tudarmstadt.tk.smartguitarcontrol.utility.BluetoothMessageHandler;

public class BluetoothService {
    private final BluetoothAdapter mBA;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private Handler mHandler = null;
    private BluetoothMessageHandler bmh = null;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // Generated through "online guid generator"
    //private static final UUID CUSTOM_UUID = UUID.fromString("9a134245-c42b-4610-983e-9987617239bf");
    // serial uuid 1e0ca4ea-299d-4335-93eb-27fcfe7fa848
    //private static final UUID CUSTOM_UUID = UUID.fromString("1e0ca4ea-299d-4335-93eb-27fcfe7fa848");
    //serial uuid from git
    private static final UUID CUSTOM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private static final String TAG = "C_BluetoothService";

    public BluetoothService(){
        mBA = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    public synchronized int getState() {
        return mState;
    }

    public boolean handlerSet(){
        return null!=mHandler;
    }

    private boolean bmhSet(){
        return null!=bmh;
    }

    public void setHandler(Handler handler){
        mHandler = handler;
    }

    public void removeHandler() {
        mHandler = null;
    }

    public void setBMH(BluetoothMessageHandler tmp){
        bmh = tmp;
    }

    public BluetoothMessageHandler getBMH(){
        return bmh;
    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write un-synchronized
        r.write(out);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        // Update UI title
        //TODO: Currently the ui listens to changes of the bluetooth module
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected started");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        //TODO:  Currently the ui listens to changes of the bluetooth module
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        mState = STATE_NONE;
        // Update UI title
        // Start the service over to restart listening mode
        Log.w(TAG,"Connection lost");
        Message msg = bmh.obtainMessage(Constants.DISCONNECTED);
        bmh.dispatchMessage(msg);
        //TODO: Currently the ui listens to changes of the bluetooth module
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                // safe version? :  tmp = device.createRfcommSocketToServiceRecord(CUSTOM_UUID);
                    tmp = device.createRfcommSocketToServiceRecord(CUSTOM_UUID);
                    //tmp = device.createInsecureRfcommSocketToServiceRecord(CUSTOM_UUID);
            } catch (IOException e) {
                Log.e(TAG,"could not create socket",e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            // Always cancel discovery because it will slow down a connection
            mBA.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // TODO: Ask user for auto reconnect?
                //connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread" );
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    if(handlerSet()){
                        mHandler.obtainMessage(Constants.MESSAGE_DATA, bytes,-1, buffer).sendToTarget();
                    }
                    if(bmhSet()){
                        String raw_message = new String(buffer,0,bytes);
                        try{
                            JSONObject json_message = new JSONObject(raw_message);
                            bmh.obtainMessage(Constants.MESSAGE_DATA,  -1, -1, json_message).sendToTarget();
                        } catch (JSONException e){
                            Log.e(TAG, "run: received answer could not be json-parsed",e);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                // TODO: Maybe tell ui that data was send
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
