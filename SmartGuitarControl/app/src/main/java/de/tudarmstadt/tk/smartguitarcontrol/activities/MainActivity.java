package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.Devices;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.fragments.EditDatabase;
import de.tudarmstadt.tk.smartguitarcontrol.utility.BluetoothMessageHandler;

public class MainActivity extends AppCompatActivity implements BluetoothMessageHandler.BT_Handling {



    public static final int CUSTOM_REQUEST_ENABLE_BT = 1337;
    public static final int CUSTOM_FIND_DEVICE = 1338;

    protected TextView txt_status;
    protected BluetoothAdapter mBA;
    protected Switch mBTSwitch;
    protected ImageButton m_btn_devices;
    protected Button m_btn_menu_feedback;
    protected Button m_btn_menu_trainHalf;
    private Button m_btn_menu_trainFull;
    private Button m_btn_menu_ts;
    protected Button m_btn_menu_settings;
    private ArrayList<Button> bluetoothButtons;
    private BluetoothMessageHandler bmh;

    protected static FragmentManager fragmentManager;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_main);
        bindVariables();
        setListenerAndReceivers();
        updateUI();
        loadPreferenceDefaults();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_REQUEST_ENABLE.equals(action)) {
                txt_status.setText(R.string.main_status_bt_starting);
            } else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                updateUI();
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                txt_status.setText(R.string.main_status_bt_searching);
            }else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                txt_status.setText(R.string.main_status_bt_disconnected);
                updateMenu(false);
            }else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
                txt_status.setText(R.string.main_status_bt_connected);
                updateMenu(true);
            }else if(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)){
                txt_status.setText(R.string.main_status_bt_disconnecting);
            }
    }};

    private void bindVariables(){
        mBTSwitch = findViewById(R.id.bt_switch);
        txt_status = findViewById(R.id.txt_main_status);
        m_btn_devices = findViewById(R.id.btn_devices);
        m_btn_menu_feedback = findViewById(R.id.btn_menu_tsg_feedback);
        m_btn_menu_trainHalf = findViewById(R.id.btn_menu_tsg_only_led);
        m_btn_menu_trainFull = findViewById(R.id.btn_menu_tsg_full_mode);
        m_btn_menu_ts = findViewById(R.id.btn_menu_ts);
        m_btn_menu_settings = findViewById(R.id.btn_menu_settings);
        fragmentManager = getSupportFragmentManager();
        mBA = BluetoothAdapter.getDefaultAdapter();
        bluetoothButtons = new ArrayList<>();
        bluetoothButtons.add(m_btn_menu_feedback);
        bluetoothButtons.add(m_btn_menu_trainHalf);
        bluetoothButtons.add(m_btn_menu_trainFull);
        bluetoothButtons.add(m_btn_menu_settings);
        bluetoothButtons.add(m_btn_menu_ts);

        bmh = new BluetoothMessageHandler(this);
        ((baseClass)this.getApplicationContext()).bluetoothService.setBMH(bmh);
    }

    private void setListenerAndReceivers(){
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        this.registerReceiver(mReceiver,filter);

        mBTSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBA.isEnabled()){
                    mBA.disable();
                } else {
                    Intent btTurnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(btTurnOnIntent, CUSTOM_REQUEST_ENABLE_BT);
                }
            }
        });
    }

    private void loadPreferenceDefaults(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sp.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES,false)) {
            PreferenceManager.setDefaultValues(this, R.xml.preferences,true);
            PreferenceManager.setDefaultValues(this, R.xml.color_preferences, true);
            SharedPreferences.Editor editor = sp.edit();
            Resources r = getResources();
            editor.putInt("color_f0",r.getColor(R.color.fingerThumb));
            editor.putInt("color_f1",r.getColor(R.color.fingerIndex));
            editor.putInt("color_f2",r.getColor(R.color.fingerMiddle));
            editor.putInt("color_f3",r.getColor(R.color.fingerRing));
            editor.putInt("color_f4",r.getColor(R.color.fingerPinky));
            editor.putInt("color_any",r.getColor(R.color.fingerAny));
            editor.apply();

        }
    }

    protected void updateUI() {
        if(mBA.isEnabled()){
            mBTSwitch.setChecked(true);
            txt_status.setText(R.string.main_status_bt_on);
            m_btn_devices.setVisibility(View.VISIBLE);
        } else {
            mBTSwitch.setChecked(false);
            txt_status.setText(R.string.main_status_bt_off);
            m_btn_devices.setVisibility(View.GONE);
        }
    }

    private void updateMenu(boolean enable){
        for(Button tmpButton : bluetoothButtons){
            tmpButton.setEnabled(enable);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == CUSTOM_REQUEST_ENABLE_BT){
            if(!this.mBA.isEnabled()) {
                Toast.makeText(getApplicationContext(),R.string.toast_hint_need_bt,
                        Toast.LENGTH_LONG).show();
            }
            // We could check, if the request is successful, but we could also just update the ui
            updateUI();
        } else if(requestCode == CUSTOM_FIND_DEVICE){
            if(resultCode == Activity.RESULT_OK){
                if(data.hasExtra(Devices.CUSTOM_EXTRA_DEVICE)){
                    BluetoothDevice device =
                            (BluetoothDevice) data.getExtras().get(Devices.CUSTOM_EXTRA_DEVICE);
                    Toast.makeText(getApplicationContext(),device.getAddress(),
                            Toast.LENGTH_LONG).show();
                    ((baseClass)this.getApplicationContext()).bluetoothService.connect(device);
                } else {
                    Toast.makeText(getApplicationContext(),"no extra",
                            Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(getApplicationContext(),R.string.toast_hint_select_device,
                        Toast.LENGTH_LONG).show();
            }
        } else if(requestCode == Constants.GET_GRIP_ID_TSG_HALF) {
            if(resultCode == Activity.RESULT_OK){
                if(data.hasExtra(Constants.CUSTOM_EXTRA_GRIP)){
                    Intent train_single = new Intent(this, TrainSingleActivity.class);
                    train_single.putExtra(
                            Constants.CUSTOM_EXTRA_GRIP,
                            data.getLongExtra(Constants.CUSTOM_EXTRA_GRIP,0));
                    train_single.putExtra(Constants.CUSTOM_EXTRA_TSG_RESPONSE,false);
                    startActivity(train_single);
                }
            }
        } else if(requestCode == Constants.GET_GRIP_ID_TSG_FULL) {
            if(resultCode == Activity.RESULT_OK) {
                if(data.hasExtra(Constants.CUSTOM_EXTRA_GRIP)){
                    Intent train_single_full = new Intent(this, TrainSingleActivity.class);
                    long tmp_grip_id = data.getLongExtra(Constants.CUSTOM_EXTRA_GRIP, 0);
                    train_single_full.putExtra(Constants.CUSTOM_EXTRA_GRIP,tmp_grip_id);
                    train_single_full.putExtra(Constants.CUSTOM_EXTRA_TSG_RESPONSE,true);
                    startActivity(train_single_full);
                }
            }
        }
    }

    public void openDevices(View view) {
        Intent devicesIntent = new Intent(this,Devices.class);
        startActivityForResult(devicesIntent, CUSTOM_FIND_DEVICE);
    }

    public void onClickFeedbackMenu(View view) {
        Intent switchIntent = new Intent(this, FeedbackActivity.class);
        startActivity(switchIntent);
    }

    public void onClickTrainSingle(View view) {
        Intent grip_list_intent = new Intent(this, ListOfGripsActivity.class);
        startActivityForResult(grip_list_intent,Constants.GET_GRIP_ID_TSG_HALF);
    }

    public void onClickTrainSingleFull(View view) {
        Intent grip_list_intent = new Intent(this,ListOfGripsActivity.class);
        startActivityForResult(grip_list_intent,Constants.GET_GRIP_ID_TSG_FULL);
    }

    public void onClickTS(View view) {
        Intent ts_intent = new Intent(this, PrepareSequenceActivity.class);
        startActivity(ts_intent);
    }

    public void onClickSettingsMenu(View view) {
        Intent intent = new Intent(this, RemoteConfigActivity.class);
        startActivity(intent);
    }

    public void onClickEditDB(View view) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        EditDatabase editDatabase = EditDatabase.newInstance("Database Options");
        editDatabase.show(fragmentTransaction,"edit_database");
    }

    public void onClickOpenSettings(View view) {
        Intent intent = new Intent(this,SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickDebug(View view) {
        updateMenu(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((baseClass)this.getApplicationContext()).bluetoothService.setBMH(bmh);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mReceiver);
        ((baseClass)this.getApplicationContext()).bluetoothService.removeHandler();
    }

    @Override
    public void handleDisconnect() {
        updateMenu(false);
        txt_status.setText(R.string.main_status_bt_disconnected);
    }

    @Override
    public void handleConnected() {
        updateMenu(true);
        txt_status.setText(R.string.main_status_bt_connected);
    }

    @Override
    public void handleData(Message msg) {
        Toast.makeText(getApplicationContext(),
                "Received Data. I don't know what to do now",Toast.LENGTH_LONG).show();
        Log.d(TAG, "handleData in MAIN: " + msg.obj.toString());
    }


}
