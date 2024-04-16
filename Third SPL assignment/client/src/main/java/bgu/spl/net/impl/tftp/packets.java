package bgu.spl.net.impl.tftp;

import java.nio.ByteBuffer;

public class packets {
   private short opcode;

   public packets(short opcode) {
      this.opcode = opcode;
   }

   public String toString() {
      return "";
   }

   public short getOpCode() {
      return opcode;
   }

   public byte[] getBytes() {
      // Convert the opcode to bytes and return them
      byte[] bytes = new byte[2];
      bytes[0] = (byte) ((opcode >> 8) & 0xFF);
      bytes[1] = (byte) (opcode & 0xFF);
      return bytes;
   }

   public class ACKPacket extends packets {
      private short blockNum;

      public ACKPacket(short opcode, short blockNum) {
         super(opcode);
         this.blockNum = blockNum;
      }

      public String toString() {
         return "ACK " + blockNum;
      }

      public short getBlockNumber() {
         return blockNum;
      }

      public byte[] getBytes() {
         byte[] bytes = new byte[4];
         ByteBuffer.wrap(bytes).putShort(this.getOpCode());
         ByteBuffer.wrap(bytes, 2, 2).putShort(this.blockNum);
         return bytes;
      }
   }

   public class BCASTPacket extends packets {
      private byte delOrAdd;
      private String fileName;

      public BCASTPacket(short opcode, byte deloradd, String name) {
         super(opcode);
         delOrAdd = deloradd;
         fileName = name;
      }

      public String toString() {
         if (((Byte) delOrAdd).intValue() == 0) {
            return "BCAST " + " del" + fileName;
         } else
            return "BCAST " + " add" + fileName;
      }

      public byte getDeletedOrAdded() {
         return delOrAdd;
      }

      public String getFileName() {
         return fileName;
      }
   }

   public class DATAPacket extends packets {
      private short dataSize;
      private short numOfBlock;
      private byte[] currentDataBlockSize;
      private byte[] data;
      private short packetSize;

      public DATAPacket(short opcode, short packetSize, short numOfBlock, byte[] currentDataBlockSize) {
         super(opcode);
         this.dataSize = dataSize;
         this.packetSize = packetSize;
         this.data = data;
         this.numOfBlock = numOfBlock;
         this.currentDataBlockSize = currentDataBlockSize;
      }

      public byte[] getData() {
         return data;
      }

      public short getPacketSize() {
         return packetSize;
      }

      public short getDataSize() {
         return dataSize;
      }

      public byte[] getCurrentDataBlockSize() {
         return currentDataBlockSize;
      }

      public short getNumOfBlock() {
         return numOfBlock;
      }

      public String toString() {

         return "DataPacket, current packet size is: " + dataSize + " in chunk number: " + numOfBlock + " ";
      }

      public byte[] getBytes() {
         byte[] bytes = new byte[6 + currentDataBlockSize.length];
         ByteBuffer.wrap(bytes).putShort(this.getOpCode());
         ByteBuffer.wrap(bytes, 2, 2).putShort(dataSize);
         ByteBuffer.wrap(bytes, 4, 2).putShort(numOfBlock);
         System.arraycopy(currentDataBlockSize, 0, bytes, 6, currentDataBlockSize.length);
         return bytes;
      }
   }

   public class DELERQPacket extends packets {

      String fileName;

      public DELERQPacket(short opcode, String filename) {
         super(opcode);
         fileName = filename;
      }

      public String toString() {
         return "DELRQ " + fileName;
      }

      public String getFileName() {
         return fileName;
      }
   }

   public class DIRQPacket extends packets {
      public DIRQPacket(short opcode) {
         super(opcode);
      }

      public String toString() {
         return "DIRQ";
      }

      public byte[] getBytes() {
         // Assuming that the packet structure is fixed
         byte[] bytes = new byte[2];
         bytes[0] = (byte) ((this.getOpCode() >> 8) & 0xFF);
         bytes[1] = (byte) (this.getOpCode() & 0xFF);
         return bytes;
      }
   }

   public class DISCPacket extends packets {

      public DISCPacket(short opcode) {
         super(opcode);
      }

      public String toString() {
         return "DISC";
      }
   }

   public class ErrorPacket extends packets {
      short errorCode;
      String errorMesssage;

      public ErrorPacket(short opcode, short errorcode, String errormessage) {// remember delimiter
         super(opcode);
         errorCode = errorcode;
         errorMesssage = errormessage;
      }

      public short getErrorCode() {
         return errorCode;
      }

      public String getErrorMessage() {
         return errorMesssage;
      }

      public String toString() {
         return "Error " + errorCode;
      }
   }

   public class LOGRQPakcet extends packets {

      String username;

      public LOGRQPakcet(short opcode, String user) {
         super(opcode);
         username = user;
      }

      public String toString() {
         return "LOGRQ " + username;
      }

      public String getUserName() {
         return username;
      }

   }

   public class RRQPacket extends packets {
      String fileName;

      public RRQPacket(short opcode, String filename) {
         super(opcode);
         fileName = filename;
      }

      public String toString() {
         return "RRQ " + fileName;
      }

      public String getFileName() {
         return fileName;
      }

   }

   public class WRQPacket extends packets {
      String fileName;

      public WRQPacket(short opcode, String filename) {
         super(opcode);
         fileName = filename;
      }

      public String toString() {
         return "WRQ " + fileName;
      }

      public String getFileName() {
         return fileName;
      }
   }

}