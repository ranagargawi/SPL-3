package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

   private ConcurrentHashMap<Integer, ConnectionHandler<T>> users_Ids;

   public ConnectionsImpl() {
      users_Ids = new ConcurrentHashMap<>();
   }

   @Override
   public void connect(int connectionId, ConnectionHandler<T> handler) {
      System.out.println("try to connect client");
      users_Ids.putIfAbsent((Integer) connectionId, handler);
      System.out.println("client is connected ");
   }

   @Override
   public void disconnect(int connectionId) {
      if (users_Ids.contains((Integer) connectionId)) {
         try {
            users_Ids.get((Integer) connectionId).close();
         } catch (IOException e) {
         }
         users_Ids.remove((Integer) connectionId);

      }
   }

   public ConcurrentHashMap<Integer, ConnectionHandler<T>> getUsers() {
      return users_Ids;
   }

   @Override
   public boolean send(int connectionId, packets msg) {
      if (users_Ids.contains((Integer) connectionId)) {
         users_Ids.get((Integer) connectionId).send(msg);
         return true;
      }
      return false;
   }

}
