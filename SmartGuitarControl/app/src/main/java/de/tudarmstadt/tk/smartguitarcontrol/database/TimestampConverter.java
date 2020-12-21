package de.tudarmstadt.tk.smartguitarcontrol.database;

import androidx.room.TypeConverter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimestampConverter {
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMANY);

    @TypeConverter
    public static Date fromTimestamp(String value){
        if(value != null){
            try{
                return df.parse(value);
            }catch (ParseException e){
                e.printStackTrace();
            }
        }
        return null;
    }

    @TypeConverter
    public static String dateToTimestamp(Date value){
        return df.format(value);
    }
}
