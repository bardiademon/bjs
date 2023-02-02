package com.bardiademon.JavaServer.bardiademon;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Time
{
    public static DateTimeFormatter getHeaderDateFormat()
    {
        return DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z" , Locale.ENGLISH).withZone(ZoneId.of("GMT"));
    }
}
