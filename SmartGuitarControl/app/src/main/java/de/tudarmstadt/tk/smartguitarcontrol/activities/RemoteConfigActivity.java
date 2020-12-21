package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.utility.BluetoothMessageHandler;
import yuku.ambilwarna.AmbilWarnaDialog;

public class RemoteConfigActivity extends AppCompatActivity implements BluetoothMessageHandler.BT_Handling{
    private Button m_button_apply;
    private TextView m_hint;
    private ScrollView content;
    private Switch top_installed;
    private Button switch_time;
    private int value_switch_time;
    private Button brightness;
    private int value_brightness;

    private Button[] fingerColors;
    private int[] colors;

    private static final String TAG = "RemoteConfigActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_remote_config);
        bindUI();
        BluetoothMessageHandler bmh = new BluetoothMessageHandler(this);
        ((baseClass)this.getApplicationContext()).bluetoothService.setBMH(bmh);
        switchVisibility(true);
    }

    private void bindUI(){
        m_button_apply = findViewById(R.id.btn_brs_apply);
        m_hint =  findViewById(R.id.txt_brs_hint);
        fingerColors = new Button[6];
        colors =  new int[6];
        fingerColors[0] = findViewById(R.id.btn_brs_f);
        fingerColors[1] = findViewById(R.id.btn_brs_f0);
        fingerColors[2] = findViewById(R.id.btn_brs_f1);
        fingerColors[3] = findViewById(R.id.btn_brs_f2);
        fingerColors[4] = findViewById(R.id.btn_brs_f3);
        fingerColors[5] = findViewById(R.id.btn_brs_f4);

        content = findViewById(R.id.brs_content);
        top_installed = findViewById(R.id.brs_switch_top_part);
        switch_time = findViewById(R.id.btn_brs_switch_time);
        value_switch_time = 20;
        brightness = findViewById(R.id.btn_brs_brightness);
        value_brightness = 35;
    }

    private void getDataFromRemote(){
        baseClass.toast(getApplicationContext(),getString(R.string.BRS_loading));
        JSONObject data_to_send = new JSONObject();
        try{
            data_to_send.put("mode","config");
            data_to_send.put("version","1.0");
        }catch (JSONException e){
            Log.e(TAG, "getDataFromRemote: error while putting data",e);
        }finally {
            ((baseClass)this.getApplicationContext()).bluetoothService.write(
                    data_to_send.toString().getBytes()
            );
            Log.d(TAG, "getDataFromRemote: data send to remote");
        }
    }

    private void switchVisibility(boolean hide){
        if(hide){
            m_button_apply.setVisibility(View.GONE);
            content.setVisibility(View.GONE);
            m_hint.setVisibility(View.VISIBLE);
        }else{
            m_button_apply.setVisibility(View.VISIBLE);
            content.setVisibility(View.VISIBLE);
            m_hint.setVisibility(View.GONE);
        }
    }

    private void selectDataFor(final int i){
        int old_color = colors[i];
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, old_color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                colors[i] = color;
                //fingerColors[i].setBackgroundColor(color);
                ViewCompat.setBackgroundTintList(fingerColors[i], ColorStateList.valueOf(color));
            }
        });
        colorPicker.show();
    }

    private void selectNumberDialog(final int min, int max, final int old, final boolean isTiming, String title){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_number_select,null);
        final TextView tv = dialogView.findViewById(R.id.txt_ns_current);
        final SeekBar sb = dialogView.findViewById(R.id.seek_ns);
        sb.setMax(max);
        sb.setProgress(old);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tv.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tv.setText(String.valueOf(old));
        builder.setView(dialogView)
            .setPositiveButton(R.string.GENERIC_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    int tmpValue = sb.getProgress();
                    if(isTiming){
                        value_switch_time = (tmpValue<min)?min:tmpValue;
                        switch_time.setText(String.valueOf(value_switch_time));
                    }else{
                        value_brightness = (tmpValue<min)?min:tmpValue;
                        brightness.setText(String.valueOf(value_brightness));
                    }
                }
            })
            .setNegativeButton(R.string.GENERIC_abort, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).setTitle(title).show();
    }

    private void sendQuitData(){
        JSONObject answer = new JSONObject();
        try{
           answer.put("version","1.0");
           answer.put("mode","main");
            ((baseClass)this.getApplicationContext()).bluetoothService.write(
                    answer.toString().getBytes());
        }catch (JSONException e){
            Log.e(TAG, "sendQuitData: was an error while putting quit data together",e);
        }
    }

    private void sendNewData(){
        JSONObject answer = new JSONObject();
        try{
            answer.put("version","1.0");
            answer.put("mode","config");
            answer.put("top_installed",top_installed.isChecked());
            answer.put("wait_ms",value_switch_time);
            answer.put("brightness",value_brightness);
            JSONObject new_color = new JSONObject();
            for(int i=0;i<6;i++){
                new_color.put(String.valueOf(i-1),androidToPyColor(colors[i]));
            }
            answer.put("colors",new_color);
            ((baseClass)this.getApplicationContext()).bluetoothService.write(
                    answer.toString().getBytes());
            Log.d(TAG, "sendNewData: NEW DATA SEND:"+answer.toString());
        }catch (JSONException e){
            Log.e(TAG, "sendNewData: was an error while putting new config data together",e);
        }
    }

    @Override
    public void handleDisconnect() {
        baseClass.toast(getApplicationContext(),
                getString(R.string.GENERIC_fail_disconnected),true);
        finish();
    }

    @Override
    public void handleConnected() {
        baseClass.toast(getApplicationContext(),
                getString(R.string.main_status_bt_connected),true);
        getDataFromRemote();
    }

    @Override
    public void handleData(Message msg) {
        JSONObject jsonObject = (JSONObject) msg.obj;
        try{
            Log.d(TAG, "handleData: "+jsonObject.toString());
            if(jsonObject.getString("version").equals("1.0") && jsonObject.has("mode")){
                if(jsonObject.getString("mode").equals("config")){
                    if(jsonObject.has("top_installed")){
                        top_installed.setChecked(jsonObject.getBoolean("top_installed"));
                    }
                    if(jsonObject.has("brightness")){
                        value_brightness = jsonObject.getInt("brightness");
                        brightness.setText(
                                String.valueOf(value_brightness));
                    }
                    if(jsonObject.has("wait_ms")){
                        value_switch_time = jsonObject.getInt("wait_ms");
                        switch_time.setText(
                                String.valueOf(value_switch_time));
                    }

                    if(jsonObject.has("colors")){
                        JSONObject incoming_colors = jsonObject.getJSONObject("colors");
                        for(int i=0;i<fingerColors.length;i++){
                            if(incoming_colors.has(String.valueOf(i-1))){
                                fingerColors[i].setEnabled(true);
                                int tmpColor = incoming_colors.getInt(String.valueOf(i-1));
                                tmpColor = pyToAndroidColor(tmpColor);
                                //fingerColors[i].setBackgroundColor(tmpColor);
                                ViewCompat.setBackgroundTintList(
                                        fingerColors[i],ColorStateList.valueOf(tmpColor));
                                colors[i] = tmpColor;
                            }else{
                                fingerColors[i].setEnabled(false);
                                colors[i] = Color.WHITE;
                            }
                        }
                    }
                    switchVisibility(false);
                }else{
                    Log.w(TAG, "handleData: could not find 'config'mode in send data");
                }
            }else{
                // "Mode" key is not existing or wrong version of data
                baseClass.toast(getApplicationContext(),
                        getString(R.string.GENERIC_data_transmit_error),true);
                finish();
            }
        }catch(JSONException e){
            Log.e(TAG, "handleData: error while accessing send data",e);
            finish();
        }
    }

    public void onClickGoBack(View view) {
        sendQuitData();
        finish();
    }

    public void onClickApplyData(View view) {
        switchVisibility(true);
        sendNewData();
        //getDataFromRemote();
    }

    public static int pyToAndroidColor(int input){
        int r = input >> 16;
        int g = (input & 0x00FF00) >> 8;
        int b = input & 0x0000FF;
        return Color.rgb(r,g,b);
    }

    public static int androidToPyColor(int input){
        int r = Color.red(input);
        int g = Color.green(input);
        int b = Color.blue(input);
        return (r << 16) + (g << 8) + b;
    }

    @Override
    protected void onStart() {
        super.onStart();
        getDataFromRemote();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendQuitData();
    }

    public void onClickSelectColor(View view) {
        switch (view.getId()){
            case R.id.btn_brs_f:
                selectDataFor(0);
                break;
            case R.id.btn_brs_f0:
                selectDataFor(1);
                break;
            case R.id.btn_brs_f1:
                selectDataFor(2);
                break;
            case R.id.btn_brs_f2:
                selectDataFor(3);
                break;
            case R.id.btn_brs_f3:
                selectDataFor(4);
                break;
            case R.id.btn_brs_f4:
                selectDataFor(5);
                break;
        }
    }

    public void onClickReboot(View view) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("version","1.0");
            jsonObject.put("mode","reboot");
            ((baseClass)this.getApplicationContext()).bluetoothService.write(
                    jsonObject.toString().getBytes());
        } catch (JSONException e) {
            Log.e(TAG, "onClickReboot: ",e);
        }
    }

    public void onClickPowerOff(View view) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("version","1.0");
            jsonObject.put("mode","power_off");
            ((baseClass)this.getApplicationContext()).bluetoothService.write(
                    jsonObject.toString().getBytes());
        } catch (JSONException e) {
            Log.e(TAG, "onClickReboot: ",e);
        }
    }

    public void onClickSelectTiming(View view) {
        selectNumberDialog(10,100,value_switch_time, true,"Select Switching Time");
    }

    public void onClickSelectBrightness(View view) {
        selectNumberDialog(35,255,value_brightness, false, "Select Brightness");
    }
}
