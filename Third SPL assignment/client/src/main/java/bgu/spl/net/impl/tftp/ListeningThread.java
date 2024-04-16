package bgu.spl.net.impl.tftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ListeningThread implements Runnable {

   private ConnectionHandler myConnectionHandler;
   private TftpClient myClient;

   public ListeningThread(ConnectionHandler c, TftpClient currClient) {
      this.myConnectionHandler = c;
      this.myClient = currClient;
   }

   @Override
   public void run() {
      Scanner scanner = new Scanner(System.in);
      try {
         while (myClient.getNotInterupted()) {// synchronization..
            System.out.print("Enter command: ");
            String line = scanner.nextLine();
            packets packetToBeSent = myClient.getPacket(line);
            myConnectionHandler.sendPacket(packetToBeSent.getBytes());
         }
      } catch (Exception error) {
      }
      scanner.close();

   }
}