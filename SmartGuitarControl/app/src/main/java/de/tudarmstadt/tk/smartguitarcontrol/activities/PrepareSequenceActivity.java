package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.adapter.RecyclerGripAdapter;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.utility.LoadGrip;

public class PrepareSequenceActivity extends AppCompatActivity implements LoadGrip.TaskResult {

    private RecyclerView rv;
    private Button m_continue;
    private TextView m_hint;
    private RecyclerGripAdapter rga;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_prepare_sequence);
        bindUI();
        m_continue.setEnabled(false);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        rga = new RecyclerGripAdapter(new RecyclerGripAdapter.customClickListener() {
            @Override
            public void onItemClick(int position) {
                rga.removeItem(position);
                rga.notifyDataSetChanged();
                checkIfEmpty();
            }
        });
        rv.setAdapter(rga);

    }

    private void bindUI(){
        rv = findViewById(R.id.recyler_ps);
        m_continue = findViewById(R.id.btn_ps_cont);
        m_hint = findViewById(R.id.txt_ps_hint);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode== Activity.RESULT_OK && requestCode==Constants.GET_GRIP_ID){
            if(null!=data && data.hasExtra(Constants.CUSTOM_EXTRA_GRIP)){
                long tmpID = data.getLongExtra(Constants.CUSTOM_EXTRA_GRIP, 0);
                LoadGrip lg = new LoadGrip(getApplicationContext(),tmpID,this);
                lg.execute();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkIfEmpty(){
        if(rga.getItemCount()>0){
            m_continue.setEnabled(true);
            m_hint.setVisibility(View.INVISIBLE);
        }else{
            m_continue.setEnabled(false);
            m_hint.setVisibility(View.VISIBLE);
        }
    }

    private long[] getAllGrips(){
        return rga.getAllGripIDS();
    }

    public void onClickAddGripToList(View view) {
        Intent get_grip = new Intent(this, ListOfGripsActivity.class);
        startActivityForResult(get_grip, Constants.GET_GRIP_ID);
    }

    public void onClickStartTraining(View view) {
        final Intent start_training = new Intent(this,TrainSingleActivity.class);
        start_training.putExtra(Constants.CUSTOM_MULTIPLE_GRIPS,getAllGrips());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.PS_enable_feedback_title)
                .setSingleChoiceItems(R.array.PS_feedback_options, 0, null)
                .setPositiveButton(R.string.GENERIC_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int choice = ((AlertDialog)dialogInterface).getListView().getCheckedItemPosition();
                        start_training.putExtra(Constants.CUSTOM_EXTRA_TSG_MODE,choice);
                        startActivity(start_training);
                    }
                })
                .setNeutralButton(R.string.GENERIC_back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    @Override
    public void TaskDone(Grip aGrip, Position[] positions) {
        rga.addPair(aGrip,positions);
        rga.notifyDataSetChanged();
        checkIfEmpty();
    }
}
