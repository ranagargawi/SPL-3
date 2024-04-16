package bgu.spl.net.srv;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, packets msg);

    void disconnect(int connectionId);
}
