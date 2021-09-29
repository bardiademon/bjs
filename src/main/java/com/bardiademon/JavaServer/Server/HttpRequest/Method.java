package com.bardiademon.JavaServer.Server.HttpRequest;

public enum Method
{
    get, post, put, patch, delete, create, copy, head, options, link, unlink, purge, lock, unlock, view, propfind;

    public static Method to (final String name)
    {
        try
        {
            return valueOf (name);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
