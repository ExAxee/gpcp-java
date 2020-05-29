package org.gpcp;

import org.gpcp.utils.BaseHandler;
import org.gpcp.utils.Packet;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server extends Thread {
    final BaseHandler.Factory<?> handlerFactory;
    final ServerSocket serverSocket;
    List<SocketThread> socketThreads;

    Server(final BaseHandler.Factory<?> handlerFactory, boolean reuseAddress) throws IOException {
        this.handlerFactory = handlerFactory;
        serverSocket = new ServerSocket();

        serverSocket.setReuseAddress(reuseAddress);
    }


    public void startServer(final String ip, final int port) throws IOException {
        startServer(ip, port, 5);
    }

    public void startServer(final String ip, final int port, final int buffer) throws IOException {
        serverSocket.bind(new InetSocketAddress(ip, port));
        serverSocket.setReceiveBufferSize(buffer);
        start();
    }


    public void closeConnection(final String ip) throws IOException {
        for (final SocketThread socketThread : socketThreads) {
            if (socketThread.getIp().equals(ip)) {
                socketThread.closeConnection();
            }
        }
    }

    public void stopServer() throws IOException {
        interrupt();

        for (final SocketThread socketThread : socketThreads) {
            socketThread.closeConnection();
        }
    }


    @Override
    public void run() {
        socketThreads = new ArrayList<>();
        while (!isInterrupted()) {
            try {
                final SocketThread socketThread =
                        new SocketThread(serverSocket.accept(), handlerFactory.buildHandler());
                socketThread.run();
                socketThreads.add(socketThread);
            } catch (Exception e) {
                // TODO better error handling
                e.printStackTrace();
            }
        }
    }

    private static class SocketThread extends Thread {
        private final Socket socket;
        private final BaseHandler handler;

        private SocketThread(final Socket socket, final BaseHandler handler) {
            this.socket = socket;
            this.handler = handler;
        }

        public String getIp() {
            return new String(socket.getInetAddress().getAddress());
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    final String data = Packet.receiveAll(socket);
                    if (data == null) {
                        break;
                    }

                    Packet.sendAll(socket, handler.handleData(data));
                }

                closeConnection();

            } catch (InterruptedIOException e) {
                // TODO remove printStackTrace
                e.printStackTrace();
            } catch (Exception e) {
                // TODO better error handling
                e.printStackTrace();
            }

            try {
                closeConnection();
            } catch (IOException e) {
                // TODO better error handling
                e.printStackTrace();
            }
        }

        public void closeConnection() throws IOException {
            interrupt();
            socket.close();
        }
    }
}
