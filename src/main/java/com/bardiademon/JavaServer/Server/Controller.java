package com.bardiademon.JavaServer.Server;

import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;
import com.bardiademon.JavaServer.Server.HttpResponse.HttpResponse;

public interface Controller
{
    HttpResponse run (final HttpRequest request);
}
