package ubc.cs317.rtsp.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RTPpacket {

  // size of the RTP header:
  static int HEADER_SIZE = 12;

  // Fields that compose the RTP header
  int version, padding, extension, cc, marker;
  int payloadType, sequenceNumber, timestamp, ssrc;

  // Bitstream of the RTP header
  public byte[] header;

  // Fill in the constructor
  public RTPpacket(int ptype, int seqnum, byte[] data, int data_length) {
    // last two will be null and 0 for now.

    // fill by default header fields:
    version = 2;
    padding = 0;
    extension = 0;
    cc = 0;
    marker = 0;
    ssrc = 0;

    // fill changing header fields:

    // Build the header bitstream, and fill it with RTP header fields.
    header = new byte[HEADER_SIZE];

    // Zero out all fields.
    for (int i = 0; i < HEADER_SIZE; i++) {
      header[i] = 0;
    }

    // Fill in header fields here.
    header[0] = (byte) (version << 6);
    header[1] = (byte) ((marker << 7) | payloadType);
    header[2] = (byte) (sequenceNumber >> 8);
    header[3] = (byte) (sequenceNumber & 0xff);
    header[4] = (byte) (timestamp >> 24);
    header[5] = (byte) (timestamp >> 16);
    header[6] = (byte) (timestamp >> 8);
    header[7] = (byte) timestamp;

    printHeader();
  }

  /**
   * Print packet header, without the CC field.
   */
  public void printHeader() {
    System.out.println("Header for the RTP Packet " + sequenceNumber);
    for (int i = 0; i < (HEADER_SIZE - 4); i++) {
      for (int j = 7; j >= 0; j--) {
        if (((i << j) & header[i]) != 0) {
          System.out.print("1");
          ;
        } else {
          System.out.print("0");
        }
        System.out.print(" ");
      }
    }
  }

  public static void main(String args[]) throws Exception {
    int port = Integer.parseInt(args[0]);
    int remoteport = Integer.parseInt(args[1]);

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    InetAddress localhost = InetAddress.getLocalHost();
    DatagramSocket clientSocket = new DatagramSocket(port, localhost);

    clientSocket.connect(localhost, remoteport);

    byte[] outData = new byte[1024];

    // Construct
    // RTPpacket newPacket = new RTPpacket(26, 52, 183082, null, 0);
    // outData = newPacket.header;
    // DatagramPacket outPacket = new DatagramPacket(outData, outData.length);
    // clientSocket.send(outPacket);
    // clientSocket.close();

  }
}
