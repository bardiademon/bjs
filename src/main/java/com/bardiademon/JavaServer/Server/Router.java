package com.bardiademon.JavaServer.Server;

import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;
import com.bardiademon.JavaServer.Server.HttpRequest.Method;
import com.bardiademon.JavaServer.Server.HttpResponse.BjsHtml;
import com.bardiademon.JavaServer.Server.HttpResponse.HttpResponse;
import com.bardiademon.JavaServer.Server.HttpResponse.ResponseFile;
import com.bardiademon.JavaServer.bardiademon.Path;
import com.bardiademon.JavaServer.bardiademon.Str;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public final class Router
{
    public final String[] path;
    public final Method method;
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    public final Controller controller;

    public Router(final Controller controller , final String[] path , final Method method)
    {
        this.controller = controller;
        this.path = path;
        this.method = method;
    }

    public static Router setController(final Controller controller)
    {
        return new Router(controller , null , null);
    }

    public void doing(final HttpRequest request , final HttpResponse response) throws HandlerException
    {
        this.httpRequest = request;
        this.httpResponse = response;

        if (!Str.isEmpty(Path.staticPath))
        {
            Path.setTemplatePath();

            final File staticPathFile = new File(Path.staticPath);
            if (staticPathFile.exists())
            {
                if (checkResponseType(response))
                {
                    switch (response.getResponseType())
                    {
                        case html:
                            resHtml();
                            break;
                        case text:
                            resText();
                            break;
                        case stream:
                            resStream();
                            break;
                        case file:
                            resFile();
                            break;
                        case bjs:
                            resBJS();
                            break;
                        default:
                            throw new HandlerException(HandlerException.Message.invalid_response_type);
                    }
                }
                // else throw new HandlerException from checkResponseType
            }
            else throw new HandlerException(HandlerException.Message.static_path_is_not_exists);
        }
        else throw new HandlerException(HandlerException.Message.static_path_is_empty);

        request.clear();
    }

    private void resFile()
    {
        final ResponseFile responseFile = httpResponse.getResponseFile();
        HttpResponse.writeFile(httpRequest.getOutputStream() , responseFile.file , responseFile.filename , httpResponse.getStatusCode() , httpResponse.getContentType());
    }

    private void resBJS() throws HandlerException
    {
        final String bjsFilename = httpResponse.getBjsHtmlFilename();
        if (!Str.isEmpty(bjsFilename))
        {

            final File bjsFile = new File(Path.GetWithFilename(bjsFilename , "bjs.html" , Path.TEMPLATE));
            if (bjsFile.exists())
            {
                try
                {
                    final BjsHtml bjsHtml = new BjsHtml(bjsFile.getAbsolutePath() , httpResponse.getParamBjsHtml());
                    bjsHtml.apply();

                    write(bjsHtml.getHtml());
                }
                catch (final Exception e)
                {
                    throw new HandlerException(e.getMessage());
                }
            }
            else
                throw new HandlerException(HandlerException.Message.html_file_not_exists + " {" + bjsFilename + "}");
        }
        else throw new HandlerException("BJS is null!");
    }

    private void resHtml() throws HandlerException
    {
        final File htmlFile = new File(Path.GetWithFilename(httpResponse.getHtmlFile() , "html" , Path.TEMPLATE));
        if (htmlFile.exists())
        {
            try
            {
                final List<String> lines = Files.readAllLines(htmlFile.toPath());

                final StringBuilder html = new StringBuilder();
                for (final String line : lines) html.append(line);

                write(html.toString());
            }
            catch (final IOException exception)
            {
                throw new HandlerException(exception.getMessage());
            }
        }
        else throw new HandlerException(HandlerException.Message.html_file_not_exists);
    }

    private void resText() throws HandlerException
    {
        write(httpResponse.getText());
    }

    private void resStream() throws HandlerException
    {
        HttpResponse.write(httpRequest.getOutputStream() , httpResponse , httpResponse.getStream());
    }

    private boolean checkResponseType(final HttpResponse response) throws HandlerException
    {
        if (response.getResponseType() != null)
        {
            switch (response.getResponseType())
            {
                case html:
                {
                    if (Str.isEmpty(response.getHtmlFile()))
                        throw new HandlerException(HandlerException.Message.html_file_is_null);
                    break;
                }
                case text:
                {
                    if (Str.isEmpty(response.getText()))
                        throw new HandlerException(HandlerException.Message.text_is_null);
                    break;
                }
                case stream:
                {
                    try
                    {
                        if (response.getStream() == null || response.getStream().available() == 0)
                            throw new HandlerException(HandlerException.Message.stream_is_null);
                    }
                    catch (IOException exception)
                    {
                        throw new HandlerException(exception.getMessage());
                    }
                    break;
                }
                case file:
                {
                    if (httpResponse.getResponseFile() == null || !httpResponse.getResponseFile().file.exists())
                        throw new HandlerException(HandlerException.Message.file_not_exists);

                    break;
                }
            }
        }
        else throw new HandlerException(HandlerException.Message.response_type_is_null);


        return true;
    }

    private void write(final String textHtml) throws HandlerException
    {
        HttpResponse.write(httpRequest.getOutputStream() , httpResponse , textHtml);
    }

    public static class HandlerException extends Exception
    {
        HandlerException(final Message message)
        {
            super(message.name());
        }

        public HandlerException(final String message)
        {
            super(message);
        }

        public enum Message
        {
            static_path_is_not_exists, static_path_is_empty, invalid_response_type,
            html_file_is_null, text_is_null, stream_is_null, response_type_is_null,
            html_file_not_exists, file_not_exists, bjs_file_not_exists
        }
    }
}
