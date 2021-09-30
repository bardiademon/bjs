package com.bardiademon.JavaServer.Server;

import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;

public interface Controller
{
    HttpResponse run (final HttpRequest request);
}
