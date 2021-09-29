package com.bardiademon.JavaServer.Server;

import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;
import com.bardiademon.JavaServer.bardiademon.Time;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HttpResponse
{
    public static final int SC_CONTINUE = 100;
    public static final int SC_SWITCHING_PROTOCOLS = 101;
    public static final int SC_OK = 200;
    public static final int SC_CREATED = 201;
    public static final int SC_ACCEPTED = 202;
    public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
    public static final int SC_NO_CONTENT = 204;
    public static final int SC_RESET_CONTENT = 205;
    public static final int SC_PARTIAL_CONTENT = 206;
    public static final int SC_MULTIPLE_CHOICES = 300;
    public static final int SC_MOVED_PERMANENTLY = 301;
    public static final int SC_MOVED_TEMPORARILY = 302;
    public static final int SC_FOUND = 302;
    public static final int SC_SEE_OTHER = 303;
    public static final int SC_NOT_MODIFIED = 304;
    public static final int SC_USE_PROXY = 305;
    public static final int SC_TEMPORARY_REDIRECT = 307;
    public static final int SC_BAD_REQUEST = 400;
    public static final int SC_UNAUTHORIZED = 401;
    public static final int SC_PAYMENT_REQUIRED = 402;
    public static final int SC_FORBIDDEN = 403;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_METHOD_NOT_ALLOWED = 405;
    public static final int SC_NOT_ACCEPTABLE = 406;
    public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
    public static final int SC_REQUEST_TIMEOUT = 408;
    public static final int SC_CONFLICT = 409;
    public static final int SC_GONE = 410;
    public static final int SC_LENGTH_REQUIRED = 411;
    public static final int SC_PRECONDITION_FAILED = 412;
    public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int SC_REQUEST_URI_TOO_LONG = 414;
    public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    public static final int SC_EXPECTATION_FAILED = 417;
    public static final int SC_INTERNAL_SERVER_ERROR = 500;
    public static final int SC_NOT_IMPLEMENTED = 501;
    public static final int SC_BAD_GATEWAY = 502;
    public static final int SC_SERVICE_UNAVAILABLE = 503;
    public static final int SC_GATEWAY_TIMEOUT = 504;
    public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;

    private String text;
    private int statusCode = 200;
    private String contentType = HttpRequest.CT_TEXT_PLAIN;
    private final Map <String, String> headers;
    private Charset charset = StandardCharsets.UTF_8;

    private InputStream stream;

    private final List <HttpRequest.Cookie> cookies;

    // Key
    private final List <String> removeCookies;

    private String htmlFile;

    private ResponseType responseType = ResponseType.text;

    public HttpResponse ()
    {
        headers = new HashMap <> ();
        cookies = new ArrayList <> ();
        removeCookies = new ArrayList <> ();
    }

    public static void error (final OutputStream stream , final Exception exception)
    {
        final HttpResponse response = new HttpResponse ();

        StackTraceElement[] stackTrace = exception.getStackTrace ();

        final StringBuilder text = new StringBuilder ("Internal server error\n");
        if (stackTrace.length > 0)
        {
            text.append ("Message: ").append (exception.getMessage ()).append ('\n');
            for (final StackTraceElement stackTraceElement : stackTrace)
            {
                text.append ("\n\n").append (String.format ("File: %s\nClass: %s\nMethod: %s\nLine: %d"
                        , stackTraceElement.getFileName () , stackTraceElement.getClassName () , stackTraceElement.getMethodName () ,
                        stackTraceElement.getLineNumber ()));
            }

        }
        else text.append (exception.getMessage ());

        response.setText (text.toString ());
        response.setCharset (StandardCharsets.UTF_8);
        response.setContentType ("text/plain");
        response.setStatusCode (500);

        try
        {
            write (stream , response , response.getText ());
        }
        catch (Handler.HandlerException handlerException)
        {
            try
            {
                stream.write (("Error <" + handlerException.getMessage () + ">").getBytes (StandardCharsets.UTF_8));
                stream.flush ();
                stream.close ();
            }
            catch (IOException e)
            {
                e.printStackTrace ();
            }

        }
    }

    public static void notFoundPage (final OutputStream outputStream) throws Handler.HandlerException
    {
        writeText (outputStream , "404 Page not found" , 404);
    }

    public static void bardiademon (final OutputStream outputStream) throws Handler.HandlerException
    {
        writeText (outputStream , "bardiademon" , 200);
    }

    public static void writeFile (final OutputStream outputStream , final File file) throws Handler.HandlerException
    {
        String contentType = null;
        try
        {
            contentType = Files.probeContentType (file.toPath ());
        }
        catch (IOException ignored)
        {
        }

        final HttpResponse response = new HttpResponse ();
        response.setContentType (contentType);
        response.setStatusCode (200);
        response.setCharset (StandardCharsets.UTF_8);
        response.setHeader ("filename" , file.getName ());

        try (final InputStream stream = new FileInputStream (file))
        {
            outputStream.write (getHeader (response , file.length ()).getBytes ());

            byte[] bytes = new byte[1024 * 5];
            for (int len = 0; len != -1; len = stream.read (bytes)) outputStream.write (bytes , 0 , len);

            outputStream.flush ();
            outputStream.close ();
        }
        catch (IOException ignored)
        {
        }
    }

    public static void writeText (final OutputStream outputStream , final String text , final int statusCode) throws Handler.HandlerException
    {
        final HttpResponse response = new HttpResponse ();
        response.setResponseType (ResponseType.text);
        response.setStatusCode (statusCode);
        response.setCharset (StandardCharsets.UTF_8);
        response.setText (text);
        response.setContentType (HttpRequest.CT_TEXT_PLAIN);

        write (outputStream , response , response.getText ());
    }

    public void setCookies (HttpRequest.Cookie cookie)
    {
        this.cookies.add (cookie);
    }

    public void setRemoveCookies (String cookieKey)
    {
        this.removeCookies.add (cookieKey);
    }

    public List <HttpRequest.Cookie> getCookies ()
    {
        return cookies;
    }

    public List <String> getRemoveCookies ()
    {
        return removeCookies;
    }

    public void setHeader (final String key , final String path)
    {
        headers.put (key , path);
    }

    public String getText ()
    {
        return text;
    }

    public void setText (String text)
    {
        this.text = text;
    }

    public int getStatusCode ()
    {
        return statusCode;
    }

    public void setStatusCode (int statusCode)
    {
        this.statusCode = statusCode;
    }

    public String getContentType ()
    {
        return contentType;
    }

    public void setContentType (String contentType)
    {
        this.contentType = contentType;
    }

    public Charset getCharset ()
    {
        return charset;
    }

    public void setCharset (Charset charset)
    {
        this.charset = charset;
    }

    public String getHtmlFile ()
    {
        return htmlFile;
    }

    public void setHtmlFile (String htmlFile)
    {
        this.htmlFile = htmlFile;
    }

    public Map <String, String> getHeaders ()
    {
        return headers;
    }

    public void setResponseType (ResponseType responseType)
    {
        this.responseType = responseType;
    }

    public InputStream getStream ()
    {
        return stream;
    }

    public void setStream (InputStream stream)
    {
        this.stream = stream;
    }


    public ResponseType getResponseType ()
    {
        return responseType;
    }

    public static void write (final OutputStream outputStream , final HttpResponse httpResponse , final String textHtml) throws Handler.HandlerException
    {
        try
        {
            write (outputStream , (getHeader (httpResponse , textHtml.length ()) + new String (textHtml.getBytes (httpResponse.getCharset ()))).getBytes ());
        }
        catch (final Exception exception)
        {
            throw new Handler.HandlerException (exception.getMessage ());
        }
    }

    public static void write (final OutputStream outputStream , final HttpResponse httpResponse , final InputStream stream) throws Handler.HandlerException
    {
        try
        {
            final ByteArrayOutputStream out = new ByteArrayOutputStream (stream.available ());
            final byte[] buffer = new byte[1027 * 5];
            for (int len = 0; len != -1; len = stream.read (buffer)) out.write (buffer , 0 , len);

            outputStream.write (getHeader (httpResponse , out.size ()).getBytes (StandardCharsets.UTF_8));
            outputStream.write (out.toByteArray ());
            outputStream.flush ();
            outputStream.close ();
        }
        catch (final IOException exception)
        {
            throw new Handler.HandlerException (exception.getMessage ());
        }
    }

    private static String getHeader (final HttpResponse httpResponse , final long len)
    {
        final StringBuilder response = new StringBuilder (String.format (
                "HTTP/1.1 %d\r\ndate:%s\r\nX-Powered-By:bardiademon\r\nConnection:Upgrade\r\nUpgrade: websocket\r\nContent-Type:%s; charset:%s\r\nContent-Length:%d" ,
                httpResponse.getStatusCode () , LocalDateTime.now ().format (Time.getHeaderDateFormat ()) , httpResponse.getContentType () , httpResponse.getCharset ().toString ().toLowerCase (Locale.ROOT)
                , len));

        if (httpResponse.getHeaders ().size () > 0)
        {
            final Set <Map.Entry <String, String>> headers = httpResponse.getHeaders ().entrySet ();
            for (final Map.Entry <String, String> entries : headers)
                response.append (String.format ("\r\n%s:%s" , entries.getKey () , entries.getValue ()));
        }

        if (httpResponse.getCookies ().size () > 0)
        {
            final List <HttpRequest.Cookie> cookies = httpResponse.getCookies ();
            for (final HttpRequest.Cookie cookie : cookies)
            {
                response.append (String.format ("\r\nSet-Cookie:%s=%s; expires=%s; path=%s; domain=%s;" ,
                        cookie.key , cookie.value , cookie.expires , cookie.path , cookie.domain));
            }
        }

        if (httpResponse.getRemoveCookies ().size () > 0)
        {
            final List <String> cookies = httpResponse.getRemoveCookies ();
            for (final String cookieKey : cookies)
            {
                response.append (String.format ("\r\nSet-Cookie:%s=deleted; expires=%s; path=/ ;" ,
                        cookieKey , LocalDateTime.now ().minusYears (100).format (Time.getHeaderDateFormat ())));
            }
        }

        response.append ("\r\n\r\n");

        return response.toString ();
    }

    private static void write (final OutputStream outputStream , byte[] bytes) throws IOException
    {
        outputStream.write (bytes);
        outputStream.flush ();
        outputStream.close ();
    }

    public enum ResponseType
    {
        html, text, stream
    }
}
