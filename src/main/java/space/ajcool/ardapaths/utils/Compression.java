package space.ajcool.ardapaths.utils;

import space.ajcool.ardapaths.ArdaPaths;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {
    /**
     * Compresses a string.
     *
     * @param str The string to compress
     */
    public static byte[] compress(String str) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
            gzipOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            ArdaPaths.LOGGER.error("Failed to compress string", e);
            return new byte[0];
        }
    }

    /**
     * Decompresses a byte array.
     *
     * @param compressed The byte array to decompress
     */
    public static String decompress(byte[] compressed) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
            byte[] buffer = new byte[1024];
            StringBuilder outStr = new StringBuilder();
            int len;
            while ((len = gzipInputStream.read(buffer)) != -1) {
                outStr.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
            }
            return outStr.toString();
        } catch (IOException e) {
            ArdaPaths.LOGGER.error("Failed to decompress byte array", e);
            return "";
        }
    }
}
