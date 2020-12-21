package de.tudarmstadt.tk.smartguitarcontrol.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Date;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.MainDB;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.fragments.ConfirmSaveFragment;
import de.tudarmstadt.tk.smartguitarcontrol.fragments.EditSaveFragment;
import de.tudarmstadt.tk.smartguitarcontrol.utility.TextValidator;
import de.tudarmstadt.tk.smartguitarcontrol.views.TabBoard;

public class EditGripActivity extends AppCompatActivity implements ConfirmSaveFragment.DialogListener,
        EditSaveFragment.AdvancedSaveMenuInterface {
    private static TabBoard tabboard;

    private static EditText m_gripname;
    private TextView m_headline;

    private static ArrayList<Position> allPositions;

    private Button m_btn_savemenu;
    private Button m_btn_submenu;

    private Vibrator vibrator;
    private static final String TAG = "EditGripActivity";

    private long m_loadedGripID; //the loaded grip
    private boolean m_extraGiven; //set if a grip is loaded

    private boolean dragAdd = false;
    private float dragBase = 0;
    protected float positionX = 0;
    protected float positionY = 0;
    private float positionOutX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_edit_grip);

        m_extraGiven = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            m_extraGiven = true;
            m_loadedGripID = extras.getLong(Constants.CUSTOM_EXTRA_GRIP);
        }

        bindUI();
        setListeners();

        m_btn_savemenu.setEnabled(false);
        m_gripname.setError(getString(R.string.warn_inp_empty_name));
        if(m_extraGiven)m_headline.setText(R.string.ANG_headline_edit);

        allPositions = new ArrayList<>();
        
        registerForContextMenu(m_btn_submenu);
    }

    private void bindUI(){
        tabboard = findViewById(R.id.ang_tabboard);
        m_gripname = findViewById(R.id.input_ang_name);

        m_headline = findViewById(R.id.txt_ang_headline);

        m_btn_savemenu = findViewById(R.id.btn_ang_savemenu);
        m_btn_submenu = findViewById(R.id.btn_ang_submenu);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void setListeners(){
        m_gripname.addTextChangedListener(new TextValidator(m_gripname) {
            @Override
            public void validate(TextView textView, String text) {
                if(null == text || text.length()==0){
                    textView.setError(getString(R.string.warn_inp_empty_name));
                    m_btn_savemenu.setEnabled(false);
                    return;
                }
                textView.setError(null);
                m_btn_savemenu.setEnabled(true);
            }
        });
        tabboard.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        positionX = motionEvent.getX();
                        positionY = motionEvent.getY();
                        dragAdd = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if(dragAdd){
                            if(Math.abs(dragBase-motionEvent.getX())>tabboard.getPartWidth()){
                                vibrator.vibrate(100);
                                dragBase = motionEvent.getX();
                            }
                        }
                        if(Math.abs(positionX-motionEvent.getX())>tabboard.getPartWidth()){
                            if(!dragAdd){
                                vibrator.vibrate(100);
                                baseClass.toast(getApplicationContext(),R.string.ANG_drag_hint,true);
                                dragBase = motionEvent.getX();
                                dragAdd = true;
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        positionOutX = motionEvent.getX();
                        showPopup();
                        return true;
                }
/*                if (motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    positionX = motionEvent.getX();
                    positionY = motionEvent.getY();
                    view.showContextMenu();
                    //X is right
                    //Y is downwards
                    // 0,0 is top left
                }*/
                //return false to trigger additional onclick methods
                return false;
            }
        });
    }

    private String getStringsToHit(){
        return tabboard.getStringsToHit();
    }

    private boolean anyStringsSet(){
        String state = tabboard.getStringsToHit();
        for(int i=0;i<state.length();i++){
            if(state.charAt(i)=='1'){
                return true;
            }
        }
        return false;
    }

    private void resetEverything(boolean notify){
        boolean positionsCleared = false;
        m_gripname.setText("");
        if(!allPositions.isEmpty()){
            allPositions.clear();
            positionsCleared = true;
            tabboard.resetInternalData();
            tabboard.invalidate();
        }
        tabboard.resetInternalData();
        tabboard.invalidate();
        if(notify){
            if(positionsCleared){
                Toast.makeText(this,
                        R.string.ANG_toast_positions_reset,Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this,
                        R.string.ANG_toast_positions_reset_dismiss,Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addPositionFromCurrentValues(int fingerID){
        int[][] data;
        if(dragAdd){
            data = tabboard.handleOnClick(positionX,positionY,fingerID,positionOutX);
        }else{
            data = tabboard.handleOnClick(positionX,positionY,fingerID);
        }
        tabboard.invalidate();
        if(data==null){return;}
        for(int[] single_data:data){
            if(null==single_data){
                return;
            }
            int fret = single_data[0];
            int string = single_data[1];
            if(fingerID==-2){
                boolean found = false;
                for(Position tmpPosition:allPositions){
                    if( tmpPosition.getPos()==fret && tmpPosition.getString_number()==string ){
                        found = true;
                        allPositions.remove(tmpPosition);
                        break;
                    }
                }
                if(!found){
                    Toast.makeText(this,R.string.ANG_toast_warning_no_position_to_remove,
                            Toast.LENGTH_SHORT).show();
                }
            }else{
                Position tmpPosition = new Position();

                tmpPosition.setString_number(string);
                tmpPosition.setPos(fret);
                tmpPosition.setFinger(fingerID);
                allPositions.add(tmpPosition);
            }
        }

    }

    private void addBarre(){
        //TODO: Eventually a feature
        Toast.makeText(this, "Not implemented yet",Toast.LENGTH_SHORT).show();
    }

    private void showPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(EditGripActivity.this);
        boolean onTopRow = tabboard.isClickOnTopRow(positionY);
        if(onTopRow){
            builder.setTitle(R.string.ANG_toggle_string_top_row)
                    .setSingleChoiceItems(R.array.top_row_options, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            handleFingerSelectTop(i);
                        }
                    }).setNeutralButton(R.string.GENERIC_back, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).show();
        }else{
            builder.setTitle(R.string.ANG_select_finger)
                    .setSingleChoiceItems(R.array.fingers,-1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            handleFingerSelect(i);
                        }
                    }).setNeutralButton(R.string.GENERIC_back, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).show();
        }
    }

    private void handleFingerSelect(int i){
        if(i>=0 && i<=4){
            addPositionFromCurrentValues(i);
        }else{
            switch (i){
                case 5:
                    addPositionFromCurrentValues(-1);
                    break;
                case 6:
                    addPositionFromCurrentValues(-2);
                    break;
            }
        }
    }

    private void handleFingerSelectTop(int i){
        if(i==0){
            //Activate top row through minus one selection
            addPositionFromCurrentValues(-1);
        }else if(i==1){
            //Remove entry on top row if possible
            addPositionFromCurrentValues(-2);
        }
    }

    private synchronized void storeInDB(boolean deleteOld){
        Grip tmpGrip = new Grip();
        tmpGrip.setName(m_gripname.getText().toString());
        tmpGrip.setHitString(getStringsToHit());
        tmpGrip.setDate(new Date());

        baseClass.storeGrip(getApplicationContext(),
                tmpGrip,
                allPositions.toArray(new Position[allPositions.size()] ));
        if(deleteOld && m_extraGiven){
            baseClass.deleteGripViaID(getApplicationContext(),
                    m_loadedGripID, false);
        }
    }

    private static synchronized void loadFromExisting(final Context context, final long grip_id){
        new AsyncTask<Void,Void,Void>(){
            MainDB tmpBase;
            Grip tmpGrip;
            Position[] tmpPositions;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                tmpBase = baseClass.getDBinstance(context);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                m_gripname.setText(tmpGrip.getName());

                for(Position cur_pos:tmpPositions){
                    Position tmpPosition = new Position();
                    tmpPosition.setFinger(cur_pos.getFinger());
                    tmpPosition.setPos(cur_pos.getPos());
                    tmpPosition.setString_number(cur_pos.getString_number());
                    allPositions.add(tmpPosition);
                    tabboard.handleSingleData(tmpPosition);
                }
                tabboard.setStringsToHit(tmpGrip.getHitString());
                tabboard.invalidate();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                tmpGrip = tmpBase.gripDAO().getGrip(grip_id);
                tmpPositions = tmpBase.gripDAO().getPositionsViaGrip(grip_id);
                return null;
            }
        }.execute();
    }

    public void onClickOptionsSubmenu(View view) {
        openContextMenu(view);
    }

    public void onClickSaveSubmenu(View view) {
        if(!anyStringsSet()){
            Toast.makeText(this,R.string.ANG_toast_warning_no_strings,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if(m_extraGiven){
            FragmentTransaction fT = getSupportFragmentManager().beginTransaction();
            EditSaveFragment esf = EditSaveFragment.newInstance("Title");
            esf.show(fT,"advancedSaveDialog");
        }else{
            FragmentManager fragmentManager = getSupportFragmentManager();
            ConfirmSaveFragment csf = new ConfirmSaveFragment();
            csf.show(fragmentManager,"saveDialog");
        }
    }

    public void onClickShowHint(View view) {
        Toast.makeText(this,R.string.ANG_hint,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v.getId()==tabboard.getId()){
            vibrator.vibrate(50);
            getMenuInflater().inflate(R.menu.finger_menu,menu);
        }else if(v.getId()==m_btn_submenu.getId()){
            getMenuInflater().inflate(R.menu.ang_options_menu,menu);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_ang_reset:
                resetEverything(true);
                return true;
            case R.id.menu_ang_add_barre:
                addBarre();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDialogPositive(DialogFragment dialog) {
        storeInDB(false);
        dialog.dismiss();
        resetEverything(false);
    }

    @Override
    public void onDialogNegative(DialogFragment dialog) {
        storeInDB(false);
        dialog.dismiss();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(m_extraGiven){
            loadFromExisting(this,m_loadedGripID);
        }
    }

    @Override
    public void onSave(EditSaveFragment dialog, boolean delete_old) {
        storeInDB(delete_old);
        dialog.dismiss();
        finish();
    }

    @Override
    public void goBack(EditSaveFragment dialog) {
        dialog.dismiss();
        finish();
    }

}
