package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.MainDB;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.utility.FilterDuplicates;

public class ImportGripActivity extends AppCompatActivity implements FilterDuplicates.TaskResult {

    private Button import_file;
    private Switch no_duplicates;
    private Switch skip_name_check;
    private TextView import_status;
    private ProgressBar import_progress;

    private static final String TAG = "ImportGripActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_import_grip);

        bindUI();
        import_progress.setVisibility(View.INVISIBLE);
        import_status.setVisibility(View.INVISIBLE);

    }

    private void bindUI(){
        import_file = findViewById(R.id.btn_import);
        no_duplicates = findViewById(R.id.switch_import);
        skip_name_check = findViewById(R.id.switch_import_skipNames);
        import_status = findViewById(R.id.txt_import_status);
        import_progress = findViewById(R.id.import_progress);
        no_duplicates.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                skip_name_check.setEnabled(b);
            }
        });
        skip_name_check.setEnabled(no_duplicates.isChecked());
    }

    private void enableUI(boolean enable){
        import_file.setEnabled(enable);
        no_duplicates.setEnabled(enable);
    }

    private void toastResult(int added, int size){
        baseClass.toast(getApplicationContext(),
                added+" "+getString(R.string.import_added)+" "+size,true);
    }

    private boolean checkPos(int pos){
        return pos>=0 && pos<4;
    }

    private boolean checkString(int string){
        return string>=0 && string<6;
    }

    private boolean checkFinger(int finger){
        return finger>=-1 && finger<5;
    }

    private boolean checkStringsToHit(String strings){
        if(strings.length()!=6){return false;}
        for(int i=0;i<strings.length();i++){
            if(strings.charAt(i)!='0' && strings.charAt(i)!='1'){return false;}
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == Constants.FILE_SELECT_CODE){
            if(resultCode== Activity.RESULT_OK && null != data){
                Uri path = data.getData();
                readFile(path);
            }else{
                baseClass.toast(getApplicationContext(),
                        getString(R.string.import_no_file_selected),true);
                enableUI(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void storeInDB(ArrayList<Pair<Grip, Position[]>> pairArrayList){
        if(no_duplicates.isChecked()){
            FilterDuplicates fd = new FilterDuplicates(getApplicationContext(), pairArrayList,
                    skip_name_check.isChecked() ,import_progress,import_status,this);
            fd.execute();
        }else{
            //MainDB mainDB = baseClass.getDBinstance(getApplicationContext());
            int added = 0;
            int size = pairArrayList.size();

            for(Pair<Grip, Position[]> gripPair:pairArrayList){
                Grip cur_grip = gripPair.first;
                cur_grip.setDate(new Date());
                baseClass.storeGrip(getApplicationContext(),cur_grip,false,gripPair.second);
                added = added + 1;
            }
            toastResult(added,size);
            enableUI(true);
        }
    }

    private void handleJSON(JSONObject jsonObject){
        try {
            ArrayList<Pair<Grip, Position[]>> sample = new ArrayList<>();
            //Build grips and positions from file
            //Check if all values are valid
            //Call storeInDB with that arraylist
            if(!(jsonObject.has("version") &&
                    jsonObject.getInt("version")==Constants.DB_VERSION)){
                throw new JSONException("Version Key not found or incorrect");
            }
            if(!jsonObject.has("grips")){
                baseClass.toast(getApplicationContext(),R.string.import_no_grips);
                enableUI(true);
                return;
            }
            JSONArray jGrips = jsonObject.getJSONArray("grips");
            for(int i=0;i<jGrips.length();i++){
                JSONObject jGrip = jGrips.getJSONObject(i);
                Grip tmpGrip = new Grip();
                tmpGrip.setName(jGrip.getString("name"));
                if(checkStringsToHit(jGrip.getString("strings"))){
                    tmpGrip.setHitString(jGrip.getString("strings"));
                }else{
                    throw new JSONException("HitString value invalid");
                }
                Position[] positions = null;
                if(jGrip.has("positions")){
                    JSONArray tmpJArr = jGrip.getJSONArray("positions");
                    positions = new Position[tmpJArr.length()];
                    for(int j=0;j<tmpJArr.length();j++){
                        Position tmpPos = new Position();
                        JSONObject jO = tmpJArr.getJSONObject(j);
                        if(jO.has("string") && checkString(jO.getInt("string"))){
                            tmpPos.setString_number(jO.getInt("string"));
                        }else{
                            throw new JSONException("String value invalid");
                        }
                        if(jO.has("pos") && checkPos(jO.getInt("pos"))){
                            tmpPos.setPos(jO.getInt("pos"));
                        }else{
                            throw new JSONException("Position value invalid");
                        }
                        if(jO.has("finger") && checkFinger(jO.getInt("finger"))){
                            tmpPos.setFinger(jO.getInt("finger"));
                        }else{
                            throw new JSONException("Finger value invalid");
                        }
                        positions[j] = tmpPos;
                    }

                }
                sample.add(new Pair<>(tmpGrip,positions));
            }

            Log.d(TAG, "handleJSON: JSONOBJECT" + jsonObject.toString(2));
            storeInDB(sample);
        }catch (JSONException e){
            baseClass.toast(getApplicationContext(),
                    getString(R.string.import_error_in_json),true);
            enableUI(true);
        }
    }

    private void readFile(Uri uri){
        String raw_json;
        JSONObject jsonObject;
        try {
            InputStream fileInputStream = getContentResolver().openInputStream(uri);
            int size = fileInputStream.available();
            byte[] buffer = new byte[size];
            fileInputStream.read(buffer);
            fileInputStream.close();
            raw_json = new String(buffer, StandardCharsets.UTF_8);
            jsonObject = new JSONObject(raw_json);
            handleJSON(jsonObject);
        }catch(IOException e){
            baseClass.toast(getApplicationContext(),
                    getString(R.string.import_error_io),true);
            Log.e(TAG, "readFile: IO",e);
            enableUI(true);
        }catch(JSONException e){
            baseClass.toast(getApplicationContext(),
                    getString(R.string.import_error_json),true);
            Log.e(TAG, "readFile: JSON",e);
            enableUI(true);
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Import"),
                    Constants.FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            baseClass.toast(getApplicationContext(), "Please install a file selector");
        }
    }

    public void onClickSelectFile(View view) {
        enableUI(false);
        showFileChooser();
    }

    @Override
    public void TaskDone(int added, int size) {
        toastResult(added,size);
        import_progress.setVisibility(View.INVISIBLE);
        import_status.setVisibility(View.INVISIBLE);
        enableUI(true);
        no_duplicates.setChecked(false);
    }
}
