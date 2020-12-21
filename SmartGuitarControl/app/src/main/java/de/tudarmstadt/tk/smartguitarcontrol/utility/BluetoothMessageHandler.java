package de.tudarmstadt.tk.smartguitarcontrol.utility;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;


import org.json.JSONObject;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;

public class BluetoothMessageHandler extends Handler {
    private static final String TAG = "BluetoothMessageHandler";

    public interface BT_Handling{
        void handleDisconnect();
        void handleConnected();
        void handleData(Message msg);
    }

    private BT_Handling bt_handling;

    public BluetoothMessageHandler(BT_Handling bt_handling){
        this.bt_handling = bt_handling;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case Constants.CONNECTED:
                bt_handling.handleConnected();
                break;
            case Constants.MESSAGE_TOAST:
                break;
            case Constants.DISCONNECTED:
                bt_handling.handleDisconnect();
                break;
            case Constants.MESSAGE_DATA:
                Log.d(TAG, "handleMessage: received data"+((JSONObject) msg.obj).toString());
                bt_handling.handleData(msg);
                break;
        }
        super.handleMessage(msg);
    }

}