package org.iq80.leveldb.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ZLib {
    private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);
    private static final ThreadLocal<Inflater> INFLATER_RAW = ThreadLocal.withInitial(() -> new Inflater(true));

    private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(Deflater::new);
    private static final ThreadLocal<Deflater> DEFLATER_RAW = ThreadLocal.withInitial(() -> new Deflater(Deflater.DEFAULT_COMPRESSION, true));

    private ZLib() {
    }

    public static ByteBuffer uncompress(ByteBuffer compressed, boolean raw) throws IOException {
        Inflater inflater = raw ? INFLATER_RAW.get() : INFLATER.get();
        try {
            byte[] inArray;
            int inOffset;
            int inLength = compressed.remaining();
            if (compressed.hasArray()) {
                inArray = compressed.array();
                inOffset = compressed.arrayOffset() + compressed.position();
            } else {
                inArray = new byte[inLength];
                int oldPos = compressed.position();
                compressed.get(inArray);
                compressed.position(oldPos);
                inOffset = 0;
            }

            inflater.setInput(inArray, inOffset, inLength);

            int maxOutputLength = Math.max(inLength * 2, 1024);
            byte[] outArray = new byte[maxOutputLength];
            int outOffset = 0;

            while (!inflater.finished()) {
                if (outOffset == outArray.length) {
                    byte[] newArray = new byte[outArray.length + Math.max(inLength * 2, 1024)];
                    System.arraycopy(outArray, 0, newArray, 0, outArray.length);
                    outArray = newArray;
                }

                int decompressed = inflater.inflate(outArray, outOffset, outArray.length - outOffset);
                if (decompressed == 0) {
                    if (inflater.needsInput()) {
                        throw new IOException("Input truncated");
                    }
                    if (inflater.needsDictionary()) {
                        throw new IOException("Dictionary required");
                    }
                }
                outOffset += decompressed;
            }

            return ByteBuffer.wrap(outArray, 0, outOffset);
        } catch (DataFormatException e) {
            throw new IOException(e);
        } finally {
            inflater.reset();
        }
    }

    public static int compress(byte[] uncompressed, int uncompressedOffset, int uncompressedLength, byte[] compressed, int compressedOffset, boolean raw) throws IOException {
        Deflater deflater = raw ? DEFLATER_RAW.get() : DEFLATER.get();
        try {
            deflater.setInput(uncompressed, uncompressedOffset, uncompressedLength);
            deflater.finish();
            return deflater.deflate(compressed, compressedOffset, compressed.length - compressedOffset);
        } finally {
            deflater.reset();
        }
    }
}
