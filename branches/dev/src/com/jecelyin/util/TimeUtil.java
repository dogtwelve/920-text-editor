package com.jecelyin.util;

import java.text.DateFormat;
import java.util.Date;

public class TimeUtil
{
    public static String getDate()
    {
        return format(new Date());
    }
    
    public static String getDate(long ts)
    {
        return format(new Date(ts));
    }
    
    private static String format(Date d)
    {
        return DateFormat.getDateTimeInstance().format(d);
    }
    
}
