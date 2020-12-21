package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.resources.MaterialResources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.adapter.RecyclerGripAdapter;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.utility.LoadAllGrips;
import de.tudarmstadt.tk.smartguitarcontrol.utility.LoadGrip;

public class ExportAllActivity extends AppCompatActivity implements LoadAllGrips.TaskResult, LoadGrip.TaskResult {
    private boolean export_all;
    private Button export;
    private FloatingActionButton add_grip;
    private RecyclerView recyclerView;
    private RecyclerGripAdapter rga;

    private ArrayList<Pair<Grip,Position[]>> m_pairArrayList;
    private Uri m_uri = null;

    private static final String TAG = "ExportAllActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_export_all);
        Bundle given = getIntent().getExtras();
        if(null != given){
            export_all = given.getBoolean(Constants.CUSTOM_EXPORT_ALL, false);
        }
        bindUI();
        prepareExportMode();

    }

    private void bindUI(){
        export = findViewById(R.id.btn_export);
        add_grip = findViewById(R.id.floating_export_add);
        recyclerView = findViewById(R.id.recycler_export);
    }

    private void prepareExportMode(){
        if(export_all){
            export.setEnabled(true);
            recyclerView.setVisibility(View.INVISIBLE);
            add_grip.hide();
        }else{
            add_grip.show();
            recyclerView.setVisibility(View.VISIBLE);
            export.setEnabled(false);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
            recyclerView.setLayoutManager(layoutManager);

            rga = new RecyclerGripAdapter(new RecyclerGripAdapter.customClickListener() {
                @Override
                public void onItemClick(int position) {

                }
            });
            recyclerView.setAdapter(rga);
        }
    }

    private void enableUI(boolean enable){
        export.setEnabled(enable);
        add_grip.setEnabled(enable);
    }

    private String generateTitle(){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm",Locale.getDefault());
        Date date = new Date();
        return df.format(date)+"_DB_export.json";

    }

    private String prepareData(){
        ArrayList<Pair<Grip,Position[]>> all_items;
        if(!export_all){
            // Get grips from recycler first
            all_items = rga.getAllPairs();
        }else{
            all_items = m_pairArrayList;
        }
        try{
            JSONObject json = new JSONObject();
            json.put("version",Constants.DB_VERSION);

            JSONArray j_arr = new JSONArray();
            for(Pair<Grip,Position[]> pair : all_items){
                JSONObject j_grip = new JSONObject();
                j_grip.put("name",pair.first.getName());
                j_grip.put("strings",pair.first.getHitString());
                JSONArray j_positions = new JSONArray();
                for(Position pos : pair.second){
                    JSONObject j_pos = new JSONObject();
                    j_pos.put("string",pos.getString_number());
                    j_pos.put("pos",pos.getPos());
                    j_pos.put("finger",pos.getFinger());
                    j_positions.put(j_pos);
                }
                if(j_positions.length()>0){
                    j_grip.put("positions",j_positions);
                }
                j_arr.put(j_grip);
            }
            json.put("grips",j_arr);
            return json.toString(4);
        } catch (JSONException e){
            Log.e(TAG, "prepareData: JSON EXCEPTION WHILE PREPARING DATA FOR EXPORT",e);
        }
        return null;
    }

    private void storeFile(){
        if(m_uri==null){
            Log.e(TAG, "OnClickWriteExternal: No Uri!");
            baseClass.toast(getApplicationContext(),R.string.export_error_io);
            enableUI(true);
            return;
        }
        String data = prepareData();
        if(null==data){
            baseClass.toast(getApplicationContext(),
                    R.string.export_error_nothing,true);
            finish();
            return;
        }
        ContentResolver cr = getContentResolver();
        OutputStream out;
        try {
            out = cr.openOutputStream(m_uri);
            if(null==out){throw new IOException();}
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
            bw.write(data);
            bw.close();
            baseClass.toast(getApplicationContext(),
                    R.string.GENERIC_success);
            enableUI(true);
            finish();
            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            Log.e(TAG, "OnClickWriteExternal: Could not write external write",e);
            baseClass.toast(getApplicationContext(),R.string.export_error_io);
        }

        enableUI(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==Constants.PERM_FILE_STORAGE){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startExplorer();
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==Constants.FILE_STORAGE_SELECT){
            if(resultCode==Activity.RESULT_OK && null != data && null != data.getData()){
                enableUI(false);
                m_uri = data.getData();
                if(export_all){
                    LoadAllGrips lag = new LoadAllGrips(getApplicationContext(), this);
                    lag.execute();
                }else{
                    storeFile();
                }
            }else{
                baseClass.toast(getApplicationContext(),
                        R.string.export_hint_directory_fail,true);
            }
        } else if(requestCode==Constants.GET_GRIP_ID){
            if(resultCode==Activity.RESULT_OK){
                if(null!=data && data.hasExtra(Constants.CUSTOM_EXTRA_GRIP)){
                    long tmpID = data.getLongExtra(Constants.CUSTOM_EXTRA_GRIP, 0);
                    LoadGrip lg = new LoadGrip(getApplicationContext(),tmpID,this);
                    lg.execute();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startExplorer(){
        if(checkStoragePermission()){
            String doc_title = generateTitle();
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE,doc_title);
            try{
                String title = getString(R.string.export_choose_directory);
                startActivityForResult(Intent.createChooser(intent, title),
                        Constants.FILE_STORAGE_SELECT);

            } catch (android.content.ActivityNotFoundException ex){
                baseClass.toast(getApplicationContext(),R.string.GENERIC_MISSING_FILE_EXPLORER);
            }
        }
    }

    private boolean checkStoragePermission(){
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        Constants.PERM_FILE_STORAGE);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        permissions,
                        Constants.PERM_FILE_STORAGE);
            }
            Log.w(TAG, "checkStoragePermission: permission not yet allowed");
            return false;
        } else {
            return true;
        }
    }

    public void onClickStartExport(View view) {
        if(checkStoragePermission()){
            startExplorer();
        }
    }

    @Override
    public void TaskDone(ArrayList<Pair<Grip, Position[]>> pairArrayList) {
        m_pairArrayList = pairArrayList;
        storeFile();
    }

    public void onClickAddGripToList(View view) {
        Intent get_grip = new Intent(this, ListOfGripsActivity.class);
        startActivityForResult(get_grip, Constants.GET_GRIP_ID);
    }

    @Override
    public void TaskDone(Grip aGrip, Position[] positions) {
        rga.addPair(aGrip,positions);
        rga.notifyDataSetChanged();
        if(rga.getItemCount()>0){
            export.setEnabled(true);
        }
    }

}
