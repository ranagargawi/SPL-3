package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.srv.packets;
import bgu.spl.net.srv.packets.ACKPacket;
import bgu.spl.net.srv.packets.DELQPacket;
import bgu.spl.net.srv.packets.DataPacket;
import bgu.spl.net.srv.packets.ERRORPacket;
import bgu.spl.net.srv.packets.LOGRQPacket;
import bgu.spl.net.srv.packets.RWQpacket;

public class TftpProtocol<T> implements BidiMessagingProtocol<T> {
    private int connectionId;
    boolean isOnline = false;
    Connections<T> connections;
    private boolean terminate = false;
    static private ConcurrentHashMap<Integer, String> activeUsers = new ConcurrentHashMap<>();
    private Vector<ERRORPacket> errors = new Vector<>();
    private ConcurrentLinkedQueue<DataPacket> data;
    private String fileNameToUse;
    private packets packets;

    @Override
    public void start(int connectionId, Connections<T> connections) {
        this.connectionId = connectionId;
        this.connections = (ConnectionsImpl<T>) connections;

    }

    @Override
    public void process(packets message) {
        System.out.println("start proccess");
        short op = message.getOpCode();

        if (isOnline || op == 7) {
            switch (op) {
                case 1:// RRQ
                    if (!isOnline)
                        connections.send(connectionId, packets.new ERRORPacket((short) 6, " "));
                    else {
                        try {
                            downloadFile(((RWQpacket) message).getFilename());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 2: // WRQ
                    if (!isOnline)
                        connections.send(connectionId, packets.new ERRORPacket(((short) 6), " "));
                    else {
                        String fileName = (((RWQpacket) message).getFilename());
                        File absent = new File("Files/" + fileName);
                        if (absent.exists()) // the file was already uploaded
                            errors.add(packets.new ERRORPacket((short) 1,
                                    "File already exists - File name exists on WRQ"));
                        else {
                            if (errors.isEmpty()) {
                                createData();
                                fileNameToUse = fileName;
                                connections.send(connectionId, packets.new ACKPacket((short) 0));
                            }

                        }
                    }
                    break;
                case 3: // DATA
                    if (!isOnline)
                        connections.send(connectionId, packets.new ERRORPacket(((short) 6), " "));
                    else {
                        data.add((DataPacket) message);
                        connections.send(connectionId, packets.new ACKPacket(((DataPacket) message).getBlockNum()));
                        if (((DataPacket) message).getDataSize() < 512) {// Last DATAPacket
                            short numOfBlocks = ((DataPacket) message).getBlockNum();
                            byte[] file = new byte[numOfBlocks * 512];
                            byte[][] fileChunks = new byte[numOfBlocks][];
                            for (int i = 0; i < numOfBlocks; i++)
                                fileChunks[i] = data.poll().getData();
                            int counter = 0;
                            for (int i = 0; i < numOfBlocks; i++)
                                for (int j = 0; j < fileChunks[i].length; j++) {
                                    file[counter] = fileChunks[i][j];
                                    counter++;
                                }
                            FileOutputStream fos;
                            try {
                                fos = new FileOutputStream("Files/" + fileNameToUse);
                                fos.write(file);
                                fos.close();
                                new File("Files/" + fileNameToUse);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            broadcast(packets.new BCASTPacket((byte) 1, fileNameToUse));
                        }
                    }
                    break;

                case 4: // ACK
                    if (!isOnline) {
                        connections.send(connectionId, packets.new ERRORPacket(((short) 6), " "));
                    } else {
                        if (!data.isEmpty()) {
                            DataPacket currDataPacket = data.peek();

                            if (currDataPacket.getBlockNum() == ((ACKPacket) message).getBlockNum() + 1) {
                                if (errors.isEmpty()) {
                                    connections.send(connectionId, data.poll());
                                }
                            } else // block number in ACK is invalid
                                errors.add(packets.new ERRORPacket((short) 0, "Illegal ACK number"));
                        }
                    }
                    break;
                case 6: // DIRQ
                    if (!isOnline)
                        connections.send(connectionId, packets.new ERRORPacket(((short) 6), " "));
                    else {
                        byte[] dirListBytes = getFilesList().getBytes();
                        createData();
                        if (dirListBytes.length == 0) {
                            connections.send(connectionId, packets.new DataPacket((short) 0, (short) 1, new byte[0]));
                            break;
                        }
                        double numOfPackets = Math.ceil(((double) dirListBytes.length) / 512);
                        for (int j = 0; j < numOfPackets; j++) {
                            if (j < numOfPackets - 1) {
                                byte[] currBlock = Arrays.copyOfRange(dirListBytes, j, j + 512);
                                data.add(packets.new DataPacket((short) 512, (short) (j + 1), currBlock));
                            } else {// last block
                                byte[] currBlock = Arrays.copyOfRange(dirListBytes, j, j + (dirListBytes.length % 512));
                                data.add(packets.new DataPacket((short) (dirListBytes.length % 512), (short) (j + 1),
                                        currBlock));
                            }
                        }
                        connections.send(connectionId, data.poll());
                    }

                    break;
                case 7: // LOGRQ
                    System.out.println("login the client");
                    LOGRQ(((LOGRQPacket) message).getUsername());
                    break;
                case 8: // DELRQ
                    if (!isOnline)
                        connections.send(connectionId, packets.new ERRORPacket(((short) 6), " "));
                    else {
                        deleteFile(((DELQPacket) message).getFilename());
                    }

                    break;
                case 10: // DISC
                    if (!isOnline)
                        connections.send(connectionId, packets.new ERRORPacket(((short) 6), " "));
                    else {
                        if (errors.isEmpty()) {
                            connections.send(connectionId, packets.new ACKPacket((short) 0));
                            connections.disconnect(connectionId);
                            terminate = true;
                            isOnline = false;
                        }
                        activeUsers.remove(connectionId);
                    }
                    break;
                default:
                    errors.add(packets.new ERRORPacket((short) 4, "Illegal TFTP operation - Unknown Opcode"));
            }

        }
        if (!errors.isEmpty()) {
            ERRORPacket minError = errors.remove(0);
            while (!errors.isEmpty()) {
                ERRORPacket currPacket = errors.remove(0);
                if (minError.getErrorCode() > currPacket.getErrorCode())
                    minError = currPacket;
            }
            connections.send(connectionId, minError); // send to client the lowest indexed error
        }

    }

    @Override
    public boolean shouldTerminate() {
        return terminate;
    }

    public String getFilesList() {
        File files = new File("Files");
        File[] listings = files.listFiles();
        String res = "";
        for (int i = 0; i < listings.length; i++)
            res = res + listings[i].getName() + '\0';
        return res;
    }

    public void LOGRQ(String name) {
        String logged = activeUsers.putIfAbsent(connectionId, name);
        if (logged != null)
            errors.add(packets.new ERRORPacket((short) 7, "User already logged in - Login username already connected"));
        else {
            connections.send(connectionId, packets.new ACKPacket((short) 0));
            isOnline = true;
        }
    }

    public void splitFile(String path) throws IOException {
        Path pathObj = Paths.get(path);
        int counter = 0;
        int sizeCounter = 0;
        int blockNum = 1;
        Vector<Byte> vec = new Vector<>();
        byte[] fileBytes = Files.readAllBytes(pathObj);
        while (counter < fileBytes.length) {
            while ((sizeCounter < 512) && fileBytes.length > counter) {
                vec.add(fileBytes[counter]);
                sizeCounter++;
                counter++;
            }
            if (sizeCounter % 512 == 0) {
                data.add(packets.new DataPacket((short) 512, (short) blockNum++, vectorToArray(vec)));
                vec.clear();
            }
            sizeCounter = 0;
        }
        data.add(packets.new DataPacket((short) vec.size(), (short) blockNum++, vectorToArray(vec)));
    }

    private byte[] vectorToArray(Vector<Byte> vec) {
        byte[] bytesArr = new byte[vec.size()];
        for (int i = 0; i < bytesArr.length; i++)
            bytesArr[i] = vec.remove(0);
        return bytesArr;
    }

    public int getErrorCode(byte[] bytes) {
        int erroCode = (int) ((bytes[2] & 0xff) << 8);
        erroCode += (int) (bytes[3] & 0xff);
        return erroCode;
    }

    public int getPacketSize(byte[] bytes) {
        int packetSize = (int) ((bytes[2] & 0xff) << 8);
        packetSize += (int) (bytes[3] & 0xff);
        return packetSize;

    }

    public int getBlockNumber(byte[] bytes) {
        int blockNumber = (int) ((bytes[4] & 0xff) << 8);
        blockNumber += (int) (bytes[5] & 0xff);
        return blockNumber;

    }

    public int getBlockNumberOp4(byte[] bytes) {
        int blockNumber = (int) ((bytes[2] & 0xff) << 8);
        blockNumber += (int) (bytes[3] & 0xff);
        return blockNumber;

    }

    public byte[] getData(byte[] bytes) {
        byte[] dataArray = Arrays.copyOfRange(bytes, 6, bytes.length);
        return dataArray;

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

    public static short bytesToShort(byte[] byteArr) {
        short result = (short) ((byteArr[0] & 0xff) << 8);
        result += (short) (byteArr[1] & 0xff);
        return result;
    }

    public void broadcast(packets msg) {
        for (Integer user : activeUsers.keySet()) {
            connections.send(user, msg);
        }
    }

    public void downloadFile(String fileName) throws IOException {
        File file = new File("Files/" + fileName);
        if (!file.exists())
            errors.add(packets.new ERRORPacket((short) 1, "File not found - RRQ of non-existing file"));
        else {
            if (errors.isEmpty()) {
                createData();
                splitFile("Files/" + fileName);
                connections.send(connectionId, data.poll()); // Send the first data packet
            }
        }
    }

    public void createData() {
        data = new ConcurrentLinkedQueue<DataPacket>();
    }

    public void deleteFile(String fileName) {
        File currFile = new File("Files/" + fileName);
        if (currFile.exists()) {
            if (errors.isEmpty()) {
                if (currFile.delete()) {// delete was successful
                    connections.send(connectionId, packets.new ACKPacket((short) 0));
                    broadcast(packets.new BCASTPacket((byte) 0, fileName));
                } else {
                    errors.add(
                            packets.new ERRORPacket((short) 2,
                                    "Access violation - File cannot be written, read or deleted."));
                }
            } else { // there are errors
                if (!currFile.canWrite())
                    errors.add(
                            packets.new ERRORPacket((short) 2,
                                    "Access violation - File cannot be written, read or deleted."));
            }
        } else {
            errors.add(packets.new ERRORPacket((short) 1, "File not found - DELRQ of non-existing file"));
        }
    }

}
