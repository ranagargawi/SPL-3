package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Server;

public class TftpServer {

  public static void main(String[] args) {

    // if (args[0].length() < 1) {
    // System.out.println("illegal port");
    // } else {
    System.out.println("start client ");
    Server.threadPerClient(
        7777, // port
        () -> new TftpProtocol(), // protocol factory
        TftpEncoderDecoder::new // message encoder decoder factory
    ).serve();
  }
  // }
}
