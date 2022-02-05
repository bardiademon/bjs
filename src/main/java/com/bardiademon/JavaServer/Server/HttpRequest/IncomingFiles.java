package com.bardiademon.JavaServer.Server.HttpRequest;

import com.bardiademon.JavaServer.bardiademon.Path;
import com.bardiademon.JavaServer.bardiademon.Str;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

public class IncomingFiles
{
    public final String filename;
    public final String contentType;
    public final String tmpFilename;
    public final String name;
    public final long len;

    private IncomingFiles(final String name , final String contentType , final String filename , final String tmpFilename , final long len)
    {
        this.name = name;
        this.contentType = contentType;
        this.tmpFilename = tmpFilename;
        this.filename = filename;
        this.len = len;
    }

    public InputStream getFile() throws IOException
    {
        final File file = new File(Path.Get(Path.TMP , tmpFilename));
        if (file.exists())
        {
            final FileInputStream inputStream = new FileInputStream(file);
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            final byte[] buffer = new byte[1024 * 8];

            for (int len = 0; len != -1; len = inputStream.read(buffer)) outputStream.write(buffer , 0 , len);

            inputStream.close();
            outputStream.flush();
            outputStream.close();

            System.gc();

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        else throw new IOException("File not exists!");
    }

    public boolean copy(final String to) throws IOException
    {
        return copy(to , false);
    }

    public boolean copy(final String to , final boolean createDir) throws IOException
    {
        return copy(to , filename , createDir);
    }

    public boolean copy(final String to , final String filename , final boolean createDir) throws IOException
    {
        return copy(to , filename , createDir , false);
    }

    public boolean copy(final String to , final boolean createDir , final boolean rewrite) throws IOException
    {
        return copy(to , filename , createDir , rewrite);
    }

    public boolean copy(final String to , String filename , final boolean createDir , final boolean rewrite) throws IOException
    {
        if (Str.isEmpty(filename)) filename = this.filename;

        if (!Str.isEmpty(filename))
        {
            File toFile = new File(to);
            if (toFile.exists() || (createDir && toFile.mkdirs()))
            {
                toFile = new File(Path.Get(toFile.getAbsolutePath() , filename));

                if (toFile.exists() && (rewrite && !toFile.delete()))
                    throw new IOException("Cannot remove this file <" + toFile.getAbsolutePath() + ">");

                if (!toFile.exists()) return (Files.copy(getFile() , toFile.toPath()) >= len);
                else throw new IOException("Filename is exists in path");
            }
            else throw new IOException("Path is not exists");
        }
        else throw new IOException("Filename is a empty!");
    }

    public static IncomingFiles getIncomingFile(final byte[] bytes , final String info) throws Exception
    {
        if (info != null && !info.isEmpty())
        {
            final String[] splitInfo = info.split("\n");

            if (splitInfo.length == 2)
            {
                final String[] filenameAndName = splitInfo[0].split(";");

                // KN => Key Name
                final String contentTypeKN = splitInfo[1];

                final String name = filenameAndName[1].trim().split("=")[1].trim().replace("\"" , "");
                final String filename = filenameAndName[2].trim().split("=")[1].trim().replace("\"" , "");

                final String contentType = contentTypeKN.trim().split(":")[1].trim();

                final String tmpFilename = UUID.randomUUID().toString().replace("-" , "");

                final File tmpFile = new File(Path.Get(Path.TMP , tmpFilename));

                if (tmpFile.getParentFile().exists() || tmpFile.getParentFile().mkdirs())
                {
                    final FileOutputStream stream = new FileOutputStream(tmpFile);

                    stream.write(bytes , 0 , bytes.length);
                    stream.flush();
                    stream.close();

                    return new IncomingFiles(name , contentType , filename , tmpFilename , bytes.length);
                }
                else throw new IOException("Directory tmp not exists!");
            }
        }
        return null;
    }
}
