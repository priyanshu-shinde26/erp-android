package com.campussync.erp.timetable;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import com.campussync.erp.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class TimetableEntryDialog {

    public interface OnSaveListener {
        void onSave(SchedulePeriod period);
    }

    public static void showAdd(Context context, String classId, String day, OnSaveListener listener) {
        // ✅ day will be overridden by system day inside show()
        show(context, classId, day, null, listener);
    }

    public static void showEdit(Context context, String classId, String day, SchedulePeriod existing, OnSaveListener listener) {
        show(context, classId, day, existing, listener);
    }

    private static void show(Context context, String classId, String day, SchedulePeriod existing, OnSaveListener listener) {

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_timetable_entry, null, false);

        //Spinner spDay = view.findViewById(R.id.spDayOfWeek);
        TextInputEditText etStart = view.findViewById(R.id.etStartTime);
        TextInputEditText etEnd = view.findViewById(R.id.etEndTime);
        TextInputEditText etSubject = view.findViewById(R.id.etSubject);
        TextInputEditText etTeacher = view.findViewById(R.id.etTeacher);

        TextInputLayout tilStart = view.findViewById(R.id.tilStartTime);
        TextInputLayout tilEnd = view.findViewById(R.id.tilEndTime);
        TextInputLayout tilSubject = view.findViewById(R.id.tilSubject);
        TextInputLayout tilTeacher = view.findViewById(R.id.tilTeacher);

        // --- Spinner setup ---
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.days_of_week, android.R.layout.simple_spinner_dropdown_item
        );
       // spDay.setAdapter(adapter);

        // ✅ For ADD: use system day
        // ✅ For EDIT: use passed day (current selectedDay)
        String dayKey = (existing == null) ? getTodayKey() : (day == null ? "" : day.trim().toLowerCase());

        // Convert "monday" -> "Monday" to match arrays.xml values
        String dayDisplay = capitalize(dayKey);

        // ✅ set selection by value (case-safe) [web:700]
        int dayIndex = adapter.getPosition(dayDisplay);
      //  spDay.setSelection(dayIndex >= 0 ? dayIndex : 0);

        // ✅ lock day spinner always
       // spDay.setEnabled(false);
       // spDay.setClickable(false);

        // --- Prefill for edit ---
        if (existing != null) {
            etSubject.setText(nullToEmpty(existing.subject));
            etTeacher.setText(nullToEmpty(existing.teacher));
            etStart.setText(nullToEmpty(existing.startTime));
            etEnd.setText(nullToEmpty(existing.endTime));
        }

        // --- Time pickers ---
        Calendar cal = Calendar.getInstance();

        etStart.setOnClickListener(v -> openTimePicker(context, cal, time -> {
            tilStart.setError(null);
            etStart.setText(time);
        }));

        etEnd.setOnClickListener(v -> openTimePicker(context, cal, time -> {
            tilEnd.setError(null);
            etEnd.setText(time);
        }));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(existing == null ? "➕ Add Period (" + dayDisplay + ")" : "✏️ Edit Period")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("💾 Save", null)
                .setNegativeButton("❌ Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                // clear old errors
                if (tilSubject != null) tilSubject.setError(null);
                if (tilTeacher != null) tilTeacher.setError(null);
                tilStart.setError(null);
                tilEnd.setError(null);

                String subject = text(etSubject);
                String teacher = text(etTeacher);
                String start = text(etStart);
                String end = text(etEnd);

                boolean ok = true;

                if (TextUtils.isEmpty(subject)) {
                    if (tilSubject != null) tilSubject.setError("Subject required");
                    else etSubject.setError("Subject required");
                    ok = false;
                }

                if (TextUtils.isEmpty(start)) {
                    tilStart.setError("Start time required");
                    ok = false;
                }

                if (TextUtils.isEmpty(end)) {
                    tilEnd.setError("End time required");
                    ok = false;
                }

                if (!ok) return;

                // ✅ Keep existing object for edit so id stays
                SchedulePeriod period = (existing != null) ? existing : new SchedulePeriod();

                period.subject = subject;
                period.teacher = teacher;
                period.startTime = start;
                period.endTime = end;

                // OPTIONAL: if you add this field in SchedulePeriod, you can store day too
                // period.day = dayKey;

                if (listener != null) listener.onSave(period);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    // -------- helpers --------

    private interface TimeCallback {
        void onTime(String time);
    }

    private static void openTimePicker(Context context, Calendar cal, TimeCallback cb) {
        new TimePickerDialog(
                context,
                (picker, hour, minute) -> cb.onTime(String.format("%02d:%02d", hour, minute)),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
        ).show();
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        String lower = s.toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    // ✅ System day key: "monday", "tuesday"... (matches your backend nodes)
    private static String getTodayKey() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);

        switch (dow) {
            case Calendar.MONDAY: return "monday";
            case Calendar.TUESDAY: return "tuesday";
            case Calendar.WEDNESDAY: return "wednesday";
            case Calendar.THURSDAY: return "thursday";
            case Calendar.FRIDAY: return "friday";
            case Calendar.SATURDAY: return "saturday";
            case Calendar.SUNDAY: return "sunday";
            default: return "monday";
        }
    }
}
