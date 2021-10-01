package com.bardiademon.JavaServer.Server.HttpResponse;

import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;
import com.bardiademon.JavaServer.Server.Router;
import com.bardiademon.JavaServer.bardiademon.Default;
import com.bardiademon.JavaServer.bardiademon.Str;
import com.bardiademon.JavaServer.bardiademon.Time;
import com.google.gson.Gson;

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

public final class HttpResponse extends StatusCode
{
    private String text;
    private int statusCode = SC_OK;
    private String contentType = HttpRequest.CT_TEXT_PLAIN;
    private final Map <String, String> headers;
    private Charset charset = StandardCharsets.UTF_8;

    private InputStream stream;
    private ResponseFile responseFile;

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

    public HttpResponse (final int statusCode)
    {
        this ();
        this.statusCode = statusCode;
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
        catch (Router.HandlerException handlerException)
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

    public static HttpResponse createHtmlResponse (final String htmlFile)
    {
        return createHtmlResponse (htmlFile , SC_OK);
    }

    public static HttpResponse createJsonResponse (final Maps maps)
    {
        return createJsonResponse (maps , SC_OK);
    }

    public static HttpResponse createJsonResponse (final Maps maps , final int statusCode)
    {
        return createTextResponse (maps.toString () , statusCode , HttpRequest.CT_APP_JSON_OR_QL);
    }

    public static HttpResponse createHtmlResponse (final String htmlFile , final int statusCode)
    {
        final HttpResponse response = new HttpResponse ();

        response.setResponseType (HttpResponse.ResponseType.html);
        response.setStatusCode (statusCode);
        response.setCharset (StandardCharsets.UTF_8);
        response.setHtmlFile (htmlFile);
        response.setContentType (HttpRequest.CT_TEXT_HTML);

        return response;
    }

    public static HttpResponse createResponseFromClass (final Object obj)
    {
        return createResponseFromClass (obj , SC_OK);
    }

    public static HttpResponse createResponseFromClass (final Object obj , final int statusCode)
    {
        return createTextResponse (((new Gson ()).toJson (obj)) , statusCode , HttpRequest.CT_APP_JSON_OR_QL);
    }

    public static HttpResponse createTextResponse (final Object text)
    {
        return createTextResponse (text , SC_OK);
    }

    public static HttpResponse createTextResponse (final Object text , final int statusCode)
    {
        return createTextResponse (text , statusCode , HttpRequest.CT_TEXT_PLAIN);
    }

    public static HttpResponse createTextResponse (final Object text , final String contentType)
    {
        return createTextResponse (text , SC_OK , contentType);
    }

    public static HttpResponse createTextResponse (final Object text , final int statusCode , final String contentType)
    {
        return createTextResponse (((text == null) ? "null" : text.toString ()) , statusCode , contentType);
    }

    public static HttpResponse createTextResponse (final String text)
    {
        return createTextResponse (text , SC_OK);
    }

    public static HttpResponse createTextResponse (final String text , final int statusCode)
    {
        return createTextResponse (text , statusCode , HttpRequest.CT_TEXT_PLAIN);
    }

    public static HttpResponse createTextResponse (final String text , final String contentType)
    {
        return createTextResponse (text , SC_OK , contentType);
    }

    public static HttpResponse createTextResponse (final String text , final int statusCode , final String contentType)
    {
        final HttpResponse response = new HttpResponse ();

        response.setResponseType (HttpResponse.ResponseType.text);
        response.setStatusCode (statusCode);
        response.setCharset (StandardCharsets.UTF_8);
        response.setText (text);
        response.setContentType (contentType);

        return response;
    }

    public static HttpResponse createStreamResponse (final ResponseFile responseFile) throws IOException
    {
        return createStreamResponse (responseFile , SC_OK);
    }

    public static HttpResponse createStreamResponse (final ResponseFile responseFile , final int statusCode) throws IOException
    {
        return createStreamResponse (responseFile , statusCode , null);
    }

    public static HttpResponse createStreamResponse (final ResponseFile responseFile , final String contentType) throws IOException
    {
        return createStreamResponse (responseFile , SC_OK , contentType);
    }

    public static HttpResponse createStreamResponse (final ResponseFile responseFile , final int statusCode , final String contentType) throws IOException
    {
        if (responseFile.file.exists ())
        {
            final HttpResponse response = new HttpResponse (statusCode);
            response.setCharset (StandardCharsets.UTF_8);
            response.setResponseFile (responseFile);
            response.setContentType (contentType);
            response.setResponseType (ResponseType.file);
            return response;
        }

        throw new IOException ("File not exists!");
    }

    public static HttpResponse createStreamResponse (final InputStream stream , final String contentType)
    {
        return createStreamResponse (stream , SC_OK , contentType);
    }

    public static HttpResponse createStreamResponse (final InputStream stream , final int statusCode)
    {
        return createStreamResponse (stream , statusCode , null);
    }

    public static HttpResponse createStreamResponse (final InputStream stream , final int statusCode , final String contentType)
    {
        final HttpResponse response = new HttpResponse (statusCode);
        response.setCharset (StandardCharsets.UTF_8);
        response.setStream (stream);
        response.setContentType (contentType);
        response.setResponseType (ResponseType.stream);
        return response;
    }

    public static void notFoundPage (final OutputStream outputStream) throws Router.HandlerException
    {
        writeText (outputStream , "404 Page not found" , SC_NOT_FOUND);
    }

    public static void bardiademon (final OutputStream outputStream) throws Router.HandlerException
    {
        writeText (outputStream , "bardiademon" , SC_OK);
    }

    public static void writeFile (final OutputStream outputStream , final File file)
    {
        writeFile (outputStream , file , file.getName () , SC_OK , null);
    }

    public static void writeFile (final OutputStream outputStream , final File file , final String filename)
    {
        writeFile (outputStream , file , SC_OK);
    }

    public static void writeFile (final OutputStream outputStream , final File file , final int statusCode)
    {
        writeFile (outputStream , file , file.getName () , statusCode);
    }

    public static void writeFile (final OutputStream outputStream , final File file , final String filename , final int statusCode)
    {
        writeFile (outputStream , file , filename , statusCode , null);
    }

    public static void writeFile (final OutputStream outputStream , final File file , final int statusCode , final String contentType)
    {
        writeFile (outputStream , file , file.getName () , statusCode , contentType);
    }

    public static void writeFile (final OutputStream outputStream , final File file , final String filename , final String contentType)
    {
        writeFile (outputStream , file , filename , SC_OK , contentType);
    }

    public static void writeFile (final OutputStream outputStream , final File file , String filename , final int statusCode , String contentType)
    {
        if (Str.isEmpty (contentType))
        {
            try
            {
                contentType = Files.probeContentType (file.toPath ());
            }
            catch (IOException ignored)
            {
            }
        }

        if (Str.isEmpty (filename)) filename = file.getName ();

        final HttpResponse response = new HttpResponse ();
        response.setContentType (contentType);
        response.setStatusCode (statusCode);
        response.setCharset (StandardCharsets.UTF_8);
        response.setHeader ("filename" , filename);

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

    public static void writeText (final OutputStream outputStream , final String text , final int statusCode) throws Router.HandlerException
    {
        final HttpResponse response = createTextResponse (text , statusCode);
        write (outputStream , response , response.getText ());
    }

    public static void write (final OutputStream outputStream , final HttpResponse httpResponse , final String textHtml) throws Router.HandlerException
    {
        try
        {
            write (outputStream , (getHeader (httpResponse , textHtml.length ()) + new String (textHtml.getBytes (httpResponse.getCharset ()))).getBytes ());
        }
        catch (final Exception exception)
        {
            throw new Router.HandlerException (exception.getMessage ());
        }
    }

    public static void write (final OutputStream outputStream , final HttpResponse httpResponse , final InputStream stream) throws Router.HandlerException
    {
        try
        {
            outputStream.write (getHeader (httpResponse , stream.available ()).getBytes ());

            final byte[] buffer = new byte[1027 * 5];
            for (int len = 0; len != -1; len = stream.read (buffer)) outputStream.write (buffer , 0 , len);

            outputStream.flush ();
            outputStream.close ();
        }
        catch (final IOException exception)
        {
            throw new Router.HandlerException (exception.getMessage ());
        }
    }

    private static String getHeader (final HttpResponse httpResponse , final long len)
    {
        final StringBuilder response = new StringBuilder (String.format (
                "HTTP/1.1 %d\r\ndate:%s\r\nX-Powered-By:%s\r\nConnection:Upgrade\r\nUpgrade: websocket\r\nContent-Type:%s; charset:%s\r\nContent-Length:%d" ,
                httpResponse.getStatusCode () ,
                LocalDateTime.now ().format (Time.getHeaderDateFormat ()) ,
                Default.X_POWERED_BY ,
                httpResponse.getContentType () ,
                httpResponse.getCharset ().toString ().toLowerCase (Locale.ROOT) , len));

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

    public void setResponseFile (final ResponseFile responseFile)
    {
        this.responseFile = responseFile;
    }

    public ResponseFile getResponseFile ()
    {
        return responseFile;
    }

    public ResponseType getResponseType ()
    {
        return responseType;
    }

    public enum ResponseType
    {
        html, text, stream, file
    }
}
