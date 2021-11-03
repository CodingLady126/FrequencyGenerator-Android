package com.alexey.tonegenerator;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

public class ListAdapter extends ArrayAdapter<Program> {

    private int resourceLayout;
    private Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView lbIndex;
        EditText etMinFreq;
        EditText etMaxFreq;
        EditText etMinutes;
        ImageButton btnDelete;
    }

    public ListAdapter(Context context, int resource, List<Program> items) {
        super(context, resource, items);
        this.resourceLayout = resource;
        this.mContext = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Program p = getItem(position);
        Log.i("program: ", p.minFreq + ", " + p.maxFreq + ": " + p.minutes);

        ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.listview_row, parent, false);
            viewHolder.lbIndex = (TextView) convertView.findViewById(R.id.index);
            viewHolder.etMinFreq = (EditText) convertView.findViewById(R.id.etMin);
            viewHolder.etMaxFreq = (EditText) convertView.findViewById(R.id.etMax);
            viewHolder.etMinutes = (EditText) convertView.findViewById(R.id.etMinutes);
            viewHolder.btnDelete = (ImageButton) convertView.findViewById(R.id.btnDelete);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SettingsActivity) mContext).removeItem(position);
            }
        });

        viewHolder.lbIndex.setText(String.valueOf(position + 1));

        viewHolder.etMinFreq.setText(p.minFreq > -1 ? String.valueOf(p.minFreq) : "");
        viewHolder.etMinFreq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(viewHolder.etMinFreq.hasFocus()) {
                    int nMinFreq = s.toString().length() > 0 ? Integer.valueOf(s.toString()) : -1;
                    int nMaxFreq = viewHolder.etMaxFreq.getText().toString().length() > 0 ? Integer.valueOf(viewHolder.etMaxFreq.getText().toString()) : -1;
                    ((SettingsActivity) mContext).onChangeItem(position, nMinFreq, nMaxFreq, Integer.valueOf(viewHolder.etMinutes.getText().toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        viewHolder.etMaxFreq.setText(p.maxFreq > -1 ? String.valueOf(p.maxFreq) : "");
        viewHolder.etMaxFreq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(viewHolder.etMaxFreq.hasFocus()) {
                    int nMinFreq = viewHolder.etMinFreq.getText().toString().length() > 0 ? Integer.valueOf(viewHolder.etMinFreq.getText().toString()) : -1;
                    int nMaxFreq = s.toString().length() > 0 ? Integer.valueOf(s.toString()) : -1;
                    ((SettingsActivity) mContext).onChangeItem(position, nMinFreq, nMaxFreq, Integer.valueOf(viewHolder.etMinutes.getText().toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        viewHolder.etMinutes.setText(String.valueOf(p.minutes));

        viewHolder.etMinutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0 && viewHolder.etMinutes.hasFocus()) {
                    int nMinFreq = viewHolder.etMinFreq.getText().toString().length() > 0 ? Integer.valueOf(viewHolder.etMinFreq.getText().toString()) : -1;
                    int nMaxFreq = viewHolder.etMaxFreq.getText().toString().length() > 0 ? Integer.valueOf(viewHolder.etMaxFreq.getText().toString()) : -1;
                    ((SettingsActivity) mContext).onChangeItem(position, nMinFreq, nMaxFreq, Integer.valueOf(s.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return convertView;
    }

}