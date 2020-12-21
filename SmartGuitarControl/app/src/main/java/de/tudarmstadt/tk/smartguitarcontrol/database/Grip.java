package de.tudarmstadt.tk.smartguitarcontrol.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity(tableName = "Grips")
public class Grip {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "grip_name")
    private String name;

    @ColumnInfo(name = "strings_to_hit")
    private String hitString;

    @TypeConverters({TimestampConverter.class})
    private Date date;

    // Getters and Setters

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHitString() {
        return hitString;
    }

    public void setHitString(String hitString) {
        this.hitString = hitString;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}

