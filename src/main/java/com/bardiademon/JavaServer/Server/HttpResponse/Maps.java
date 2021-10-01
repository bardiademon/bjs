package com.bardiademon.JavaServer.Server.HttpResponse;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public final class Maps
{
    private final Map <String, Object> map = new HashMap <> ();

    private Maps ()
    {
    }

    // KeyValues > index * 2 , key , value
    // index 1 key va index 2 value , index 3 key index 4 value ,...
    public static Maps getMaps (final Object... keyValues) throws Exception
    {
        final Maps maps = new Maps ();
        if (keyValues.length % 2 == 0)
        {
            for (int i = 0; i < keyValues.length; i++)
            {
                if (keyValues[i + 1] != null)
                    maps.map.put (keyValues[i].toString () , keyValues[++i]);
            }
        }
        else throw new Exception ("len % 2 == 0");

        return maps;
    }

    public static String getJsonString (final Object... keyValues) throws Exception
    {
        return getMaps (keyValues).toString ();
    }

    public static Map <String, Object> getMap (final Object... keyValues) throws Exception
    {
        return getMaps (keyValues).map;
    }

    @Override
    public String toString ()
    {
        return (new Gson ()).toJson (map);
    }
}
