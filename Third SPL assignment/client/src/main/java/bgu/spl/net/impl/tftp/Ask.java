package bgu.spl.net.impl.tftp;

public class Ask implements Runnable {

   private ConnectionHandler serverConnectionHandler;
   private TftpClient currClient;

   public Ask(ConnectionHandler c, TftpClient client) {
      this.serverConnectionHandler = c;
      this.currClient = client;
   }

   @Override
   public void run() {
      while (currClient.getNotInterupted()) {
         byte[] packetFromServer = serverConnectionHandler.getPacket();
         if ((currClient.askedForFile || currClient.dirc) && packetFromServer == null) {
            packetFromServer = new byte[0]; // Assuming null packet should be an empty byte array
         }
         packets packet = currClient.getPacket(new String(packetFromServer));
         currClient.process(packet, serverConnectionHandler);
      }
   }
}
