package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.utility.BluetoothMessageHandler;
import de.tudarmstadt.tk.smartguitarcontrol.utility.LoadGrip;
import de.tudarmstadt.tk.smartguitarcontrol.views.TabBoard;


public class TrainSingleActivity extends AppCompatActivity implements LoadGrip.TaskResult,
        BluetoothMessageHandler.BT_Handling {
    // This activity will be used for two things:
    //      -show the grip on guitar ( and of course the phone )
    //      -show the grip and wait for feedback

    private TextView m_headline;
    private TextView m_stateText;
    private TabBoard tabBoard;
    private ProgressBar m_progress;
    private ProgressBar m_signal_waiting;
    private ProgressBar m_build_progress;
    private Button toLeft;
    private Button toRight;
    private Button successIndicator;
    private BluetoothMessageHandler bmh;

    private long[] grip_id_array;
    //private boolean tsg_on;
    private int tsg_mode;
    private boolean sendFeedback;
    private boolean multipleGrips;
    private int gripArrayPointer;
    private Grip m_grip;
    private Position[] m_positions;
    private static final String TAG = "TrainSingleActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_train_single);
        bindUI();

        Intent given_intent = getIntent();
        if(given_intent==null){
            baseClass.toast(getApplicationContext(),getString(R.string.TSG_error),true);
            finish();
            return;
        }else if(given_intent.hasExtra(Constants.CUSTOM_EXTRA_GRIP)){
            grip_id_array = new long[1];
            grip_id_array[0] = given_intent.getLongExtra(Constants.CUSTOM_EXTRA_GRIP,0);
        }else if(given_intent.hasExtra(Constants.CUSTOM_MULTIPLE_GRIPS)){
            grip_id_array = given_intent.getLongArrayExtra(Constants.CUSTOM_MULTIPLE_GRIPS);
        }else{
            baseClass.toast(getApplicationContext(),getString(R.string.TSG_error),true);
            finish();
            return;
        }



        //tsg_on =  given_intent.getBooleanExtra(
        //        Constants.CUSTOM_EXTRA_TSG_RESPONSE,false);
        tsg_mode = given_intent.getIntExtra(Constants.CUSTOM_EXTRA_TSG_MODE, 2);
        switch (tsg_mode){
            case 0:
            case 1:
                sendFeedback = true;
                break;
            default:
                sendFeedback = false;
        }
        gripArrayPointer = 0;
        configureProgressUI();

        bmh = new BluetoothMessageHandler(this);
        ((baseClass)this.getApplicationContext()).bluetoothService.setBMH(bmh);

        LoadGrip lg = new LoadGrip(getApplicationContext(),
                grip_id_array[gripArrayPointer], this);
        lg.execute();
    }

    public void bindUI(){
        m_headline = findViewById(R.id.tsg_headline);
        m_stateText = findViewById(R.id.txt_tsg_progress_state);
        m_progress = findViewById(R.id.tsg_progress);
        m_build_progress = findViewById(R.id.tsg_progress_build);
        m_signal_waiting = findViewById(R.id.tsg_wait_signal);
        tabBoard = findViewById(R.id.tsg_tabBoard);
        toLeft = findViewById(R.id.btn_tsg_left);
        toRight = findViewById(R.id.btn_tsg_right);
        successIndicator = findViewById(R.id.btn_tsg_success);
    }

    private void configureProgressUI(){
        m_build_progress.setMax(1);
        m_build_progress.setProgress(0);
        m_signal_waiting.setVisibility(View.INVISIBLE);
        toLeft.setVisibility(View.INVISIBLE);
        toRight.setVisibility(View.INVISIBLE);
        successIndicator.setVisibility(View.INVISIBLE);
        int tmpLen =  grip_id_array.length;
        m_progress.setIndeterminate(false);

        if(tmpLen>1){
            m_progress.setMax(grip_id_array.length);
            updateStateIndicators();
            multipleGrips = true;
        }else{
            m_progress.setVisibility(View.INVISIBLE);
            m_stateText.setVisibility(View.INVISIBLE);
            multipleGrips = false;
        }
    }

    private void sendToGuitar(){
        JSONObject packet = new JSONObject();
        try {
            packet.put("version", "1.0");
            packet.put("mode","TSG");
            packet.put("tsg_feedback",sendFeedback);
            packet.put("strings",m_grip.getHitString());
            JSONArray json_positions = new JSONArray();
            for(Position pos : m_positions){
                JSONObject tmp = new JSONObject();
                tmp.put("fret",pos.getPos());
                tmp.put("string",pos.getString_number());
                tmp.put("finger",pos.getFinger());
                json_positions.put(tmp);
            }
            packet.put("positions",json_positions);
        } catch (JSONException e) {
            Log.e(TAG, "sendToGuitar: JSON_ERROR",e);
        }
        Log.d(TAG, "sendToGuitar: JSON_PACKET:\t"+packet.toString());

        ((baseClass)this.getApplicationContext()).bluetoothService.write(packet.toString().getBytes());

        Log.d(TAG, "sendToGuitar: Send complete");
    }

    @Override
    public void TaskDone(Grip aGrip, Position[] positions) {
        m_grip = aGrip;
        m_positions = positions;
        m_headline.setText(aGrip.getName());
        for(Position cur_pos : positions){
            int[] pos_data = {cur_pos.getPos(),
                    cur_pos.getString_number(),
                    cur_pos.getFinger()};
            tabBoard.handleSingleData(pos_data);
        }
        tabBoard.setStringsToHit(aGrip.getHitString());
        tabBoard.invalidate();
        sendToGuitar();
    }

    public void onClickShowHint(View view) {
        Toast.makeText(this,R.string.TSG_hint,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void handleDisconnect() {
        baseClass.toast(getApplicationContext(),
                getString(R.string.GENERIC_fail_disconnected),true);
        finish();
    }

    @Override
    public void handleConnected(){
        Toast.makeText(getApplicationContext(),
                R.string.main_status_bt_connected,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void handleData(Message msg) {
        JSONObject jsonObject = (JSONObject) msg.obj;
        try{
            if(jsonObject.getString("version").equals("1.0")){
               if(jsonObject.has("prepare_done") &&
                       jsonObject.getBoolean("prepare_done")){
                    m_build_progress.setProgress(1);
                   if(jsonObject.has("tsg_feedback") &&
                           jsonObject.getBoolean("tsg_feedback")){
                       m_signal_waiting.setVisibility(View.VISIBLE);
                   }
                   gripChangeReady();
                   return;
               }else if(jsonObject.has("success") &&
                       jsonObject.getBoolean("success")){
                   if(multipleGrips){
                       if(gripIncAllowed()){
                           if(tsg_mode==1){
                               //sub-auto mode
                               toLeft.setEnabled(true);
                               toRight.setEnabled(true);
                           }else{
                               //mode should be 0 here
                               gripArrayPointer = gripArrayPointer + 1;
                               gripSwitch();
                           }

                       }else{
                           baseClass.toast(getApplicationContext(),
                                   getString(R.string.GENERIC_success),true);
                       }
                   }else{
                       baseClass.toast(getApplicationContext(),
                               getString(R.string.GENERIC_success),true);
                   }
                   successIndicator.setVisibility(View.VISIBLE);
                   m_signal_waiting.setVisibility(View.INVISIBLE);
                   //TODO: Maybe time user for feedback when hit-detection is possible
                   return;
               }
            }
            baseClass.toast(getApplicationContext(),
                    getString(R.string.GENERIC_fail),true);
            finish();
        }catch (JSONException e){
            Log.e(TAG, "handleData: couldn't find correct information in jsonObject",e);
            Log.e(TAG, "handleData: JSON OBJECT"+jsonObject.toString());
            baseClass.toast(getApplicationContext(),getString(R.string.GENERIC_fail),true);
            finish();
        }
    }

    private void gripSwitch(){
        // Clear tab board, start grip load from db, hide buttons, hide wait signal, reset progress
        tabBoard.resetInternalData();
        tabBoard.invalidate();
        updateStateIndicators();
        m_build_progress.setProgress(0);
        if(sendFeedback){m_signal_waiting.setVisibility(View.INVISIBLE);}

        LoadGrip lg = new LoadGrip(getApplicationContext(),
                grip_id_array[gripArrayPointer], this);
        lg.execute();

        toLeft.setVisibility(View.INVISIBLE);
        toRight.setVisibility(View.INVISIBLE);
        successIndicator.setVisibility(View.INVISIBLE);
    }

    private String getStateDescription(){
        return (gripArrayPointer+1) + " / " + grip_id_array.length;
    }

    private void updateStateIndicators(){
        m_stateText.setText(getStateDescription());
        m_progress.setProgress(gripArrayPointer+1);
    }

    private void gripChangeReady(){
        if(gripChangeAllowed()){
            if(gripArrayPointer>0) {
                toLeft.setVisibility(View.VISIBLE);
            }
            if(gripIncAllowed()){
                toRight.setVisibility(View.VISIBLE);
            }
            if(tsg_mode==1){
                toLeft.setEnabled(false);
                toRight.setEnabled(false);
            }
        }
    }

    private boolean gripChangeAllowed(){
        //Checks if manuel or sub-auto mode is enabled
        return multipleGrips && (tsg_mode>0);
    }

    private boolean gripIncAllowed(){
        return gripArrayPointer < grip_id_array.length-1;
    }

    public void onClickGoLeft(View view) {
        if(gripChangeAllowed()){
            if(gripArrayPointer>0){
                gripArrayPointer = gripArrayPointer - 1;
                gripSwitch();
            }
        }
    }

    public void onClickGoRight(View view) {
        if(gripChangeAllowed()){
            if(gripIncAllowed()){
                gripArrayPointer = gripArrayPointer + 1;
                gripSwitch();
            }
        }
    }
}
