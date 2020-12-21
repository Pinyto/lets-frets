package de.tudarmstadt.tk.smartguitarcontrol.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;


import de.tudarmstadt.tk.smartguitarcontrol.R;

public class EditSaveFragment extends DialogFragment {
    private Button m_save_as_new;
    private Button m_save_overwrite;
    private Button m_close;
    private Button m_back;

    private static final String TAG = "EditSaveFragment";

    public interface AdvancedSaveMenuInterface{
        void onSave(EditSaveFragment dialog,boolean delete_old);
        void goBack(EditSaveFragment dialog);
    }

    private AdvancedSaveMenuInterface saveInterface;

    public EditSaveFragment(){

    }

    public static EditSaveFragment newInstance(String title){
        EditSaveFragment frag = new EditSaveFragment();
        Bundle args = new Bundle();
        args.putString("title",title);
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_save_edit_grip,container,false);
        bindUI(view);
        setListeners();
        return view;
    }

    private void setListeners(){
        m_save_as_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveInterface.onSave(EditSaveFragment.this,false);
            }
        });
        m_save_overwrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveInterface.onSave(EditSaveFragment.this,true);
            }
        });
        m_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveInterface.goBack(EditSaveFragment.this);
            }
        });
        m_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    private void bindUI(View view){
        m_save_as_new = view.findViewById(R.id.btn_ang_edit_save_as_new);
        m_save_overwrite = view.findViewById(R.id.btn_ang_edit_overwrite);
        m_close = view.findViewById(R.id.btn_ang_edit_abort);
        m_back = view.findViewById(R.id.btn_ang_edit_back);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            saveInterface = (AdvancedSaveMenuInterface) context;
        }catch (ClassCastException e){
            Log.w(TAG, "calling method need to implement interface!");
        }
    }
}
