package de.tudarmstadt.tk.smartguitarcontrol.fragments;



import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;


import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;

import de.tudarmstadt.tk.smartguitarcontrol.Constants;
import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.activities.EditGripActivity;
import de.tudarmstadt.tk.smartguitarcontrol.activities.ExportAllActivity;
import de.tudarmstadt.tk.smartguitarcontrol.activities.ImportGripActivity;
import de.tudarmstadt.tk.smartguitarcontrol.activities.ListOfGripsActivity;
import de.tudarmstadt.tk.smartguitarcontrol.activities.ManageGripActivity;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditDatabase extends DialogFragment {
    private Button m_export;
    private Button m_export_all;
    private Button m_import;
    private Button m_manage;

    private Context m_context;

    private static final String TAG = "EditDatabase";

    public EditDatabase() {
        // Required empty public constructor
    }

    public static EditDatabase newInstance(String title) {
        EditDatabase frag = new EditDatabase();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_database, container, false);
        bindUI(view);
        setListeners();
        return view;
    }

    private void bindUI(View view){
        m_manage = view.findViewById(R.id.btn_edit_db_manage);
        m_export = view.findViewById(R.id.btn_edit_db_export);
        m_export_all = view.findViewById(R.id.btn_edit_db_export_all);
        m_import = view.findViewById(R.id.btn_edit_db_import);
    }

    private void setListeners(){
        m_manage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(m_context, ManageGripActivity.class);
                startActivity(intent);
            }
        });
        m_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(m_context, ImportGripActivity.class);
                startActivity(intent);
            }
        });
        m_export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(m_context, ExportAllActivity.class);
                intent.putExtra(Constants.CUSTOM_EXPORT_ALL, false);
                startActivity(intent);
            }
        });
        m_export_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(m_context, ExportAllActivity.class);
                intent.putExtra(Constants.CUSTOM_EXPORT_ALL,true);
                startActivity(intent);
            }
        });
    }

    private void startActivity(long grip_id){
        Intent editGripActivity = new Intent(m_context, EditGripActivity.class);
        editGripActivity.putExtra(Constants.CUSTOM_EXTRA_GRIP,grip_id);
        startActivity(editGripActivity);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode==Constants.GET_GRIP_ID) {
            if(resultCode==Activity.RESULT_OK){
                long id =  data.getLongExtra(Constants.CUSTOM_EXTRA_GRIP,0);
                baseClass.toast(getContext(),"Id of grip: "+id);
                startActivity(id);
            }else{
                Log.w(TAG, "onActivityResult: result not okay:"+resultCode);
            }
        } else if (requestCode==Constants.GET_GRIP_FOR_DELETE) {
            if(resultCode==Activity.RESULT_OK){
                long id = data.getLongExtra(Constants.CUSTOM_EXTRA_GRIP, 0);
                baseClass.deleteGripViaID(m_context,id,true);
            }
        } else if(requestCode==Constants.FILE_STORAGE_SELECT){
            if(resultCode==Activity.RESULT_OK){
                baseClass.toast(m_context,"Worked");
            }else{
                baseClass.toast(m_context, "didn't worked");
            }
        } else {
            Log.w(TAG, "onActivityResult: wrong request code?");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_context = context;
    }


}
