package de.tudarmstadt.tk.smartguitarcontrol.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface GripDAO {
    //insert method for db add,remove,change etc.

    @Insert
    long addGrip(Grip grip);

    @Insert
    void addPosition(Position position);

    @Update
    void updateGrip(Grip grip);

    @Delete
    void deleteGrip(Grip grip);

    @Query("SELECT * FROM grips")
    Grip[] getAllGrips();

    @Query("SELECT * FROM grips WHERE id=:query_grip_id")
    Grip getGrip(long query_grip_id);

    @Query("SELECT * FROM positions WHERE grip_id=:query_grip_id")
    Position[] getPositionsViaGrip(long query_grip_id);

    @Query("DELETE FROM grips WHERE id = :query_grip_id")
    void deleteGripViaID(long query_grip_id);
}
