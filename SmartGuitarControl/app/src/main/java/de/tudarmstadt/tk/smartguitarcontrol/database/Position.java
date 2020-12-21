package de.tudarmstadt.tk.smartguitarcontrol.database;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "Positions", foreignKeys = @ForeignKey(entity = Grip.class,parentColumns = "id", childColumns = "grip_id", onDelete = CASCADE), indices = {@Index("grip_id")})
public class Position {

    @PrimaryKey(autoGenerate = true)
    public long id;

    private long grip_id;

    private int string_number;

    //0-4 thumb to pinky; -1 = any

    private int finger;

    private int pos;

    //Getters and Setters

    public long getId() {
        return id;
    }

    public long getGrip_id() {
        return grip_id;
    }

    public void setGrip_id(long grip_id) {
        this.grip_id = grip_id;
    }

    public int getFinger() {
        return finger;
    }

    public void setFinger(int finger) {
        this.finger = finger;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getString_number() {
        return string_number;
    }

    public void setString_number(int string_number) {
        this.string_number = string_number;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(null==obj){
            return false;
        }
        if(obj instanceof Position){
            Position tmpPosition = (Position) obj;
            return (this.finger == tmpPosition.getFinger() &&
                    this.pos == tmpPosition.getPos() &&
                    this.string_number == tmpPosition.getString_number());
        }else{
            return false;
        }
    }
}
