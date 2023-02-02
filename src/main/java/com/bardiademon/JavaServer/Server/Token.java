package com.bardiademon.JavaServer.Server;

import com.bardiademon.JavaServer.bardiademon.Code;
import com.bardiademon.JavaServer.bardiademon.Default;
import com.bardiademon.JavaServer.bardiademon.Path;
import com.bardiademon.JavaServer.bardiademon.Str;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.hmac.HMACSigner;
import io.fusionauth.jwt.hmac.HMACVerifier;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.ZonedDateTime;

public final class Token
{
    public String getSecret()
    {
        return getSecret(false);
    }

    public String getSecret(final boolean createJwtSecret)
    {
        final File file = new File(Path.JWT_SECRET);
        try
        {
            if ((file.exists() && file.length() > 0) || (createJwtSecret && file.createNewFile()))
            {
                String code;
                if (file.length() == 0)
                {
                    final Code codeLong = Code.CreateCodeLong(1000000);
                    codeLong.createCode();
                    code = codeLong.getCode();

                    try (final FileWriter writer = new FileWriter(file))
                    {
                        writer.write(code);
                        writer.flush();
                    }
                }
                else code = new String(Files.readAllBytes(file.toPath()));

                if (!Str.isEmpty(code)) return code;
            }
        }
        catch (Exception ignored)
        {
        }
        return "";
    }

    public String getToken(final String secret , final String value)
    {
        return getToken(secret , value , Default.X_POWERED_BY);
    }

    public String getToken(final String secret , final String value , final String subject)
    {
        return getToken(secret , value , ZonedDateTime.now() , subject);
    }

    public String getToken(final String secret , final String value , final ZonedDateTime issuedAt , final String subject)
    {
        return getToken(secret , value , issuedAt , ZonedDateTime.now().plusDays(1) , subject);
    }

    public String getToken(final String secret , final String value , final ZonedDateTime issuedAt)
    {
        return getToken(secret , value , issuedAt , ZonedDateTime.now().plusDays(1) , Default.X_POWERED_BY);
    }

    public String getToken(final String secret , final String value , final ZonedDateTime issuedAt , final ZonedDateTime expiration)
    {
        return getToken(secret , value , issuedAt , expiration , Default.X_POWERED_BY);
    }

    public String getToken(final String secret , final String value , final ZonedDateTime issuedAt , final ZonedDateTime expiration , final String subject)
    {
        return JWT.getEncoder().encode(new JWT().setIssuer(value)
                .setIssuedAt(issuedAt)
                .setSubject(subject)
                .setExpiration(expiration) , HMACSigner.newSHA256Signer(secret));
    }

    public JWT getValue(final String secret , final String jwtCode)
    {
        try
        {
            return JWT.getDecoder().decode(jwtCode , HMACVerifier.newVerifier(secret));
        }
        catch (Exception ignored)
        {
        }
        return null;
    }
}
