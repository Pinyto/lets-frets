package de.tudarmstadt.tk.smartguitarcontrol.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.widget.AmbilWarnaPreference;

/**
 * A simple {@link Fragment} subclass.
 */
public class ColorSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener {

    private Preference[] mPreferences;
    private SharedPreferences sp;
    private Context mContext;
    private static final String TAG = "ColorSettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.color_preferences,rootKey);
        Preference c0 = findPreference("color_f0");
        Preference c1 = findPreference("color_f1");
        Preference c2 = findPreference("color_f2");
        Preference c3 = findPreference("color_f3");
        Preference c4 = findPreference("color_f4");
        Preference cAny = findPreference("color_any");
        mPreferences = new Preference[]{c0,c1,c2,c3,c4,cAny};
    }

    private void setupPreferences(){
        for(Preference pref:mPreferences){
            pref.setOnPreferenceClickListener(this);
            setCustomIcon(pref,sp.getInt(pref.getKey(),Color.BLUE));
        }
    }

    private void setCustomIcon(Preference pref,int color){
        Drawable drawable = pref.getIcon();
        drawable.setTint(color);
        pref.setIcon(drawable);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        final String key = preference.getKey();
        int old = sp.getInt(key, Color.BLUE);
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(mContext, old,
                new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                //do nothing
                dialog.getDialog().dismiss();
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(key,color);
                editor.apply();

                setCustomIcon(preference, color);
                dialog.getDialog().dismiss();

            }
        });
        dialog.show();
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        setupPreferences();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }


}
