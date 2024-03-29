package com.bardiademon.JavaServer.bardiademon;

public class GetSize
{
    public static String Get(long Byte)
    {
        if (Byte >= 1024)
        {
            float size = (float) Byte / 1024;
            if (size >= 1024)
            {
                size = (size / 1024);
                if (size >= 1024)
                {
                    size = size / 1024;
                    return String.format("%s GB" , toString(size));
                }
                else return String.format("%s MB" , toString(size));
            }
            else return String.format("%s KB" , toString(size));
        }
        else return String.format("%s byte" , toString(Byte));
    }

    private static String toString(double size)
    {
        return String.format("%.3f" , Math.abs(size));
    }
}
