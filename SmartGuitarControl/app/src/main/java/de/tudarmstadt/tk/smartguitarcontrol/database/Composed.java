package de.tudarmstadt.tk.smartguitarcontrol.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

@Entity(tableName = "Composed")
public class Composed {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "composition_name")
    private String name;

    @ColumnInfo(name = "tune_0",defaultValue = "E")
    private String tuning_0;

    @ColumnInfo(name = "tune_1", defaultValue = "A")
    private String tuning_1;

    @ColumnInfo(name = "tune_2",defaultValue = "D")
    private String tuning_2;

    @ColumnInfo(name = "tune_3",defaultValue = "G")
    private String tuning_3;

    @ColumnInfo(name = "tune_4",defaultValue = "B")
    private String tuning_4;

    @ColumnInfo(name = "tune_5",defaultValue = "E")
    private String tuning_5;

    private int BPM;

    //Maybe at Meter, Tempo, or more

    private String track_0;

    private String track_1;

    private String track_2;

    private String track_3;

    private String track_4;

    private String track_5;

    @TypeConverters({TimestampConverter.class})
    private Date date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTuning_0() {
        return tuning_0;
    }

    public void setTuning_0(String tuning_0) {
        this.tuning_0 = tuning_0;
    }

    public String getTuning_1() {
        return tuning_1;
    }

    public void setTuning_1(String tuning_1) {
        this.tuning_1 = tuning_1;
    }

    public String getTuning_2() {
        return tuning_2;
    }

    public void setTuning_2(String tuning_2) {
        this.tuning_2 = tuning_2;
    }

    public String getTuning_3() {
        return tuning_3;
    }

    public void setTuning_3(String tuning_3) {
        this.tuning_3 = tuning_3;
    }

    public String getTuning_4() {
        return tuning_4;
    }

    public void setTuning_4(String tuning_4) {
        this.tuning_4 = tuning_4;
    }

    public String getTuning_5() {
        return tuning_5;
    }

    public void setTuning_5(String tuning_5) {
        this.tuning_5 = tuning_5;
    }

    public int getBPM() {
        return BPM;
    }

    public void setBPM(int BPM) {
        this.BPM = BPM;
    }

    public String getTrack_0() {
        return track_0;
    }

    public void setTrack_0(String track_0) {
        this.track_0 = track_0;
    }

    public String getTrack_1() {
        return track_1;
    }

    public void setTrack_1(String track_1) {
        this.track_1 = track_1;
    }

    public String getTrack_2() {
        return track_2;
    }

    public void setTrack_2(String track_2) {
        this.track_2 = track_2;
    }

    public String getTrack_3() {
        return track_3;
    }

    public void setTrack_3(String track_3) {
        this.track_3 = track_3;
    }

    public String getTrack_4() {
        return track_4;
    }

    public void setTrack_4(String track_4) {
        this.track_4 = track_4;
    }

    public String getTrack_5() {
        return track_5;
    }

    public void setTrack_5(String track_5) {
        this.track_5 = track_5;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
