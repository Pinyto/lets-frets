package de.tudarmstadt.tk.smartguitarcontrol.utility;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

public abstract class TextValidator implements TextWatcher {
    private final TextView m_textView;

    public TextValidator(TextView aTextView){
        this.m_textView = aTextView;
    }

    public abstract void validate(TextView textView, String text);

    @Override
    final public void afterTextChanged(Editable s){
        validate(m_textView, m_textView.getText().toString());
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Empty
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Empty
    }
}
