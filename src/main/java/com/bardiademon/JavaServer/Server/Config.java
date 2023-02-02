package com.bardiademon.JavaServer.Server;

import com.bardiademon.JavaServer.bardiademon.Path;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

final class Config
{
    private int port;
    private String host;

    private boolean ok;

    Config()
    {
        try
        {
            final File configFile = new File(Path.CONFIG_BJS);
            if (configFile.exists() && configFile.length() > 0)
            {
                final JSONObject configJson = new JSONObject(new String(Files.readAllBytes(configFile.toPath()) , StandardCharsets.UTF_8));
                port = configJson.getInt(KeyJson.port.name());

                try
                {
                    host = configJson.getString(KeyJson.host.name());
                }
                catch (JSONException ignored)
                {
                }

                ok = true;
            }
            else throw new IOException("config.bjs not found");
        }
        catch (IOException | JSONException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isOk()
    {
        return ok;
    }

    public int getPort()
    {
        return port;
    }

    public String getHost()
    {
        return host;
    }

    enum KeyJson
    {
        port, host
    }
}
