package me.soda.witch.shared.socket;

import me.soda.witch.shared.LogUtil;
import me.soda.witch.shared.socket.messages.Message;
import me.soda.witch.shared.socket.messages.messages.DisconnectData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class TcpServer {
    private final ServerSocket serverSocket;
    private final HashSet<Connection> conns = new HashSet<>();
    private final ExecutorService connectionThreadPool = Executors.newCachedThreadPool();

    public TcpServer() throws IOException {
        serverSocket = new ServerSocket();
    }

    public void start(int port) throws IOException {
        serverSocket.bind(new InetSocketAddress(port));
        new ServerThread().start();
    }

    public HashSet<Connection> getConnections() {
        return conns;
    }

    public void stop() throws IOException, InterruptedException {
        conns.forEach(connection -> connection.close(DisconnectData.Reason.NORMAL));
        connectionThreadPool.shutdown();
        connectionThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        serverSocket.close();
    }

    public boolean isStopped() {
        return serverSocket.isClosed();
    }

    public abstract void onOpen(Connection connection);

    public abstract void onClose(Connection connection, DisconnectData packet);

    public abstract void onMessage(Connection connection, Message message);


    private class ServerThread extends Thread {
        @Override
        public void run() {
            while (!serverSocket.isClosed()) {
                try {
                    connectionThreadPool.execute(new ServerConnection(serverSocket.accept()));
                } catch (IOException e) {
                    LogUtil.printStackTrace(e);
                }
            }
        }
    }

    private class ServerConnection extends Connection {
        public ServerConnection(Socket socket) throws IOException {
            super(socket);
        }

        @Override
        public void onOpen() {
            conns.add(this);
            TcpServer.this.onOpen(this);
        }

        @Override
        public void onClose(DisconnectData disconnectData) {
            conns.remove(this);
            TcpServer.this.onClose(this, disconnectData);
        }

        @Override
        public void onMessage(Message message) {
            TcpServer.this.onMessage(this, message);
        }
    }
}
