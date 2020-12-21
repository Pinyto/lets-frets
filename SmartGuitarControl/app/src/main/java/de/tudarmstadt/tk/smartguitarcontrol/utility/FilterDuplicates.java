package de.tudarmstadt.tk.smartguitarcontrol.utility;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;

import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.MainDB;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;


public class FilterDuplicates extends AsyncTask<Void,Integer,Void> {

    public interface TaskResult{
        void TaskDone(int added, int size);
    }

    private static final String TAG = "FilterDuplicates";

    private WeakReference<Context> weakReference;
    private WeakReference<ProgressBar> progressBar;
    private WeakReference<TextView> statusText;
    private MainDB mainDB;
    private ArrayList<Pair<Grip, Position[]>> samples;
    private static boolean skipName;
    private int sampleSize;
    private int count_added;
    private ArrayList<Pair<Grip, Position[]>> data_set;
    private TaskResult tr;


    public FilterDuplicates(Context context, ArrayList<Pair<Grip, Position[]>> pairArrayList,
                            boolean skipNameCheck, ProgressBar progress, TextView status,
                            FilterDuplicates.TaskResult tr){
        weakReference = new WeakReference<>(context);
        samples = pairArrayList;
        sampleSize = pairArrayList.size();
        skipName = skipNameCheck;
        progressBar = new WeakReference<>(progress);
        statusText = new WeakReference<>(status);
        this.tr = tr;
        count_added = 0;
    }

    private String buildStatus(int index){
        return index + " / " + sampleSize;
    }

    private void updateProgress(int index){
        progressBar.get().setProgress(index);
        statusText.get().setText(buildStatus(index));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mainDB = baseClass.getDBinstance(weakReference.get());
        progressBar.get().setVisibility(View.VISIBLE);
        progressBar.get().setMax(sampleSize);
        progressBar.get().setProgress(0);
        statusText.get().setText(buildStatus(0));
        statusText.get().setVisibility(View.VISIBLE);
        data_set = new ArrayList<>();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        tr.TaskDone(count_added,sampleSize);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        updateProgress(values[0]);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Grip[] allGrips = mainDB.gripDAO().getAllGrips();
        for(Grip cur_grip:allGrips){
            Position[] tmpPos = mainDB.gripDAO().getPositionsViaGrip(cur_grip.getId());
            data_set.add(new Pair<>(cur_grip,tmpPos));
        }
        int i;

        for(i=0;i<sampleSize;i++){
            Pair<Grip,Position[]> curSample = samples.get(i);
            if(!gripHasDuplicate(curSample, data_set)){
                curSample.first.setDate(new Date());
                long id = mainDB.gripDAO().addGrip(curSample.first);
                for(Position tmpPos:curSample.second){
                    tmpPos.setGrip_id(id);
                    mainDB.gripDAO().addPosition(tmpPos);
                }
                count_added = count_added + 1;
            }else{
                Log.i(TAG, "doInBackground: Duplicate Object, not added");
            }
            publishProgress(i+1);
        }
        return null;
    }

    private static boolean gripHasDuplicate(Pair<Grip, Position[]> sample, ArrayList<Pair<Grip,Position[]>> dataBase){

        if(dataBase.size()==0)return false;
        for(Pair<Grip,Position[]> pair:dataBase){
            if(!skipName){
                if(!sample.first.getName().equalsIgnoreCase(pair.first.getName())){continue;}
            }
            if(!sample.first.getHitString().equals(pair.first.getHitString())){continue;}
            if(!(sample.second.length==pair.second.length)){continue;}
            for(Position sample_pos:sample.second){
                boolean inside = false;
                for(Position pair_pos:pair.second){
                    if(sample_pos.equals(pair_pos)){
                        inside = true;
                        break;
                    }
                }
                if(!inside){
                    //Position does not exist, return false
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
