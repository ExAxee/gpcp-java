package org.gpcp.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Packet {

    public static final int HEADER_LENGTH = 4;
    public static final ByteOrder HEADER_BYTEORDER = ByteOrder.BIG_ENDIAN;
    public static final Charset ENCODING = StandardCharsets.UTF_8;

    private Packet() {
    }

    public static void sendAll(final Socket socket, final String data) throws IOException {
        final OutputStream outputStream = socket.getOutputStream();

        byte[] bytes = data.getBytes(ENCODING);
        outputStream.write(ByteBuffer.allocate(HEADER_LENGTH).order(HEADER_BYTEORDER)
                .putInt(bytes.length).array()); // header

        outputStream.write(bytes); // data
    }

    public static String receiveAll(final Socket socket) throws IOException {
        final InputStream inputStream = socket.getInputStream();

        final byte[] headerBytes = new byte[HEADER_LENGTH];
        int count = inputStream.read(headerBytes);
        if (count != HEADER_LENGTH) {
            return null;
        }

        final int dataLength = ByteBuffer.wrap(headerBytes).getInt();
        final byte[] dataBytes = new byte[dataLength];
        count = inputStream.read(dataBytes);
        if (count != dataLength) {
            return null;
        }

        return new String(dataBytes, ENCODING);
    }
}
