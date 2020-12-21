package de.tudarmstadt.tk.smartguitarcontrol.fragments;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;


import de.tudarmstadt.tk.smartguitarcontrol.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConfirmSaveFragment extends DialogFragment {

    public interface DialogListener {
        void onDialogPositive(DialogFragment dialog);
        void onDialogNegative(DialogFragment dialog);
    }

    private DialogListener listener;
    public ConfirmSaveFragment() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.ANG_dialog_main).setMessage(R.string.ANG_dialog_text)
                .setPositiveButton(R.string.GENERIC_yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            listener.onDialogPositive(ConfirmSaveFragment.this);
                        }
                    })
                .setNegativeButton(R.string.GENERIC_no,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            listener.onDialogNegative(ConfirmSaveFragment.this);
                        }
                    })
                .setNeutralButton(R.string.GENERIC_abort,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();
                        }
                    });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            listener = (DialogListener) context;
        }catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement interface");
        }
    }
}
