package com.bardiademon.JavaServer.Server;

import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;
import com.bardiademon.JavaServer.Server.HttpRequest.Method;
import com.bardiademon.JavaServer.Server.HttpRequest.StreamReader;
import com.bardiademon.JavaServer.bardiademon.Default;
import com.bardiademon.JavaServer.bardiademon.Path;
import com.bardiademon.JavaServer.bardiademon.Str;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class Server
{

    private ServerSocket server;
    private OnError onError;

    private OnFile onFile;

    private final List <Router> routes = new ArrayList <> ();

    public void run (final OnError onError) throws IOException
    {
        final Config config = new Config ();
        if (config.isOk ()) run (config.getPort () , config.getHost () , onError);
        else throw new IOException ("config.bjs error");
    }

    public void run (int port , final OnError onError) throws IOException
    {
        run (port , "localhost" , onError);
    }

    public void run (int port , final String host , final OnError onError) throws IOException
    {
        System.out.printf ("\n%s%s\n\n" , Default._V , Default.POWERED_BY);

        if (Str.isEmpty (host)) server = new ServerSocket (port);
        else server = new ServerSocket (port , 0 , InetAddress.getByName (host));

        server.setReceiveBufferSize (Integer.MAX_VALUE);
        server.setSoTimeout (Integer.MAX_VALUE);
        System.out.println ("Server run in port " + port);
        this.onError = onError;
    }

    public void setOnFile (final OnFile onFile)
    {
        this.onFile = onFile;
    }

    public void onGet (final Controller controller , final String... route)
    {
        on (Method.get , controller , route);
    }

    public void onPost (final Controller controller , final String... route)
    {
        on (Method.post , controller , route);
    }

    public void on (final Method method , final Controller controller , final String... route)
    {
        routes.add (new Router (controller , route , method));
    }

    public void listen ()
    {
        new Thread (() ->
        {
            while (true)
            {
                try
                {
                    final Socket accept = server.accept ();
                    new Client (accept);
                }
                catch (final IOException e)
                {
                    onError.onListenError (e);
                }
            }
        }).start ();
    }

    private final class Client
    {
        private Client (final Socket socket)
        {
            new Thread (() ->
            {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try
                {
                    inputStream = socket.getInputStream ();
                }
                catch (final IOException e)
                {
                    onError.onGetInputStreamError (e);
                }
                try
                {
                    outputStream = socket.getOutputStream ();
                }
                catch (final IOException e)
                {
                    onError.onGetOutputStreamError (e);
                }

                if (inputStream != null && outputStream != null)
                {
                    try
                    {
                        final HttpRequest request = getHttpRequest (inputStream);
                        if (request.getPath () != null && routes.size () > 0)
                        {
                            request.setInputStream (inputStream);
                            request.setOutputStream (outputStream);
                            Path.setPublicPath ();

                            final String favicon = favicon (request.getPath ());
                            if (favicon != null) request.setPath (favicon);

                            final String pathFile = getPathFile (request.getPath ());
                            File file;

                            if (Str.isEmpty (pathFile) || !(file = new File (Path.Get (Path.publicPath , pathFile))).exists ())
                            {
                                for (final Router route : routes)
                                {
                                    for (final String path : route.path)
                                    {
                                        final Map <String, String> pathParam = toPath (path , request.getPath ());
                                        if ((pathParam != null || path.equals (request.getPath ())) && route.method.equals (request.getMethod ()))
                                        {
                                            try
                                            {
                                                if (pathParam != null) request.setUrlPathParam (pathParam);
                                                route.doing (request , route.controller.run (request));
                                            }
                                            catch (final Router.HandlerException e)
                                            {
                                                HttpResponse.error (outputStream , e);
                                                onError.onGetHandlerException (e);
                                            }
                                            finally
                                            {
                                                socket.close ();
                                            }
                                            return;
                                        }
                                    }
                                }

                                HttpResponse.notFoundPage (outputStream);

                                socket.close ();
                            }
                            else
                            {
                                if (onFile != null) onFile.file (request , file);
                                else HttpResponse.writeFile (outputStream , file);
                            }

                        }
                        else HttpResponse.bardiademon (outputStream);
                    }
                    catch (final Exception exception)
                    {
                        HttpResponse.error (outputStream , exception);
                    }
                }
            }).start ();
        }
    }

    /*
     * List <Object[]> => new Object[] { PP_I_KEY , PP_I_INDEX }
     * PP_I: PP => PathParam , I => Index
     */
    private static final int PP_I_KEY = 0, PP_I_INDEX = 1;

    // Pathi ke az server taiin shode /PATH/{KEY}/PATH/{KEY}/...
    private List <Object[]> pathParam (final String serverPath) throws Router.HandlerException
    {
        final String[] split = serverPath.split ("/");
        final List <Object[]> keyIndex = new ArrayList <> ();

        for (int i = 0; i < split.length; i++)
        {
            for (int j = i + 1; j < split.length; j++)
            {
                if (i != j && split[i].equals (split[j]))
                    throw new Router.HandlerException ("duplicate Path[" + split[i] + "]");
            }
        }

        for (int i = 0, splitLength = split.length; i < splitLength; i++)
        {
            String path = split[i];
            path = path.trim ();
            if (path.startsWith ("{") && path.endsWith ("}"))
                keyIndex.add (new Object[] { path.substring (1 , path.length () - 1) , i });
        }
        return keyIndex;
    }

    private Map <String, String> toPath (final String serverPath , final String userPath) throws Router.HandlerException
    {
        final List <Object[]> keyIndex = pathParam (serverPath);
        if (keyIndex.size () > 0)
        {
            final String[] serverPathSplit = serverPath.split ("/");
            final String[] userPathSplit = userPath.split ("/");

            if (serverPathSplit.length == userPathSplit.length)
            {
                final Map <String, String> param = new HashMap <> ();

                boolean foundKey;
                for (int i = 0; i < userPathSplit.length; i++)
                {
                    foundKey = false;
                    for (int j = 0, keyIndexSize = keyIndex.size (); j < keyIndexSize; j++)
                    {
                        final Object[] index = keyIndex.get (j);
                        if (((int) index[PP_I_INDEX]) == i)
                        {
                            param.put ((String) index[PP_I_KEY] , userPathSplit[i]);
                            try
                            {
                                keyIndex.remove (j);
                            }
                            catch (Exception ignored)
                            {
                            }
                            foundKey = true;
                            break;
                        }
                    }
                    // agar hata yeki az path haye server ba path haye user barabar nabashe router eshtebah ast
                    if (!foundKey && !serverPathSplit[i].equals (userPathSplit[i])) return null;
                }

                if (param.size () > 0) return param;
            }
        }
        return null;
    }

    public HttpRequest getHttpRequest (final InputStream inputStream) throws Exception
    {
        final HttpRequest httpRequest = new HttpRequest ();
        final GetInfo getInfo = new GetInfo ();
        final AtomicInteger numberOfLine = new AtomicInteger (0);
        final AtomicReference <Exception> exception = new AtomicReference <> ();
        final boolean[] mainHeaderOk = { false };
        final AtomicBoolean getOneFile = new AtomicBoolean (false);

        final AtomicReference <StringBuilder> infoOneFile = new AtomicReference <> (new StringBuilder ());
        AtomicReference <ByteArrayOutputStream> outputStream = new AtomicReference <> (new ByteArrayOutputStream ());

        // seta khat mire ta etelaat file bashe
        final AtomicInteger counterGetFile = new AtomicInteger (0);

        final StreamReader reader = new StreamReader ();

        new Thread (() -> reader.read (inputStream , (line , bytes) ->
        {
            if (line.equals ("|bardiademon.NULL|"))
            {
                synchronized (httpRequest)
                {
                    httpRequest.notify ();
                    httpRequest.notifyAll ();
                }
            }
            else
            {
                final int i = numberOfLine.incrementAndGet ();
                if (i == 1)
                {
                    final String[] mr = line.split (" ");
                    final String mtd = mr[0];
                    Method method;
                    try
                    {
                        final String methodStr = mtd.trim ().toLowerCase (Locale.ROOT);
                        method = Method.to (methodStr);
                        httpRequest.setMethod (method);

                        String path = mr[1].trim ();

                        if (path.contains ("?"))
                        {
                            String[] split = path.split ("\\?(?!\\?)");

                            if (split.length > 0)
                            {

                                final String[] keysValues = split[1].split ("&");

                                final Map <String, String> pathParam = new HashMap <> ();

                                String[] splitKeyValue;
                                for (final String keyValue : keysValues)
                                {
                                    splitKeyValue = keyValue.split ("=");
                                    if (splitKeyValue.length == 2) pathParam.put (splitKeyValue[0] , splitKeyValue[1]);
                                }

                                httpRequest.setPathParameters (pathParam);

                                path = split[0];
                            }

                        }
                        httpRequest.setPath (path);
                    }
                    catch (Exception e)
                    {
                        exception.set (new Exception ("Method not allow"));
                        synchronized (httpRequest)
                        {
                            httpRequest.notify ();
                            httpRequest.notifyAll ();
                        }

                        return false;
                    }
                }
                else if (i == 2)
                {
                    final String[] hostPort = getInfo.splitHost (getInfo.getHost (line));
                    String host;
                    int port;
                    if (hostPort.length > 0)
                    {
                        host = hostPort[0];
                        try
                        {
                            port = Integer.parseInt (hostPort[1]);
                        }
                        catch (final Exception e)
                        {
                            exception.set (e);
                            synchronized (httpRequest)
                            {
                                httpRequest.notify ();
                                httpRequest.notifyAll ();
                            }
                            return false;
                        }
                        httpRequest.setHost (host);
                        httpRequest.setPort (port);
                    }
                }
                else if (line.isEmpty () && !mainHeaderOk[0])
                {
                    if (httpRequest.getContentType () == null)
                    {
                        synchronized (httpRequest)
                        {
                            httpRequest.notify ();
                            httpRequest.notifyAll ();
                        }
                        return false;
                    }
                    else mainHeaderOk[0] = true;
                }
                else if (mainHeaderOk[0])
                {
                    switch (httpRequest.getContentType ())
                    {
                        case HttpRequest.CT_APP_JS:
                        case HttpRequest.CT_APP_XML:
                        case HttpRequest.CT_TEXT_PLAIN:
                        case HttpRequest.CT_APP_JSON_OR_QL:
                        case HttpRequest.CT_TEXT_HTML:
                        {
                            httpRequest.setRawStr (new String (bytes , StandardCharsets.UTF_8));
                            reader.setGetFullLine (true);

                            synchronized (httpRequest)
                            {
                                httpRequest.notify ();
                                httpRequest.notifyAll ();
                            }

                            return true;
                        }
                        case HttpRequest.CT_APP_X_WWW_FROM_URLENCODED:
                        {
                            final Map <String, String> params = new HashMap <> ();
                            final String[] split = line.trim ().split ("&");
                            if (split.length > 0)
                            {
                                String[] paramSplit;
                                for (final String param : split)
                                {
                                    paramSplit = param.split ("=");
                                    if (paramSplit.length == 2) params.put (paramSplit[0] , paramSplit[1]);
                                }
                            }
                            if (params.size () > 0) httpRequest.setParameters (params);

                            synchronized (httpRequest)
                            {
                                httpRequest.notify ();
                                httpRequest.notifyAll ();
                            }
                            return false;
                        }
                        case HttpRequest.CT_MULTIPART_FROM_DATA:
                        {
                            if (httpRequest.getBoundary () != null)
                            {
                                if (("--" + httpRequest.getBoundary ()).equals (line.toLowerCase (Locale.ROOT)) && !getOneFile.get ())
                                {
                                    getOneFile.set (true);
                                    return true;
                                }
                                else
                                {
                                    if (("--" + httpRequest.getBoundary ()).equals (line.toLowerCase (Locale.ROOT)) || line.toLowerCase (Locale.ROOT).equals ("--" + httpRequest.getBoundary () + "--") || (outputStream.get ().size () > 0 && line.contains ("Content-Disposition: form-data;")))
                                    {
                                        getOneFile.set (false);

                                        try
                                        {
                                            httpRequest.setFileRequests (HttpRequest.FileRequest.getInstance (outputStream.get ().toByteArray () , infoOneFile.toString ()));
                                        }
                                        catch (Exception e)
                                        {
                                            exception.set (e);
                                            synchronized (httpRequest)
                                            {
                                                httpRequest.notify ();
                                                httpRequest.notifyAll ();
                                            }
                                            return false;
                                        }

                                        infoOneFile.set (new StringBuilder ());
                                        counterGetFile.set (0);
                                        outputStream.set (new ByteArrayOutputStream ());

                                        if (line.toLowerCase (Locale.ROOT).equals ("--" + httpRequest.getBoundary () + "--"))
                                        {
                                            synchronized (httpRequest)
                                            {
                                                httpRequest.notify ();
                                                httpRequest.notifyAll ();
                                            }
                                            return false;
                                        }
                                        else
                                        {
                                            if (line.contains ("Content-Disposition: form-data;"))
                                            {
                                                counterGetFile.incrementAndGet ();
                                                infoOneFile.get ().append (line).append ("\n");
                                            }
                                            return true;
                                        }
                                    }
                                    else
                                    {
                                        if (counterGetFile.get () >= 3)
                                            outputStream.get ().write (bytes , 0 , bytes.length);
                                        else
                                        {
                                            counterGetFile.incrementAndGet ();
                                            infoOneFile.get ().append (line).append ("\n");
                                        }
                                    }
                                }
                            }
                            else
                            {
                                exception.set (new Exception ("Boundary is null"));
                                synchronized (httpRequest)
                                {
                                    httpRequest.notify ();
                                    httpRequest.notifyAll ();
                                }
                                return false;
                            }
                            break;
                        }
                        default:
                        {
                            exception.set (new Exception ("Content-type is null"));
                            synchronized (httpRequest)
                            {
                                httpRequest.notify ();
                                httpRequest.notifyAll ();
                            }
                            return false;
                        }
                    }
                }

                line = line.toLowerCase (Locale.ROOT).trim ();
                if (line.contains (GetInfo.K_USER_AGENT))
                    httpRequest.setUserAgent (getInfo.getUserAgent (line));
                else if (line.contains (GetInfo.K_ACCEPT_ENCODING))
                    httpRequest.setAcceptEncoding (getInfo.getAcceptEncoding (line));
                else if (line.contains (GetInfo.K_CONTENT_TYPE))
                {
                    final String[] contentTypeAndBoundary = getInfo.getContentTypeAndBoundary (line);
                    httpRequest.setContentType (contentTypeAndBoundary[0]);
                    httpRequest.setBoundary (contentTypeAndBoundary[1]);
                }
                else if (line.contains (GetInfo.K_ACCEPT_LANGUAGE))
                    httpRequest.setAcceptLanguage (getInfo.getAcceptLanguage (line));
                else if (line.contains (GetInfo.K_COOKIE))
                    httpRequest.setCookies (getInfo.getCookies (line));
                else if (line.contains (GetInfo.K_ACCEPT))
                    httpRequest.setAccepts (getInfo.getAccepts (line));

            }
            return true;
        })).start ();

        synchronized (httpRequest)
        {
            httpRequest.wait ();
        }

        return httpRequest;
    }

    public String favicon (final String userPath)
    {
        return (userPath.equals ("/favicon.ico") ? String.format ("/%s/favicon.ico" , Path.publicName) : null);
    }

    // agar file bashad
    private String getPathFile (final String userPath)
    {
        final String[] split = userPath.split ("/");
        if (split.length > 2 && split[1].equals (Path.publicName))
            return userPath.substring (String.format ("/%s" , Path.publicName).length ());
        return null;
    }

    private final static class GetInfo
    {
        private static final String K_HOST = "host", K_USER_AGENT = "user-agent", K_ACCEPT_ENCODING = "accept-encoding",
                K_CONTENT_TYPE = "content-type", K_ACCEPT_LANGUAGE = "accept-language", K_COOKIE = "cookie", K_ACCEPT = "accept",
                K_CONTENT_LENGTH = "content-length";

        public GetInfo ()
        {
        }

        private String getHost (final String line)
        {
            return getValue (line , K_HOST);
        }

        private String getUserAgent (final String line)
        {
            return getValue (line , K_USER_AGENT);
        }

        private String[] splitHost (final String host)
        {
            if (host != null && !host.isEmpty ())
            {
                if (host.contains (":"))
                {
                    return host.split (":");
                }
                else return new String[] { host , "80" };
            }
            else return new String[] { };
        }

        private String getValue (final String line , final String key)
        {
            if (line.contains (key))
            {
                final String[] split = line.split (":");
                if (split.length == 2) return split[1].trim ();

//                final StringBuilder value = new StringBuilder ();
//                final char[] chars = input.toCharArray ();
//                for (int i = (input.indexOf (key) + key.length ()); i < input.length (); i++)
//                {
//                    if (chars[i] == '\n') break;
//                    value.append (chars[i]);
//                }
//                return value.toString ().trim ();
            }

            return null;
        }

        private String getAcceptEncoding (final String line)
        {
            return getValue (line , K_ACCEPT_ENCODING);
        }

        public String[] getContentTypeAndBoundary (final String line)
        {
            try
            {
                final String value = getValue (line , K_CONTENT_TYPE);
                if (value != null && !value.isEmpty ())
                {
                    final String[] split = value.split (";");

                    if (split.length > 1)
                    {
                        String boundary = "";
                        try
                        {
                            boundary = split[1].split ("=")[1].trim ();
                        }
                        catch (Exception ignored)
                        {
                        }
                        return new String[] { split[0].trim () , boundary };
                    }
                    else return new String[] { split[0].trim () , "" };
                }
            }
            catch (Exception ignored)
            {
            }

            return new String[] { "" , "" };
        }

        private String getAcceptLanguage (final String line)
        {
            return getValue (line , K_ACCEPT_LANGUAGE);
        }

        private Map <String, String> getCookies (final String line)
        {
            final String cookiesStr = getValue (line , K_COOKIE);
            if (cookiesStr != null && !cookiesStr.isEmpty ())
            {
                final String[] splitCookies = cookiesStr.split (";");
                if (splitCookies.length > 0)
                {
                    final Map <String, String> cookies = new HashMap <> ();
                    for (final String splitCookie : splitCookies)
                    {
                        try
                        {
                            final String[] cookie = splitCookie.trim ().split ("=");
                            cookies.put (cookie[0] , cookie[1]);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    return cookies;
                }
            }
            return null;
        }

        private List <String> getAccepts (final String line)
        {
            final String accept = getValue (line , K_ACCEPT);
            if (accept != null)
            {
                final String[] accepts = accept.split (",");
                return Arrays.asList (accepts);
            }
            return null;
        }
    }

    public interface OnFile
    {
        void file (final HttpRequest request , final File file);
    }

    public interface OnError
    {
        void onListenError (final IOException exception);

        void onGetInputStreamError (final IOException exception);

        void onGetOutputStreamError (final IOException exception);

        void onGetHandlerException (final Router.HandlerException exception);
    }
}
