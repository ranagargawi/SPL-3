package bgu.spl.net.impl.tftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import bgu.spl.net.api.MessageEncoderDecoder;

public class ConnectionHandler {

   private String host;
   private int port;
   private Socket socket;
   private MessageEncoderDecoder<byte[]> encdec;

   public ConnectionHandler(String host, int port) {
      this.host = host;
      this.port = port;
      this.encdec = new TftpEncoderDecoder();
   }

   public boolean connect() {
      try {
         socket = new Socket();
         socket.connect(new InetSocketAddress(host, port));
         return true;
      } catch (IOException e) {
         System.err.println("Connection failed (Error: " + e.getMessage() + ")");
         return false;
      }
   }

   public boolean getBytes(byte[] bytes, int bytesToRead) {
      try {
         int bytesRead = socket.getInputStream().read(bytes, 0, bytesToRead);
         return bytesRead != -1;
      } catch (IOException e) {
         System.err.println("recv failed (Error: " + e.getMessage() + ")");
         return false;
      }
   }

   public boolean sendBytes(byte[] bytes) {
      try {
         socket.getOutputStream().write(bytes);
         return true;
      } catch (IOException e) {
         System.err.println("send failed (Error: " + e.getMessage() + ")");
         return false;
      }
   }

   public boolean getLine(StringBuilder line) {
      byte[] buffer = new byte[1];
      try {
         while (getBytes(buffer, 1)) {
            char ch = (char) buffer[0];
            line.append(ch);
            if (ch == '\n') {
               return true;
            }
         }
      } catch (Exception e) {
         System.err.println("recv failed (Error: " + e.getMessage() + ")");
      }
      return false;
   }

   public boolean sendLine(String line) {
      return sendBytes(line.getBytes(StandardCharsets.UTF_8));
   }

   public byte[] getPacket() {
      byte[] buffer = new byte[1];
      int index = 0;
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
         while (getBytes(buffer, 1)) {
            byte[] packet = encdec.decodeNextByte(buffer[0]);
            if (packet != null) {
               outputStream.write(packet);
               return outputStream.toByteArray();
            }
            if (++index > 517) {
               break;
            }
         }
      } catch (Exception e) {
         System.err.println("recv failed (Error: " + e.getMessage() + ")");
      }
      return null;
   }

   public void sendPacket(byte[] packet) {
      sendBytes(packet);
   }

}
