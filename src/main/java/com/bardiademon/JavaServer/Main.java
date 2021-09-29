package com.bardiademon.JavaServer;

import com.bardiademon.JavaServer.Server.Handler;
import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;
import com.bardiademon.JavaServer.Server.HttpRequest.Method;
import com.bardiademon.JavaServer.Server.HttpResponse;
import com.bardiademon.JavaServer.Server.Server;
import com.bardiademon.JavaServer.bardiademon.Path;

import java.io.File;
import java.io.IOException;

public final class Main
{
    public static void main (final String[] args) throws IOException
    {
        Server server = new Server ();

        Path.setDefaultStaticPath ();

        server.run (2000 , new Server.OnError ()
        {
            @Override
            public void onListenError (IOException exception)
            {
                System.out.println (exception.getMessage ());
            }

            @Override
            public void onGetInputStreamError (IOException exception)
            {
                System.out.println (exception.getMessage ());
            }

            @Override
            public void onGetOutputStreamError (IOException exception)
            {
                System.out.println (exception.getMessage ());
            }

            @Override
            public void onGetHandlerException (Handler.HandlerException exception)
            {
                System.out.println (exception.getMessage ());
            }
        });

        server.on (Method.get , request ->
        {
            final HttpResponse response = new HttpResponse ();
            response.setText ("bardiademon");
            return response;
        } , "/home" , "/");

        server.listen ();
    }
}
