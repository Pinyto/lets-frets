package de.tudarmstadt.tk.smartguitarcontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

public class Devices extends Activity {

    public static String CUSTOM_EXTRA_DEVICE = "device";
    public static final int CUSTOM_REQUEST_PERM_COARSE_LOCATION = 420;

    private BluetoothAdapter mBA = null;
    protected ArrayAdapter<BluetoothDevice> newDeviceAdapter = null;
    protected TextView m_txt_new = null;
    protected Button m_btn_scan = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_devices);

        setResult(Activity.RESULT_CANCELED);
        setProgressBarIndeterminate(true);

        mBA = BluetoothAdapter.getDefaultAdapter();

        ListView pairedDevicesList = findViewById(R.id.lst_paired_devices);
        ListView newDevicesList = findViewById(R.id.lst_new_devices);
        ArrayAdapter<BluetoothDevice> pairedDevicesAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedDevicesList.setOnItemClickListener(listClicker);
        pairedDevicesList.setAdapter(pairedDevicesAdapter);
        Set<BluetoothDevice> pairedDevices = mBA.getBondedDevices();
        TextView m_txt_paired = findViewById(R.id.txt_paired_devices);
        if(pairedDevices.size() > 0){
            m_txt_paired.setText(R.string.devices_paired_devices);
            m_txt_paired.setVisibility(View.VISIBLE);
            for(BluetoothDevice device : pairedDevices){
                pairedDevicesAdapter.add(device);
                pairedDevicesAdapter.notifyDataSetChanged();
            }
        }else{
            m_txt_paired.setText(R.string.devices_paired_devices_empty);
            m_txt_paired.setVisibility(View.VISIBLE);
        }
        m_txt_new = findViewById(R.id.txt_new_devices);
        m_btn_scan = findViewById(R.id.btn_devices_scan);
        if(mBA.isDiscovering()){
            m_btn_scan.setText(R.string.devices_stop_scan);
            setProgressBarVisibility(true);
        }

        newDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        newDevicesList.setOnItemClickListener(listClicker);
        newDevicesList.setAdapter(newDeviceAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(true){
                    //TODO: Filter for smart guitars
                    m_txt_new.setVisibility(View.VISIBLE);
                    newDeviceAdapter.add(device);
                    newDeviceAdapter.notifyDataSetChanged();
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                m_btn_scan.setText(R.string.devices_stop_scan);
                setProgressBarIndeterminateVisibility(true);
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                setProgressBarIndeterminateVisibility(false);
                m_btn_scan.setText(R.string.devices_start_scan);
            }

        }
    };

    private final AdapterView.OnItemClickListener listClicker = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mBA.cancelDiscovery();
            BluetoothDevice device = (BluetoothDevice) adapterView.getItemAtPosition(i);
            Intent intent = new Intent();
            intent.putExtra(CUSTOM_EXTRA_DEVICE,device);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CUSTOM_REQUEST_PERM_COARSE_LOCATION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startDeviceScan(findViewById(R.id.btn_devices_scan));
            }
        }
    }

    public void startDeviceScan(View view) {
        // TODO: CHECK RIGHTS AND START DISCOVERY
        if(mBA.isDiscovering()){
            mBA.cancelDiscovery();
            return;
        }
        boolean permissionGranted = checkPermission();
        if(permissionGranted){
            newDeviceAdapter.clear();
            mBA.startDiscovery();
        }else{
            //TODO: IMPROVE REQUEST BY EXPLAINING NEED
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    CUSTOM_REQUEST_PERM_COARSE_LOCATION);
        }
    }

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(Devices.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mReceiver);

    }
}
