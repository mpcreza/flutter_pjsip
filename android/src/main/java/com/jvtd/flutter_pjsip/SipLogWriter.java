package com.jvtd.flutter_pjsip;

import android.util.Log;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

public class SipLogWriter extends LogWriter {
    @Override
    public void write(LogEntry entry) {
        int pjsipLogLevel = entry.getLevel();
        String logString = entry.getMsg();

        switch (pjsipLogLevel){
            case 1:
                Log.e("EXCEPTION", logString);
                break;
            case 2:
                Log.w("WARNING", logString);
                break;
            case 3:
                Log.i("INFO", logString);
                break;
            case 4:
                Log.d("DEBUG", logString);
                break;
            default:
                Log.v("VERBOSE", logString);
                break;
        }
    }
}
