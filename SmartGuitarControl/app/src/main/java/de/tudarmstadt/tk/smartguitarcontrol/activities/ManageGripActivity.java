package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.adapter.RecyclerGripAdapter;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.utility.LoadAllGrips;

public class ManageGripActivity extends AppCompatActivity implements LoadAllGrips.TaskResult,
        RecyclerGripAdapter.customClickListener {
    private RecyclerView rv;
    private RecyclerGripAdapter rga;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(null!=actionBar){
            actionBar.hide();
        }
        setContentView(R.layout.activity_manage_grip);
        bindUI();
        rga = new RecyclerGripAdapter(this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        rv.setAdapter(rga);
        rv.setLayoutManager(manager);
    }

    private void bindUI(){
        rv = findViewById(R.id.mg_rcy_list);
    }

    private void startEditActivity(long id){
        Intent intent = new Intent(this, EditGripActivity.class);
        intent.putExtra(Constants.CUSTOM_EXTRA_GRIP, id);
        startActivity(intent);
    }

    @Override
    public void TaskDone(ArrayList<Pair<Grip, Position[]>> pairArrayList) {
        rga.clear();
        rga.addPairCollection(pairArrayList);
        rga.notifyDataSetChanged();
    }

    public void onClickAddGrip(View view) {
        Intent intent = new Intent(view.getContext(), EditGripActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        reloadAdapter();
        super.onResume();
    }

    private void reloadAdapter(){
        LoadAllGrips lg = new LoadAllGrips(this,this);
        lg.execute();
    }

    private void deleteItem(int position){
        baseClass.deleteGripViaID(this,rga.getGripViaPosition(position).getId(),true);
        reloadAdapter();
    }

    @Override
    public void onItemClick(int position) {
        startEditActivity(rga.getGripViaPosition(position).getId());
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case Constants.COTEXT_MENU_DELETE_GRIP:
                deleteItem(item.getGroupId());
            default:
                return super.onContextItemSelected(item);
        }
    }


}
