package com.jvtd.flutter_pjsip;

import android.util.Log;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

import static android.content.ContentValues.TAG;

public class SipLogWriter extends LogWriter {
    @Override
    public void write(LogEntry entry) {
        Log.d(TAG, getClass().getSimpleName() + " " + entry.getMsg());
    }
}
