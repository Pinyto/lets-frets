package de.tudarmstadt.tk.smartguitarcontrol;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.room.Room;

import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.MainDB;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;

public class baseClass extends Application {
    public BluetoothService bluetoothService;
    private static MainDB mainDB;

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothService = new BluetoothService();
    }

    public static MainDB getDBinstance(Context context){
        if(null!=mainDB){
            return mainDB;
        }else{
            mainDB = Room.databaseBuilder(context,MainDB.class,"mainDB")
                    .fallbackToDestructiveMigration().build();
            return mainDB;
        }
    }

    public static void storeGrip(final Context context,final Grip grip,final Position... positions){
        new AsyncTask<Void,Void,Void>(){
            MainDB tmpDB;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                tmpDB = getDBinstance(context);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                toast(context,context.getString(R.string.GENERIC_save_successful));
            }

            @Override
            protected Void doInBackground(Void... voids) {
                long tmpGripID = tmpDB.gripDAO().addGrip(grip);
                for (Position cur_pos : positions){
                    cur_pos.setGrip_id(tmpGripID);
                    tmpDB.gripDAO().addPosition(cur_pos);
                }
                return null;
            }
        }.execute();
    }

    public static void storeGrip(final Context context, final Grip grip,final boolean notify, final Position... positions){
        new AsyncTask<Void,Void,Void>(){
            MainDB tmpDB;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                tmpDB = getDBinstance(context);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(notify) {
                    toast(context, context.getString(R.string.GENERIC_save_successful));
                }
            }

            @Override
            protected Void doInBackground(Void... voids) {
                long tmpGripID = tmpDB.gripDAO().addGrip(grip);
                for (Position cur_pos : positions){
                    cur_pos.setGrip_id(tmpGripID);
                    tmpDB.gripDAO().addPosition(cur_pos);
                }
                return null;
            }
        }.execute();
    }

    public static void deleteGripViaID(final Context context, final long grip_id, final boolean notify){
        new AsyncTask<Void,Void,Void>(){
            MainDB tmpDB;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                tmpDB = baseClass.getDBinstance(context);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if(notify){
                    toast(context,context.getString(R.string.GENERIC_delete_successful));
                }
                super.onPostExecute(aVoid);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                tmpDB.gripDAO().deleteGripViaID(grip_id);
                return null;
            }
        }.execute();
    }

    public static void toast(final Context context, final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void toast(final Context context, final String text,final boolean long_msg) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            public void run() {
                if(long_msg){
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public static void toast(final Context context, @StringRes final int id){
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, context.getString(id), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void toast(final Context context, @StringRes final int id, final boolean long_msg){
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            public void run() {
                if(long_msg){
                    Toast.makeText(context, context.getString(id), Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(context, context.getString(id), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
