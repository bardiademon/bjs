package com.bardiademon.JavaServer.bardiademon;

import com.bardiademon.JavaServer.Server.HttpRequest.HttpRequest;
import com.bardiademon.JavaServer.Server.HttpResponse;

public interface Controller
{
    HttpResponse run (final HttpRequest request);
}
