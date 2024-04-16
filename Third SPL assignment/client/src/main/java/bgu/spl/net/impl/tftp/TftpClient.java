package bgu.spl.net.impl.tftp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import bgu.spl.net.impl.tftp.packets.ACKPacket;
import bgu.spl.net.impl.tftp.packets.BCASTPacket;
import bgu.spl.net.impl.tftp.packets.DATAPacket;
import bgu.spl.net.impl.tftp.packets.DELERQPacket;
import bgu.spl.net.impl.tftp.packets.DIRQPacket;
import bgu.spl.net.impl.tftp.packets.DISCPacket;
import bgu.spl.net.impl.tftp.packets.LOGRQPakcet;
import bgu.spl.net.impl.tftp.packets.RRQPacket;
import bgu.spl.net.impl.tftp.packets.ErrorPacket;

public class TftpClient {

    protected boolean askedForFile = false;
    protected boolean dirc = false;
    private boolean disc = false;
    private boolean WriteReq = false;
    private boolean notInterupted = true;
    private String fileName = "";
    private int wrqLength = 0;
    private byte[] wrqbuffer = new byte[0];
    private Vector<Character> dataFile = new Vector<>();
    private packets packets;

    public packets getPacket(String line) {
        if (line.equals("DIRQ")) {
            dirc = true;
            return packets.new DIRQPacket((short) 6);
        } else if (line.startsWith("Error")) {
            return packets.new ErrorPacket((short) 5, Short.parseShort(line.substring(6)), "Error");
        } else if (line.equals("DISC")) {
            disc = true;
            return packets.new DISCPacket((short) 10);
        } else if (line.startsWith("ACK<")) {
            return packets.new ACKPacket((short) 4, Short.parseShort(line.substring(4)));
        } else if (line.startsWith("LOGRQ<") && line.endsWith(">")) {
            return packets.new LOGRQPakcet((short) 7, line.substring(6, line.length() - 1));
        } else if (line.startsWith("WRQ")) {
            WriteReq = true;
            if (line.length() == 3 || line.length() == 4 || line.charAt(3) != ' ') {
                return null; // Invalid request
            }

            String fileN = line.substring(4);
            try {
                FileInputStream is = new FileInputStream(fileN);
                wrqLength = is.available();
                if (wrqLength == -1) {
                    return null; // Error reading file
                }
                byte[] buffer = new byte[wrqLength];
                is.read(buffer);
                wrqbuffer = buffer;
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return packets.new RRQPacket((short) 2, fileName);
        } else if (line.startsWith("RRQ"))

        {
            WriteReq = true;
            fileName = line.length() == 3 ? "" : line.substring(4);
            return packets.new RRQPacket((short) 1, fileName);
        } else if (line.startsWith("DELRQ")) {
            return packets.new DELERQPacket((short) 8, line.substring(6));
        } else {
            return null; // Invalid request
        }
    }

    public void process(packets packet, ConnectionHandler connectionHandler) {

        short opcode = packet.getOpCode();
        switch (opcode) {
            case 3:// Data packet..
                processOP3(packet, connectionHandler);
                break;
            case 4:
                processOP4(packet, connectionHandler);
                break;
            case 5:
                processOP5(packet, connectionHandler);
                break;
            case 9:
                processOP9(packet, connectionHandler);
                break;
        }
    }

    public void processOP3(packets packet, ConnectionHandler connectionHandler) {
        if (getWriteRequest()) {
            byte[] ret = ((DATAPacket) packet).getCurrentDataBlockSize();
            int size = ret.length;
            if (((DATAPacket) packet).getPacketSize() != 1
                    && ((DATAPacket) packet).getCurrentDataBlockSize() != null) {
                byte[] tempArray = ((DATAPacket) packet).getCurrentDataBlockSize();
                ret = combine(ret, tempArray);
                size += ((DATAPacket) packet).getPacketSize();
            }
            if ((size < 512 && ((DATAPacket) packet).getPacketSize() <= 512) ||
                    (size > 512 && ((DATAPacket) packet).getPacketSize() > 0
                            && ((DATAPacket) packet).getPacketSize() < 512)
                    ||
                    ((DATAPacket) packet).getPacketSize() < 512) {
                try {
                    FileOutputStream output = new FileOutputStream(fileName);
                    output.write(ret);
                    // output.close();
                    fileName = "";
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        }
        if (dirc) {
            byte[] enteringData = ((DATAPacket) packet).getCurrentDataBlockSize();
            Vector<Character> incomingData = new Vector<>();
            if (!dataFile.isEmpty()) {
                for (Character b : incomingData) {
                    incomingData.add((char) b);
                }
                dataFile.addAll(incomingData);
            } else {
                for (byte b : enteringData) {
                    dataFile.add((char) b);
                }
            }
            StringBuilder s = new StringBuilder();
            if ((dataFile.size() < 512 && incomingData.size() <= 512) ||
                    (dataFile.size() > 512 && incomingData.size() > 0 && incomingData.size() < 512)) {
                for (Character b : dataFile) {
                    if (b != '0') {
                        s.append(b);
                    } else {
                        System.out.println(s);
                        s.setLength(0);
                    }
                }
                dirc = false;
                dataFile.clear();
            }
        }
        ACKPacket ackp = packet.new ACKPacket((short) 4, ((DATAPacket) packet).getNumOfBlock());
        connectionHandler.sendPacket(ackp.getBytes());

    }

    public void processOP4(packets packet, ConnectionHandler connectionHandler) {
        short blockNumber = ((ACKPacket) packet).getBlockNumber();
        System.out.println("ACK " + blockNumber);
        if (disc)
            notInterupted = false;

        if (WriteReq) {
            if (wrqLength < 512) {
                WriteReq = false;
                DATAPacket dataPack = packets.new DATAPacket((short) 3, (short) wrqLength, (short) (blockNumber + 1),
                        wrqbuffer);
                connectionHandler.sendPacket(dataPack.getBytes());
                wrqLength = 0;
                wrqbuffer = new byte[0];
            } else {
                byte[] part = getPart(wrqbuffer, 0, 513);
                byte[] tempArr = getPart(wrqbuffer, 513, wrqLength);
                wrqbuffer = tempArr;
                wrqLength -= 512;
                DATAPacket dataPacket = packets.new DATAPacket((short) 3, (short) 512, (short) (blockNumber + 1), part);
                connectionHandler.sendPacket(dataPacket.getBytes());
            }
        }
    }

    private byte[] getPart(byte[] arr, int from, int to) {
        int size = to - from;
        byte[] ans = new byte[size];
        System.arraycopy(arr, from, ans, 0, size);
        return ans;
    }

    public void processOP5(packets packet, ConnectionHandler connectionHandler) {
        short code = ((ErrorPacket) packet).getErrorCode();
        System.out.println("Error " + code);
        if (code == 1) {
            askedForFile = false;
        }
        if (code == 5) {
            WriteReq = false;
        }
        if (code == 3) {
            WriteReq = false;
        }
    }

    public void processOP9(packets packet, ConnectionHandler connectionHandler) {
        char deletedOrAdded = (char) ((BCASTPacket) packet).getDeletedOrAdded();
        String name = ((BCASTPacket) packet).getFileName();
        short binary = (short) (deletedOrAdded & 0xff);
        if (binary == 1) {// added file
            System.out.println("Bcast " + "add " + name);
        } else if (binary == 0) {// deleted file
            System.out.println("Bcast " + "del " + name);
        }
    }

    public boolean getNotInterupted() {
        return notInterupted;
    }

    public boolean getWriteRequest() {
        return WriteReq;
    }

    public boolean getDirc() {
        return dirc;
    }

    private byte[] combine(byte[] a, byte[] b) {
        byte[] ans = new byte[a.length + b.length];
        System.arraycopy(a, 0, ans, 0, a.length);
        System.arraycopy(b, 0, ans, a.length, b.length);
        return ans;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Main host port\n\n");
            System.exit(-1);
        }
        System.out.println("kjd");
        String host = "localhost";
        short port = Short.parseShort("7777");
        ConnectionHandler connectionHandler = new ConnectionHandler(host, port);
        TftpClient currClient = new TftpClient();
        if (!connectionHandler.connect() && true) {
            System.err.println("Cannot connect to " + host + ":" + port);
            System.exit(1);
        } else {
            System.out.println("this client connected succesfully to server");
        }

        ListeningThread keyBoard = new ListeningThread(connectionHandler, currClient);
        Ask serverThread = new Ask(connectionHandler, currClient);
        while (currClient.getNotInterupted()) {
            Thread thread1 = new Thread(keyBoard);
            Thread thread2 = new Thread(serverThread);
            thread1.start();
            thread2.start();
            try {
                thread1.join();
                thread2.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}
