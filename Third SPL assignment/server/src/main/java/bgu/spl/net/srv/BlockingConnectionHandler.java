package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.tftp.TftpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final TftpProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    private int connectionId;
    private Connections<T> myConnections;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, TftpProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        protocol.start(this.connectionId, myConnections);
        try (Socket sock = this.sock) { // just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                packets nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage == null) {
                    System.out.println("null");
                }
                if ((packets) nextMessage != (packets) null) {
                    System.out.println("go to procces");
                    protocol.process((packets) nextMessage);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(packets msg) {
        // IMPLEMENT IF NEEDED
        if (msg != null) {
            try {
                out.write(encdec.encode(msg));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public BidiMessagingProtocol<T> getProtocol() {
        return protocol;
    }
}
