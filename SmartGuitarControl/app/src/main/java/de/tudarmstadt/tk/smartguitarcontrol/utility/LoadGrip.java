package de.tudarmstadt.tk.smartguitarcontrol.utility;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.MainDB;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;

public class LoadGrip extends AsyncTask<Void,Void,Void> {

    public interface TaskResult{
        void TaskDone(Grip aGrip, Position[] positions);
    }

    private long m_grip_id;
    private TaskResult tr;
    private WeakReference<Context> weakReference;
    private MainDB mainDB;
    private Grip m_grip;
    private Position[] positions;

    public LoadGrip(Context context,long id,TaskResult tr){
        weakReference = new WeakReference<>(context);
        m_grip_id = id;
        this.tr = tr;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mainDB = baseClass.getDBinstance(weakReference.get());
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        tr.TaskDone(m_grip,positions);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        m_grip = mainDB.gripDAO().getGrip(m_grip_id);
        positions = mainDB.gripDAO().getPositionsViaGrip(m_grip_id);
        return null;
    }
}
