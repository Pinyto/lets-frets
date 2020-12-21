package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.smartguitarcontrol.BluetoothService;
import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.FretBoard;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.utility.BluetoothMessageHandler;

public class FeedbackActivity extends AppCompatActivity implements BluetoothMessageHandler.BT_Handling {

    private boolean transferring = false;
    private boolean firstReceived;
    private Button m_btn_switch;
    private final static String TAG = "FeedbackActivity";
    private FretBoard fretBoard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_feedback);
        m_btn_switch = findViewById(R.id.btn_feedback_switch);
        fretBoard = findViewById(R.id.fret_canvas);
        BluetoothMessageHandler bmh = new BluetoothMessageHandler(this);
        ((baseClass)this.getApplicationContext()).bluetoothService.setBMH(bmh);
    }

    private void startTransferring(){
        try {
            JSONObject jsonStart = new JSONObject();
            jsonStart.put("version","1.0");
            jsonStart.put("mode","show");
            transferring = true;
            m_btn_switch.setText(R.string.feedback_stop);
            m_btn_switch.setEnabled(false);
            firstReceived = false;
            ((baseClass)this.getApplicationContext()).bluetoothService.write(
                    jsonStart.toString().getBytes());
        }catch (JSONException e){
            Toast.makeText(this,"Try again",Toast.LENGTH_SHORT).show();
        }
    }

    //Init stop of transfer, disable button, till transaction is closed
    private void stopTransferring(){
        m_btn_switch.setEnabled(false);
        endTransmission();
    }

    public void onClickSwitchTransfer(View view) {
        if(transferring){
            stopTransferring();
        }else{
            startTransferring();
        }
    }

    private void endTransmission(){
        if(transferring){
            JSONObject answer = new JSONObject();
            try{
                answer.put("version","1.0");
                answer.put("mode","show_done");
                ((baseClass)this.getApplicationContext()).bluetoothService.write(
                        answer.toString().getBytes());
            }catch (JSONException e){
                baseClass.toast(getApplicationContext(),"JSON. Went wrong");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        endTransmission();
    }

    @Override
    public void handleDisconnect() {
        baseClass.toast(getApplicationContext(),getString(R.string.GENERIC_fail_disconnected));
        finish();
    }

    @Override
    public void handleConnected() {
        baseClass.toast(getApplicationContext(),getString(R.string.main_status_bt_connected));
        m_btn_switch.setEnabled(true);
        m_btn_switch.setText(R.string.feedback_start);
        transferring = false;
    }

    @Override
    public void handleData(Message msg) {
        JSONObject jsonObject = (JSONObject) msg.obj;
        try{
            if(jsonObject.getString("version").equals("1.0")){
                if(jsonObject.getString("mode").equals("show")){
                    if(!jsonObject.getBoolean("success")){
                        baseClass.toast(getApplicationContext(),
                                getString(R.string.GENERIC_data_transmit_error),true);
                        transferring = false;
                        finish();
                        return;
                    }
                    if(jsonObject.has("refined")){
                        if(!firstReceived){
                            firstReceived = true;
                            m_btn_switch.setEnabled(true);
                        }
                        double[][] data = new double[4][6];
                        JSONObject refined = jsonObject.getJSONObject("refined");
                        for(int i=0;i<data.length;i++){
                            JSONArray tmpJSON_arr = refined.getJSONArray(Integer.toString(i));
                            for(int j=0;j<tmpJSON_arr.length();j++){
                                data[i][j] = tmpJSON_arr.getDouble(j);
                            }
                        }
                        fretBoard.setDataViaRaw(data);
                        fretBoard.invalidate();
                    }
                }else if(jsonObject.getString("mode").equals("show_done")){
                    m_btn_switch.setEnabled(true);
                    m_btn_switch.setText(R.string.feedback_start);
                    transferring = false;
                }
            }
        }catch (JSONException e){
            baseClass.toast(getApplicationContext(),
                    getString(R.string.GENERIC_data_transmit_error),true);
            finish();
        }
    }
}
