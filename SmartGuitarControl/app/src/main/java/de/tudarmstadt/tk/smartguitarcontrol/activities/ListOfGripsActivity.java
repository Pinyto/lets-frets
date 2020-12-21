package de.tudarmstadt.tk.smartguitarcontrol.activities;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.adapter.RecyclerGripAdapter;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.utility.LoadAllGrips;

public class ListOfGripsActivity extends Activity implements LoadAllGrips.TaskResult {
    
    private static RecyclerGripAdapter m_adapter;

    private static final String TAG = "ListOfGripsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_grips);
        setResult(Activity.RESULT_CANCELED);

        RecyclerView recyclerView = findViewById(R.id.recycler_grips);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        m_adapter = new RecyclerGripAdapter(new RecyclerGripAdapter.customClickListener() {
            @Override
            public void onItemClick(int position) {
                Grip grip = m_adapter.getGripViaPosition(position);
                Intent intent = new Intent();
                intent.putExtra(Constants.CUSTOM_EXTRA_GRIP,grip.getId());
                setResult(Activity.RESULT_OK,intent);
                finishActivity(RESULT_OK);
                finish();
            }
        });
        recyclerView.setAdapter(m_adapter);
        LoadAllGrips lag = new LoadAllGrips(getApplicationContext(), this);
        lag.execute();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void TaskDone(ArrayList<Pair<Grip, Position[]>> pairArrayList) {
        m_adapter.addPairCollection(pairArrayList);
        m_adapter.notifyDataSetChanged();
        Log.d(TAG, "TaskDone: added all pair!");
    }
}
