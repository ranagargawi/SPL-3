package bgu.spl.net.srv;

public class packets {
   private short opcode;

   public packets(short opCode) {
      this.opcode = opCode;
   }

   public short getOpCode() {
      return this.opcode;
   }

   public byte[] getBytes() {
      // Convert the opcode to bytes and return them
      byte[] bytes = new byte[2];
      bytes[0] = (byte) ((opcode >> 8) & 0xFF);
      bytes[1] = (byte) (opcode & 0xFF);
      return bytes;
   }

   public class RWQpacket extends packets {
      private String filename;

      public RWQpacket(short opCode, String filename) {
         super((short) opCode);
         this.filename = filename;
      }

      public String getFilename() {
         return this.filename;
      }
   }

   public class LOGRQPacket extends packets {
      private String userName;

      public LOGRQPacket(short opcode, String userName) {
         super((short) 7);
         this.userName = userName;
      }

      public String getUsername() {
         return this.userName;
      }
   }

   public class ERRORPacket extends packets {
      private short errorCode;
      private String errorMessage;

      public ERRORPacket(short errorCode, String errorMessage) {
         super((short) 5);
         this.errorCode = errorCode;

         switch (errorCode) {
            case 0:
               this.errorMessage = "undefined Error.";
               break;
            case 1:
               this.errorMessage = "Requested File not found, could not complete operation.";
               break;
            case 2:
               this.errorMessage = "Access violation \u2013 File cannot be written, read or deleted.";
               break;
            case 3:
               this.errorMessage = "Disk full or allocation exceeded \u2013 No room in disk.";
               break;
            case 4:
               this.errorMessage = "Illegal TFTP operation \u2013 Unknown Opcode.";
               break;
            case 5:
               this.errorMessage = "File already exists \u2013 File name exists on WRQ.";
               break;
            case 6:
               this.errorMessage = "User not logged in \u2013 Any opcode received before Login completes.";
               break;
            case 7:
               this.errorMessage = "User already logged in \u2013 Login username already connected.";
         }

      }

      public short getErrorCode() {
         return errorCode;
      }

      public String getErrorMessage() {
         return errorMessage;
      }
   }

   public class DISCPacket extends packets {

      public DISCPacket() {
         super((short) 10);
      }
   }

   public class DIRQPacket extends packets {
      public DIRQPacket() {
         super((short) 6);
      }

      public byte[] getBytes() {
         // Assuming that the packet structure is fixed
         byte[] bytes = new byte[2];
         bytes[0] = (byte) ((this.getOpCode() >> 8) & 0xFF);
         bytes[1] = (byte) (this.getOpCode() & 0xFF);
         return bytes;
      }
   }

   public class DELQPacket extends packets {
      private String FileName;

      public DELQPacket(String FileName) {
         super((short) 8);
         this.FileName = FileName;
      }

      public String getFilename() {
         return this.FileName;
      }
   }

   public class DataPacket extends packets {
      private short dataSize;
      private short blockNum;
      private byte[] data;
      int size = 0;

      public DataPacket(short dataSize, short blockNum, byte[] data) {
         super((short) 3);
         this.dataSize = dataSize;
         this.blockNum = blockNum;
         this.data = data;
      }

      public short getDataSize() {
         return this.dataSize;
      }

      public short getBlockNum() {
         return this.blockNum;
      }

      public byte[] getData() {
         return this.data;
      }

   }

   public class BCASTPacket extends packets {
      private byte isAdded;
      private String filename;

      public BCASTPacket(byte isAdded, String filename) {
         super((short) 9);
         this.isAdded = isAdded;
         this.filename = filename;
      }

      public byte getIsAddedOrDeleted() {
         return this.isAdded;
      }

      public String getFilename() {
         return this.filename;
      }
   }

   public class ACKPacket extends packets {

      private short blockNum;

      public ACKPacket(short blockNum) {
         super((short) 4);
         this.blockNum = blockNum;
      }

      public short getBlockNum() {
         return this.blockNum;
      }

   }
}
