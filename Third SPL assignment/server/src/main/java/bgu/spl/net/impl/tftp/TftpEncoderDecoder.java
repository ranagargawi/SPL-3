
package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Vector;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.srv.LOGRQ;
import bgu.spl.net.srv.packets;
import bgu.spl.net.srv.packets.ACKPacket;
import bgu.spl.net.srv.packets.BCASTPacket;
import bgu.spl.net.srv.packets.DataPacket;
import bgu.spl.net.srv.packets.ERRORPacket;

public class TftpEncoderDecoder<T> implements MessageEncoderDecoder<T> {
    private short opCode = 0;
    private Vector<Byte> Vbytes = new Vector<Byte>();
    private byte[] opCodeByte = new byte[2];
    private boolean opCodeReceived = false;
    private short packetSize = 0;
    int len = 0;
    private packets packets = new packets(opCode);

    public short getOC(byte[] byteArr) {
        short result = (short) ((byteArr[0] & 0xff) << 8);
        result += (short) (byteArr[1] & 0xff);
        return result;
    }

    private void clearFields() {
        len = 0;
        opCode = 0;
        opCodeReceived = false;
        packetSize = 0;
        opCodeByte = new byte[2];
        Vbytes.clear();
    }

    @Override
    public packets decodeNextByte(byte nextByte) {
        System.out.println("start decoding");
        Vbytes.add(nextByte);
        if (Vbytes.size() == 1)
            opCodeByte[0] = nextByte;
        if (Vbytes.size() == 2) {
            opCodeByte[1] = nextByte;
            opCode = bytesToShort(opCodeByte);
            opCodeReceived = true;
            // byte[] decodedArr = vectorToArray(Vbytes);

        }
        if (opCodeReceived) {
            System.out.println("opcode recieved");
            // byte[] decodedArr = vectorToArray(Vbytes);
            if (len >= 1 && (opCode == 10 || opCode == 6)) {
                return (packets) createEasyPackets(opCode, vectorToArray(Vbytes));
            }
            if (len > 1) {
                if (opCode == 4 && len == 3) {
                    byte[] blocks = Arrays.copyOfRange(vectorToArray(Vbytes), 2, 4);
                    short numOfBlocks = bytesToShort(blocks);
                    ACKPacket ans = packets.new ACKPacket(numOfBlocks);
                    clearFields();
                    return (packets) ans;
                }
                if (opCode == 3) {
                    if (len > 5) {
                        packetSize = bytesToShort(Arrays.copyOfRange(vectorToArray(Vbytes), 2, 4));
                        if (len + 1 == packetSize + 6) {
                            DataPacket ans = packets.new DataPacket(packetSize,
                                    bytesToShort(Arrays.copyOfRange(vectorToArray(Vbytes), 4, 6)),
                                    Arrays.copyOfRange(vectorToArray(Vbytes), 6, len + 1));

                            clearFields();
                            return (packets) ans;
                        }
                    }
                }
                System.out.println("decode");
                if (nextByte == '\0') {
                    System.out.println("111111");
                    if (opCode == 1 || opCode == 2 || (opCode == 5 && len > 3)
                            || opCode == 7
                            || opCode == 8 || opCode == 9) {

                        return (packets) (createDelimiter0Packets(opCode, vectorToArray(Vbytes)));
                    }

                }
            }
        }
        System.out.println("finish decode");
        len++;
        return null;
    }

    public String getErrorMsg(byte[] msg) {
        return new String(msg, 4, msg.length, StandardCharsets.UTF_8);
    }

    public byte[] getErrorCode(byte[] bytes) {
        byte[] error = new byte[2];
        error[0] = (byte) ((bytes[2] & 0xff) << 8);
        error[0] = (byte) ((bytes[3] & 0xff) << 8);
        return error;
    }

    public byte[] getPacketSize(byte[] bytes) {
        byte[] packetSize = new byte[2];
        packetSize[0] = (byte) ((bytes[2] & 0xff) << 8);
        packetSize[1] = (byte) (bytes[3] & 0xff);
        return packetSize;

    }

    public byte[] getBlockNumber(byte[] bytes) {
        byte[] block = new byte[2];
        block[0] = (byte) ((bytes[4] & 0xff) << 8);
        block[1] = (byte) (bytes[5] & 0xff);
        return block;

    }

    public byte[] getBlockNumberOp4(byte[] bytes) {
        byte[] block = new byte[2];
        block[0] = (byte) ((bytes[2] & 0xff) << 8);
        block[0] = (byte) ((bytes[3] & 0xff) << 8);
        return block;

    }

    public byte[] getData(byte[] bytes) {
        byte[] dataArray = Arrays.copyOfRange(bytes, 6, bytes.length);
        return dataArray;

    }

    public byte[] getDatsize(byte[] bytes) {
        byte[] packetSize = new byte[2];
        packetSize[0] = (byte) ((bytes[2] & 0xff) << 8);
        packetSize[1] = (byte) (bytes[3] & 0xff);
        return packetSize;

    }

    public String getfileName(byte[] bytes) {
        for (int i = 2; i < bytes.length - 2; i++) {
            if (bytes[i] == '\0') {
                String userName = new String(bytes, 0, i, StandardCharsets.UTF_8);
                return userName;
            }
        }
        return "what";

    }

    public String getFileNameDelrq(byte[] bytes) {
        for (int i = 2; i < bytes.length - 2; i++) {
            if (bytes[i] == '\0') {
                String userName = new String(bytes, 0, i, StandardCharsets.UTF_8);
                return userName;
            }
        }
        return "what";
    }

    public String getUserName(byte[] bytes) {
        for (int i = 2; i < bytes.length - 2; i++) {
            if (bytes[i] == '\0') {
                String userName = new String(bytes, 0, i, StandardCharsets.UTF_8);
                return userName;
            }
        }
        return "what";

    }

    public byte getIsAdded(byte[] message) {
        return message[2];
    }

    public String getfileNameBcast(byte[] message) {
        String s = new String(message, 3, message.length, StandardCharsets.UTF_8);
        return s;
    }

    private packets createDelimiter0Packets(short opcode, byte[] decoderBuffer) {
        packets s = new packets(opcode);

        switch (opcode) {
            case 1:
                String filename = new String(decoderBuffer, 2, decoderBuffer.length, StandardCharsets.UTF_8);
                s = packets.new RWQpacket(opcode, filename);
                break;
            case 2:
                String filename1 = new String(decoderBuffer, 2, decoderBuffer.length, StandardCharsets.UTF_8);
                s = packets.new RWQpacket(opcode, filename1);
                break;
            case 5:
                short errCode = bytesToShort(Arrays.copyOfRange(decoderBuffer, 2, 4));
                s = packets.new ERRORPacket(errCode, " ");
                break;
            case 7:
                System.out.println("create LOG packet ");
                int index = new String(Arrays.copyOfRange(decoderBuffer, 2, decoderBuffer.length)).indexOf('\0');
                byte[] tempArray = Arrays.copyOfRange(decoderBuffer, 2, index + 2);
                String username3 = new String(tempArray, StandardCharsets.UTF_8);
                s = (packets) new LOGRQ(opcode, username3);
                short x = s.getOpCode();
                System.out.println(x);
                break;
            case 8:
                index = new String(Arrays.copyOfRange(decoderBuffer, 2, decoderBuffer.length)).indexOf('\0');
                tempArray = Arrays.copyOfRange(decoderBuffer, 2, index + 2);
                String deleteFileName = new String(tempArray, StandardCharsets.UTF_8);
                s = packets.new DELQPacket(deleteFileName);
                break;
            case 9:
                byte delOrAdd = decoderBuffer[2];
                String filename5 = new String(decoderBuffer, 3, decoderBuffer.length, StandardCharsets.UTF_8);
                s = packets.new BCASTPacket(delOrAdd, filename5);
                break;

        }
        return s;

    }

    @Override
    public byte[] encode(packets message) {
        byte[] encodedMessage = new byte[518];
        byte[] tempBytes;
        encodedMessage[0] = shortToBytes(message.getOpCode())[0];
        encodedMessage[1] = shortToBytes(message.getOpCode())[1];
        switch (message.getOpCode()) {
            case 9: // broadcast
                encodedMessage[2] = ((BCASTPacket) message).getIsAddedOrDeleted();
                tempBytes = ((BCASTPacket) message).getFilename().getBytes();
                for (int i = 0; i < tempBytes.length; i++) {
                    encodedMessage[i + 3] = tempBytes[i];
                }
                encodedMessage[encodedMessage.length - 1] = 0;

                if (((BCASTPacket) message).getFilename().contains("/0"))
                    encodedMessage = null;
                break;
            case 4:// Acknowledge
                encodedMessage[2] = shortToBytes(((ACKPacket) message).getBlockNum())[0];
                encodedMessage[3] = shortToBytes(((ACKPacket) message).getBlockNum())[1];
                break;
            case 5:// Error
                encodedMessage[2] = shortToBytes(((ERRORPacket) message).getErrorCode())[0];
                encodedMessage[3] = shortToBytes(((ERRORPacket) message).getErrorCode())[1];
                tempBytes = ((ERRORPacket) message).getErrorMessage().getBytes();
                for (int i = 0; i < tempBytes.length; i++) {
                    encodedMessage[i + 4] = tempBytes[i];
                }
                encodedMessage[encodedMessage.length - 1] = 0;
                if (((ERRORPacket) message).getErrorMessage().contains("/0"))
                    encodedMessage = null;
                break;
            case 3:// Data
                encodedMessage[2] = shortToBytes(((DataPacket) message).getDataSize())[0];
                encodedMessage[3] = shortToBytes(((DataPacket) message).getDataSize())[1];
                encodedMessage[4] = shortToBytes(((DataPacket) message).getBlockNum())[0];
                encodedMessage[5] = shortToBytes(((DataPacket) message).getBlockNum())[1];
                for (int i = 0; i < ((DataPacket) message).getData().length; i++) {
                    encodedMessage[i + 6] = ((DataPacket) message).getData()[i];
                }
                break;
        }
        return encodedMessage;

    }

    private packets createEasyPackets(short opcode, byte[] myBuffer) {
        packets s = new packets(opcode);
        switch (opcode) {
            case 6:
                s = packets.new DIRQPacket();
                break;
            case 10:
                s = packets.new DISCPacket();
                break;

        }
        clearFields();
        return s;
    }

    public byte[] shortToBytes(short num) {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte) ((num >> 8) & 0xFF);
        bytesArr[1] = (byte) (num & 0xFF);
        return bytesArr;
    }

    public static short bytesToShort(byte[] byteArr) {
        short result = (short) ((byteArr[0] & 0xff) << 8);
        result += (short) (byteArr[1] & 0xff);
        return result;
    }

    private byte[] vectorToArray(Vector<Byte> vec) {
        byte[] bytesArr = new byte[vec.size()];
        for (int i = 0; i < bytesArr.length; i++)
            bytesArr[i] = vec.remove(0);
        return bytesArr;
    }

    // private packets decode(byte[] message) {
    // packets decodeMessage;
    // byte[] tempArray;
    // int index;
    // switch (opCode) {
    // case 2:
    // index = (new String(Arrays.copyOfRange(message, 2,
    // message.length))).indexOf(0);
    // tempArray = Arrays.copyOfRange(message, 2, index + 2);
    // String fileName = new String(tempArray, StandardCharsets.UTF_8);
    // decodeMessage = packet.new RWQpacket(opCode, fileName);
    // break;
    // case 3:
    // short dataSize = bytesToShort(Arrays.copyOfRange(message, 2, 4));
    // short blockNum = bytesToShort(Arrays.copyOfRange(message, 4, 6));
    // tempArray = Arrays.copyOfRange(message, 6, dataSize + 6);
    // decodeMessage = packet.new DataPacket(dataSize, blockNum, tempArray);
    // this.datePacketSize = 0;
    // break;
    // case 4:
    // decodeMessage = packet.new ACKPacket(bytesToShort(Arrays.copyOfRange(message,
    // 2, 4)));
    // break;
    // default:
    // decodeMessage = (packet.new ERRORPacket((short) 4)).getBytes();
    // break;
    // case 6:
    // decodeMessage = packet.new DIRQPacket();
    // break;
    // case 7:
    // index = (new String(Arrays.copyOfRange(message, 2,
    // message.length))).indexOf(0);
    // tempArray = Arrays.copyOfRange(message, 2, index + 2);
    // String username = new String(tempArray, StandardCharsets.UTF_8);
    // decodeMessage = packet.new LOGRQPacket(username);
    // break;
    // case 8:
    // index = (new String(Arrays.copyOfRange(message, 2,
    // message.length))).indexOf(0);
    // tempArray = Arrays.copyOfRange(message, 2, index + 2);
    // String deleteFileName = new String(tempArray, StandardCharsets.UTF_8);
    // decodeMessage = packet.new DELQPacket(deleteFileName);
    // break;
    // case 10:
    // decodeMessage = packet.new DISCPacket();
    // }

    // this.len = 0;
    // Vbytes.clear();
    // this.opCode = 0;
    // return (packets) decodeMessage;
    // }

}