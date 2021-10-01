package com.bardiademon.JavaServer.Server.HttpRequest;

import com.bardiademon.JavaServer.bardiademon.Path;
import com.bardiademon.JavaServer.bardiademon.Time;
import com.google.gson.Gson;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest
{
    // QL => QraphQl
    public static final String CT_APP_JSON_OR_QL = "application/json";
    public static final String CT_APP_JS = "application/javascript";
    public static final String CT_TEXT_HTML = "text/html";
    public static final String CT_TEXT_PLAIN = "text/plain";
    public static final String CT_APP_XML = "application/xml";
    public static final String CT_MULTIPART_FROM_DATA = "multipart/form-data";
    public static final String CT_APP_X_WWW_FROM_URLENCODED = "application/x-www-form-urlencoded";

    private InputStream inputStream;
    private OutputStream outputStream;

    private String host;
    private Method method;
    private int port;
    private String path;
    private String userAgent;
    private Map <String, String> headers;
    private Map <String, IncomingFiles> incomingFiles;
    private Map <String, String> parameters;
    private Map <String, String> pathParameters;
    private Map <String, String> urlPathParam;
    private Map <String, String> cookies;
    private String contentType;
    private String contentLength;
    private String boundary;
    private List <String> accepts;
    private String acceptEncoding;
    private String acceptLanguage;
    private String rawStr;

    public String getHost ()
    {
        return host;
    }

    public void setHost (String host)
    {
        this.host = host;
    }

    public Method getMethod ()
    {
        return method;
    }

    public void setMethod (Method method)
    {
        this.method = method;
    }

    public int getPort ()
    {
        return port;
    }

    public void setPort (int port)
    {
        this.port = port;
    }

    public void setContentLength (String contentLength)
    {
        this.contentLength = contentLength;
    }

    public String getContentLength ()
    {
        return contentLength;
    }

    public String getPath ()
    {
        return path;
    }

    public void setPath (String path)
    {
        this.path = path;
    }

    public String getUserAgent ()
    {
        return userAgent;
    }

    public void setUserAgent (String userAgent)
    {
        this.userAgent = userAgent;
    }

    public Map <String, String> getHeaders ()
    {
        return headers;
    }

    public void setHeaders (Map <String, String> headers)
    {
        this.headers = headers;
    }

    public Map <String, IncomingFiles> getIncomingFiles ()
    {
        return incomingFiles;
    }

    public void setIncomingFiles (final IncomingFiles incomingFiles)
    {
        if (this.incomingFiles == null) this.incomingFiles = new HashMap <> ();
        this.incomingFiles.put (incomingFiles.name , incomingFiles);
    }

    public Map <String, String> getParameters ()
    {
        return parameters;
    }

    public void setParameters (Map <String, String> parameters)
    {
        this.parameters = parameters;
    }

    public Map <String, String> getPathParameters ()
    {
        return pathParameters;
    }

    public void setPathParameters (Map <String, String> pathParameters)
    {
        this.pathParameters = pathParameters;
    }

    public Map <String, String> getUrlPathParam ()
    {
        return urlPathParam;
    }

    public void setUrlPathParam (Map <String, String> urlPathParam)
    {
        this.urlPathParam = urlPathParam;
    }

    public Map <String, String> getCookies ()
    {
        return cookies;
    }

    public void setCookies (Map <String, String> cookies)
    {
        this.cookies = cookies;
    }

    public String getContentType ()
    {
        return contentType;
    }

    public void setContentType (String contentType)
    {
        this.contentType = contentType;
    }

    public String getBoundary ()
    {
        return boundary;
    }

    public void setBoundary (String boundary)
    {
        this.boundary = boundary;
    }

    public List <String> getAccepts ()
    {
        return accepts;
    }

    public void setAccepts (List <String> accepts)
    {
        this.accepts = accepts;
    }

    public String getAcceptEncoding ()
    {
        return acceptEncoding;
    }

    public void setAcceptEncoding (String acceptEncoding)
    {
        this.acceptEncoding = acceptEncoding;
    }

    public String getAcceptLanguage ()
    {
        return acceptLanguage;
    }

    public void setAcceptLanguage (String acceptLanguage)
    {
        this.acceptLanguage = acceptLanguage;
    }

    public String getRawStr ()
    {
        return rawStr;
    }

    public void setRawStr (String rawStr)
    {
        if (this.rawStr == null) this.rawStr = rawStr;
        else this.rawStr += rawStr;
    }

    public <T> T getRaw (final Class <T> rawType)
    {
        return (new Gson ()).fromJson (rawStr , rawType);
    }

    public void clear ()
    {
        new Thread (() ->
        {
            try
            {
                final Map <String, IncomingFiles> fileRequests = getIncomingFiles ();
                if (fileRequests != null)
                {
                    fileRequests.forEach ((key , value) ->
                    {
                        try
                        {
                            final File file = new File (Path.Get (Path.TMP , value.tmpFilename));
                            if (file.exists ()) file.delete ();
                        }
                        catch (Exception ignored)
                        {
                        }
                    });
                    fileRequests.clear ();
                }
                System.gc ();
            }
            catch (final Exception ignored)
            {
            }
        }).start ();
    }

    public void setInputStream (final InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public void setOutputStream (final OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public InputStream getInputStream ()
    {
        return inputStream;
    }

    public OutputStream getOutputStream ()
    {
        return outputStream;
    }

    public static final class Cookie
    {
        public String key, value;
        public String path, domain;
        public String expires;
        public boolean httpOnly, secure;

        public Cookie (final String key , final String value , final String path , final String domain , final LocalDateTime expires , final boolean httpOnly , final boolean secure)
        {
            this.key = key;
            this.value = value;
            this.path = path;
            this.domain = domain;
            this.expires = expires.format (Time.getHeaderDateFormat ());
            this.httpOnly = httpOnly;
            this.secure = secure;
        }
    }
}
