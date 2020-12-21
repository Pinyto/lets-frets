package de.tudarmstadt.tk.smartguitarcontrol.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;


@Database(entities = {Grip.class, Position.class}, version = Constants.DB_VERSION,exportSchema = false)
public abstract class MainDB extends RoomDatabase {


    public abstract GripDAO gripDAO();


}
