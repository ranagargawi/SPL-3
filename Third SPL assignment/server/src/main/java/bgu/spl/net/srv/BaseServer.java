package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<TftpProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;

    public BaseServer(
            int port,
            Supplier<TftpProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.sock = null;
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("Server started");
            Connections<T> connections = new ConnectionsImpl<>();
            int connectionId = 0;

            this.sock = serverSock; // just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                TftpProtocol<T> tempProtocol = protocolFactory.get();
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        tempProtocol);
                handler.getProtocol().start(connectionId, connections);
                System.out.println("connect client");
                connections.connect(connectionId, (ConnectionHandler<T>) handler);
                connectionId++;
                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
        if (sock != null)
            sock.close();
    }

    protected void execute(BlockingConnectionHandler<T> handler) {
        new Thread(handler).start();
    }

}
