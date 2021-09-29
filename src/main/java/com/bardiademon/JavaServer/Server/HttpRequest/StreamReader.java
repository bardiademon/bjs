package com.bardiademon.JavaServer.Server.HttpRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class StreamReader
{

    public StreamReader ()
    {
    }

    public void read (final InputStream inputStream , final OnReader onReader)
    {
        new Thread (() ->
        {
            try
            {
                if (inputStream.available () > 0)
                {
                    final byte[] buffer = new byte[1024 * 3];

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream ();
                    ByteArrayOutputStream lineStream = new ByteArrayOutputStream ();

                    for (int len = 0; len != -1; len = inputStream.read (buffer))
                    {
                        outputStream.write (buffer , 0 , len);
                        outputStream.flush ();
                        outputStream.close ();

                        final byte[] bytes = outputStream.toByteArray ();

                        for (int i = 0, bytesLength = bytes.length; i < bytesLength; i++)
                        {
                            final byte aByte = bytes[i];
                            lineStream.write (aByte);

                            // (i + 1) >= bytes.length -> vaghti be akharin khat mirese new line nadare pas shart dovomi kar nemikone
                            if ((i + 1) >= bytes.length || new String (new byte[] { aByte } , StandardCharsets.UTF_8).equals ("\n"))
                            {
                                lineStream.flush ();
                                lineStream.close ();

                                if (onReader.line (new String (lineStream.toByteArray () , StandardCharsets.UTF_8).trim () , lineStream.toByteArray ()))
                                    lineStream = new ByteArrayOutputStream ();
                                else
                                {

                                    System.gc ();
                                    return;
                                }
                            }
                        }

                        outputStream.flush ();
                        outputStream.close ();
                        outputStream = new ByteArrayOutputStream ();
                    }
                }
                else onReader.line ("|bardiademon.NULL|" , null);
            }
            catch (final IOException ignored)
            {
            }
        }).start ();
    }

    public interface OnReader
    {
        boolean line (final String line , byte[] bytes);
    }

}
