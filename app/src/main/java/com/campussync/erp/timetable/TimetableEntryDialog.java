package com.campussync.erp.timetable;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.campussync.erp.R;

public class TimetableEntryDialog {

    public interface OnSaveListener {
        void onSave(TimetableEntry entry);
    }

    public static void show(Context context,
                            TimetableEntry existing,
                            OnSaveListener listener) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_timetable_entry, null, false);

        Spinner spDay = view.findViewById(R.id.spDayOfWeek);
        EditText etStart = view.findViewById(R.id.etStartTime);
        EditText etEnd = view.findViewById(R.id.etEndTime);
        EditText etSubject = view.findViewById(R.id.etSubject);
        EditText etTeacher = view.findViewById(R.id.etTeacher);
        EditText etRoom = view.findViewById(R.id.etRoom);

        if (existing != null) {
            // pre-fill fields
            etStart.setText(existing.getStartTime());
            etEnd.setText(existing.getEndTime());
            etSubject.setText(existing.getSubjectName());
            etTeacher.setText(existing.getTeacherName());
            etRoom.setText(existing.getClassroom());
            // very simple: we won't pre-select day in spinner to keep it short
        }

        new AlertDialog.Builder(context)
                .setTitle(existing == null ? "Add Entry" : "Edit Entry")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    TimetableEntry e = existing != null ? existing : new TimetableEntry();

                    String day = spDay.getSelectedItem().toString();
                    e.setDayOfWeek(day);
                    e.setStartTime(etStart.getText().toString().trim());
                    e.setEndTime(etEnd.getText().toString().trim());
                    e.setSubjectName(etSubject.getText().toString().trim());
                    e.setTeacherName(etTeacher.getText().toString().trim());
                    e.setClassroom(etRoom.getText().toString().trim());

                    if (listener != null) listener.onSave(e);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
