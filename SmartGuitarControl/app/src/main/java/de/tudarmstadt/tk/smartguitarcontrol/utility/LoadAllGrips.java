package de.tudarmstadt.tk.smartguitarcontrol.utility;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.MainDB;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;

public class LoadAllGrips extends AsyncTask<Void,Void,Void> {

    public interface TaskResult{
        void TaskDone(ArrayList<Pair<Grip, Position[]>> pairArrayList);
    }

    private TaskResult tr;
    private WeakReference<Context> weakReference;
    private MainDB mainDB;
    private ArrayList<Pair<Grip,Position[]>> m_storage;

    public LoadAllGrips(Context context,TaskResult tr){
        weakReference = new WeakReference<>(context);
        this.tr = tr;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mainDB = baseClass.getDBinstance(weakReference.get());
        m_storage = new ArrayList<>();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        tr.TaskDone(m_storage);
    }

    protected Void doInBackground(Void... voids) {
        Grip[] tmpGripArray = mainDB.gripDAO().getAllGrips();
        for(Grip c_grip:tmpGripArray){
            m_storage.add(
                    new Pair<>(
                            c_grip,
                            mainDB.gripDAO().getPositionsViaGrip(c_grip.getId())));
        }
        return null;
    }
}
