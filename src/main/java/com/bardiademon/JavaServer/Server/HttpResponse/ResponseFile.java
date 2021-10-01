package com.bardiademon.JavaServer.Server.HttpResponse;

import java.io.File;

public final class ResponseFile
{
    public final File file;
    public final String filename;

    private ResponseFile (final File file , final String filename)
    {
        this.file = file;
        this.filename = filename;
    }

    public static ResponseFile getResponseFile (final File file)
    {
        return (getResponseFile (file , file.getName ()));
    }

    public static ResponseFile getResponseFile (final File file , final String filename)
    {
        if (file == null) return null;

        return new ResponseFile (file , filename);
    }
}
