package com.schoolnav.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeFormatUtils {
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final SimpleDateFormat sdfNew =
            new SimpleDateFormat("MM.dd HH:mm");

    private static final SimpleDateFormat sdfJudge =
            new SimpleDateFormat("MM.dd");

    public static String formatDate(String dateString) {
        try {
            Date date = sdf.parse(dateString);
            return sdfNew.format(date);
        } catch (ParseException e) {
            Log.e("TimeFormatUtity", "e");
        }
        return dateString;
    }

    public static int isToday(String dateString) {
        try {
            Date date1 = sdf.parse(dateString);
            Date date2 = new Date();
            if(sdfJudge.format(date1).equals(sdfJudge.format(date2))){
                return 1;
            }
        } catch (ParseException e) {
            Log.e("TimeFormatUtity", "e");
        }
        return 0;
    }
}
