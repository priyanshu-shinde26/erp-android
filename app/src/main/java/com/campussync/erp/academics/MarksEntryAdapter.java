package com.campussync.erp.academics;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;

import java.util.ArrayList;
import java.util.List;

public class MarksEntryAdapter
        extends RecyclerView.Adapter<MarksEntryAdapter.MarkViewHolder> {

    private final List<AcademicStudentModel> list = new ArrayList<>();
    private final boolean noTest; // ✅ Store the flag

    public MarksEntryAdapter(boolean noTest) {
        this.noTest = noTest;
    }

    // ================= PUBLIC METHODS =================

    public void addItems(List<AcademicStudentModel> page) {
        int start = list.size();
        list.addAll(page);
        notifyItemRangeInserted(start, page.size());
    }

    public List<AcademicStudentModel> getCurrentList() {
        return list;
    }

    public void clear() {
        list.clear();
        notifyDataSetChanged();
    }

    // ================= ADAPTER CORE =================

    @NonNull
    @Override
    public MarkViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_marks_entry, parent, false);

        return new MarkViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MarkViewHolder holder, int position) {

        AcademicStudentModel s = list.get(position);

        holder.tvName.setText(s.name);
        holder.tvRoll.setText("Roll: " + s.rollNo);

        // Remove listeners before setting values to prevent feedback loops during recycled view binding
        holder.cbAbsent.setOnCheckedChangeListener(null);
        holder.etMarks.removeTextChangedListener(holder.marksWatcher);

        // ✅ HANDLE "NO TEST" UI STATE
        if (noTest) {
            holder.etMarks.setEnabled(false);
            holder.etMarks.setText("");
            holder.cbAbsent.setChecked(true);
            holder.cbAbsent.setEnabled(false); // Optional: prevent user from unchecking
            s.enteredMarks = -1;
        } else {
            // Normal state restore
            holder.cbAbsent.setEnabled(true);
            holder.cbAbsent.setChecked(s.enteredMarks < 0);
            holder.etMarks.setEnabled(s.enteredMarks >= 0);
            holder.etMarks.setText(s.enteredMarks >= 0 ? String.valueOf(s.enteredMarks) : "");

            // ---------- ABSENT LISTENER ----------
            holder.cbAbsent.setOnCheckedChangeListener((b, checked) -> {
                if (checked) {
                    holder.etMarks.setEnabled(false);
                    holder.etMarks.setText("");
                    s.enteredMarks = -1;
                } else {
                    holder.etMarks.setEnabled(true);
                    s.enteredMarks = 0;
                }
            });

            // ---------- MARKS LISTENER ----------
            // Attach the watcher stored in ViewHolder
            holder.marksWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
                @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
                @Override
                public void afterTextChanged(Editable e) {
                    if (!holder.cbAbsent.isChecked()) {
                        try {
                            s.enteredMarks = Integer.parseInt(e.toString());
                        } catch (Exception ex) {
                            s.enteredMarks = 0;
                        }
                    }
                }
            };
            holder.etMarks.addTextChangedListener(holder.marksWatcher);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VIEW HOLDER =================

    static class MarkViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvRoll;
        EditText etMarks;
        CheckBox cbAbsent;
        TextWatcher marksWatcher; // ✅ Store reference to remove/add cleanly

        MarkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvRoll = itemView.findViewById(R.id.tvRollNo);
            etMarks = itemView.findViewById(R.id.etMarks);
            cbAbsent = itemView.findViewById(R.id.cbAbsent);
        }
    }
}