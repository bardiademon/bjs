package com.bardiademon.JavaServer.bardiademon;

import java.io.File;

public final class Path
{

    private Path ()
    {

    }

    public static final String ROOT = System.getProperty ("user.dir");
    public static final String TMP = Get (ROOT , "tmp");

    public static String staticPath;

    public static final String DEFAULT_STATIC_PATH = Get (ROOT , "static");

    // bjs => bardiademon Java Server
    public static final String CONFIG_BJS = Get (ROOT , "config.bjs");

    public static final String TEMPLATE_NAME = "template";
    public static String publicName = "public";
    public static String publicPath;

    public static String TEMPLATE;
    public static String PUBLIC;

    public static final String JWT_SECRET = Get (ROOT , "jwt-secret.bjs");

    public static String favicon;

    public static void setDefaultStaticPath ()
    {
        staticPath = DEFAULT_STATIC_PATH;
    }

    public static void setTemplatePath ()
    {
        TEMPLATE = Get (staticPath , TEMPLATE_NAME);
    }

    public static void setPublicPath ()
    {
        publicPath = Get (staticPath , publicName);
    }

    public static String Get (Object... paths)
    {
        final StringBuilder finalPath = new StringBuilder ();
        for (int i = 0, len = paths.length; i < len; i++)
        {
            finalPath.append (paths[i]);
            if ((i + 1) < len) finalPath.append (File.separator);
        }
        return finalPath.toString ();
    }

    public static String GetWithFilename (String filename , final String type , Object... paths)
    {
        String get = Get (paths);
        if (!Str.isEmpty (filename))
        {
            get += File.separator + filename;
            if (!Str.isEmpty (type)) get += "." + type;
        }
        return get;
    }
}
