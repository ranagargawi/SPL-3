package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.srv.packets;

public interface BidiMessagingProtocol<T> {
    /**
     * Used to initiate the current client protocol with it's personal connection ID
     * and the connections implementation
     **/
    void start(int connectionId, Connections<T> myConnections);

    void process(packets message);

    /**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();

}
