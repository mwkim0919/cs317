/*
 * University of British Columbia
 * Department of Computer Science
 * CPSC317 - Internet Programming
 * Assignment 2
 * 
 * Author: Jonatan Schroeder
 * January 2013
 * 
 * This code may not be used without written consent of the authors, except for 
 * current and future projects and assignments of the CPSC317 course at UBC.
 */

package ubc.cs317.rtsp.client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import ubc.cs317.rtsp.client.exception.RTSPException;
import ubc.cs317.rtsp.client.model.Frame;
import ubc.cs317.rtsp.client.model.Session;

/**
 * This class represents a connection with an RTSP server.
 */
public class RTSPConnection {

  private static final int BUFFER_LENGTH = 15000;
  private static final long MINIMUM_DELAY_READ_PACKETS_MS = 20;
  /**
   * Minimum time the rtpPlayer is playing the frames.
   */
  private static final long MIN_PLAY_FRAME_MS = 10;
  /**
   * About of time that the player will wait for the frames to be in the buffer.
   */
  private static final long INITIAL_BUFFER_MS = 2000;

  private Session session;
  /**
   * Refactored the name from rtpTimer to rtpFrameReceiverTask.
   */
  private Timer rtpFrameReceiveTimer;
  /**
   * create rtpFrameReceiveTimerTask for gracefully canceling TimerTask Thread.
   */
  private TimerTask rtpFrameReceiveTimerTask;
  /**
   * RTP Player Timer.
   */
  private Timer rtpPlayTimer;
  /**
   * RTP Player TimerTask.
   */
  private TimerTask rtpPlayTimerTask;
  /**
   * Queue buffer for the frames.
   */
  private Queue<Frame> rtpBuffer;

  /**
   * RTP connection socket.
   */
  private DatagramSocket datagramSocket;
  private DatagramPacket datagramPacket;
  /**
   * TCP Connection socket.
   */
  private Socket socket;
  /**
   * Write for TCP Connection
   */
  private PrintWriter writer;
  /**
   * Reader for TCP Connection
   */
  private BufferedReader reader;
  /**
   * Port number for RTP Socekt that the client is listening to.
   */
  private int clientPort = 3089;
  /**
   * Command Sequence number.
   */
  private int cSeq = 1;
  /**
   * Name of the video that we are currently playing.
   */
  private String videoName;
  /**
   * Current Command Session number.
   */
  private String rtpSession;
  /**
   * State whether the RTP player is open.
   */
  private boolean isOpen = false;
  /**
   * State whether the RTP is playing.
   */
  private boolean isPlayed = false;

  /**
   * Start in Millisecond for RTSP Player Timer.
   */
  private long startMillis;
  
  /**
   * Start in Millisecond for pause.
   */
  private long startPause = 0;
  
  /**
   * Establishes a new connection with an RTSP server. No message is sent at this point, and no stream is set
   * up.
   * 
   * @param session
   *          The Session object to be used for connectivity with the UI.
   * @param server
   *          The hostname or IP address of the server.
   * @param port
   *          The TCP port number where the server is listening to.
   * @throws RTSPException
   *           If the connection couldn't be accepted, such as if the host name or port number are invalid or
   *           there is no connectivity.
   */
  public RTSPConnection(Session session, String server, int port) throws RTSPException {

    try {
      this.session = session;
      // Create TCP connection between Client and the Server.
      socket = new Socket(server, port);
      // Create TCP Writer, so we can send server input.
      writer = new PrintWriter(socket.getOutputStream());
      // Create TCP Reader, so we can read server input
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      // Create RTP datagram Socket.
      datagramSocket = new DatagramSocket(clientPort, InetAddress.getByName(server));
      
      byte[] buffer = new byte[BUFFER_LENGTH];
      datagramPacket = new DatagramPacket(buffer, 0, buffer.length);
      
      // RTP Frame buffer with minimum size of 1000 Frames.
      rtpBuffer = new PriorityQueue<Frame>(1000, new FrameSeqNumComparator());

    } catch (SocketException e) {
      throw new RTSPException("SocketException", e);
    } catch (UnknownHostException e) {
      throw new RTSPException("UnknownHostException", e);
    } catch (IOException e) {
      throw new RTSPException("IOException", e);
    }
  }

  /**
   * Sends a SETUP request to the server. This method is responsible for sending the SETUP request, receiving
   * the response and retrieving the session identification to be used in future messages. It is also
   * responsible for establishing an RTP datagram socket to be used for data transmission by the server. The
   * datagram socket should be created with a random UDP port number, and the port number used in that
   * connection has to be sent to the RTSP server for setup. This datagram socket should also be defined to
   * timeout after 1 second if no packet is received.
   * 
   * @param videoName
   *          The name of the video to be setup.
   * @throws RTSPException
   *           If there was an error sending or receiving the RTSP data, or if the RTP socket could not be
   *           created, or if the server did not return a successful response.
   * 
   */
  public synchronized void setup(String videoName) throws RTSPException {
    // If a video is not open
    if (isOpen == false) {
      try {
        // Save video name for future use.
        this.videoName = videoName;
        // Create SETUP and send to the server.
        sendServerInput("SETUP " + videoName + " RTSP/1.0\nCSeq: " + cSeq
            + "\nTransport: RTP/UDP; client_port=" + clientPort + "\n\n");
        RTSPResponse response = RTSPResponse.readRTSPResponse(reader);
        // if the response of the server is not 200 then throw a RTSPExcpetion.
        if (response.getResponseCode() != 200) {
          throw new RTSPException("response was not successful");
        }
        // Save the SESSION value.
        rtpSession = response.getHeaderValue("SESSION");
        
        // now video is open
        isOpen = true;
      } catch (IOException e) {
        throw new RTSPException("IOException", e);
      }
    } else {
      throw new RTSPException("A video is already open. Close first before you want to open.");
    }
  }

  /**
   * Sends a PLAY request to the server. This method is responsible for sending the request, receiving the
   * response and, in case of a successful response, starting the RTP timer responsible for receiving RTP
   * packets with frames.
   * 
   * @throws RTSPException
   *           If there was an error sending or receiving the RTSP data, or if the server did not return a
   *           successful response.
   */
  public synchronized void play() throws RTSPException {
    try {
      // If a video is open
      if (isOpen == true) {

        // If a video is not playing
        if (isPlayed == false) {
          // Create PLAY command and send to the server.
          sendServerInput("PLAY " + videoName + " RTSP/1.0\nCSeq: " + cSeq + "\nSession: " + rtpSession
              + "\n\n");
          RTSPResponse response = RTSPResponse.readRTSPResponse(reader);
          // if the response of the server is not 200 then throw a RTSPExcpetion.
          if (response.getResponseCode() != 200) {
            throw new RTSPException("response was not successful");
          }
          // Save the SESSION value.
          rtpSession = response.getHeaderValue("SESSION");
          // Start Frame Receiver.
          startRTPFrameReceiver();
          // Start the RTP Player.
          startRTPPlayer();
          // now a video is playing.
          isPlayed = true;
          // If a video is already playing, let the user know with a message.
        } else {
          throw new RTSPException("Video is already playing");
        }
        // if a video is not open, let the user know with a message.
      } else {
        throw new RTSPException("Video is not open");
      }
    } catch (IOException e) {
      throw new RTSPException("IOException", e);
    }
  }

  /**
   * Starts a timer that reads RTP packets repeatedly. The timer will wait at least
   * MINIMUM_DELAY_READ_PACKETS_MS after receiving a packet to read the next one.
   */
  private void startRTPFrameReceiver() {
    rtpFrameReceiveTimer = new Timer();
    rtpFrameReceiveTimerTask = new TimerTask() {
      @Override
      public void run() {
        receiveRTPPacket();
      }
    };
    rtpFrameReceiveTimer.schedule(rtpFrameReceiveTimerTask, 0, MINIMUM_DELAY_READ_PACKETS_MS);
  }

  /**
   * Starts a timer that plays RTP Packet from the buffer. This also wait for INITIAL_BUFFER_MS to let the
   * buffer fill up and play the frames. The timer will wait at least MIN_PLAY_FRAME_MS after receiving a
   * packet to read the next one.
   */
  private void startRTPPlayer() {
    rtpPlayTimer = new Timer();
    rtpPlayTimerTask = new RTPPlayTimerTask();
    rtpPlayTimer.scheduleAtFixedRate(rtpPlayTimerTask, INITIAL_BUFFER_MS, MIN_PLAY_FRAME_MS);
    if (startMillis == 0) {
      startMillis = System.currentTimeMillis() + INITIAL_BUFFER_MS;
    }
  }

  /**
   * Receives a single RTP packet and processes the corresponding frame. The data received from the datagram
   * socket is assumed to be no larger than BUFFER_LENGTH bytes. This data is then parsed into a Frame object
   * (using the parseRTPPacket method) and the method session.processReceivedFrame is called with the
   * resulting packet. In case of timeout no exception should be thrown and no frame should be processed.
   */
  private void receiveRTPPacket() {
    try {
      // Receive datagramPacket from DatagramSocket.
      datagramSocket.receive(datagramPacket);
      // Parse the RTP Packet.
      Frame frame = parseRTPPacket(datagramPacket.getData(), datagramPacket.getLength());
      // enqueue the rtpBuffer.
      rtpBuffer.offer(frame);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Sends a PAUSE request to the server. This method is responsible for sending the request, receiving the
   * response and, in case of a successful response, cancelling the RTP timer responsible for receiving RTP
   * packets with frames.
   * 
   * @throws RTSPException
   *           If there was an error sending or receiving the RTSP data, or if the server did not return a
   *           successful response.
   */
  public synchronized void pause() throws RTSPException {
    try {
      // if a video is open
      if (isOpen == true) {

        // if a video is playing
        if (isPlayed == true) {

          sendServerInput("PAUSE " + videoName + " RTSP/1.0\nCSeq: " + cSeq + "\nSession: " + rtpSession
              + "\n\n");
          RTSPResponse response = RTSPResponse.readRTSPResponse(reader);
          if (response.getResponseCode() != 200) {
            throw new RTSPException("response was not successful");
          }
          rtpSession = response.getHeaderValue("SESSION");
          ((RTPPlayTimerTask) rtpPlayTimerTask).pause();
          cancelAllTimers();
          
          // now a video is paused.
          isPlayed = false;
        } else {
          throw new RTSPException("Video is already paused");
        }
        // If a video is not opened, let the user know with a message.
      } else {
        throw new RTSPException("Video is not opened");
      }
    } catch (IOException e) {
      throw new RTSPException("IOException", e);
    }
  }

  /**
   * Sends a TEARDOWN request to the server. This method is responsible for sending the request, receiving the
   * response and, in case of a successful response, closing the RTP socket. This method does not close the
   * RTSP connection, and a further SETUP in the same connection should be accepted. Also this method can be
   * called both for a paused and for a playing stream, so the timer responsible for receiving RTP packets
   * will also be cancelled.
   * 
   * @throws RTSPException
   *           If there was an error sending or receiving the RTSP data, or if the server did not return a
   *           successful response.
   */
  public synchronized void teardown() throws RTSPException {
    try {
      if (isOpen == true) {
        sendServerInput("TEARDOWN " + videoName + " RTSP/1.0\nCSeq: " + cSeq + "\nSession: " + rtpSession
            + "\n\n");
        RTSPResponse response = RTSPResponse.readRTSPResponse(reader);
        if (response.getResponseCode() != 200) {
          throw new RTSPException("response was not successful");
        }
        rtpSession = response.getHeaderValue("SESSION");
        rtpBuffer.clear();
        // Reset Start Millis.
        startMillis = 0;
        cancelAllTimers();
        
        // Video is closed.
        isOpen = false;
        // Video is not played.
        isPlayed = false;
      } else {
        throw new RTSPException("Video already closed");
      }
    } catch (IOException e) {
      throw new RTSPException("IOException", e);
    }
  }

  /**
   * Closes the connection with the RTSP server. This method should also close any open resource associated to
   * this connection, such as the RTP connection, if it is still open.
   */
  public synchronized void closeConnection() {
    try {
      rtpBuffer.clear();
      // Reset Start Millis.
      startMillis = 0;
      cancelAllTimers();
      datagramSocket.close();
      socket.close();

      // Video is closed.
      isOpen = false;
      // Video is not played.
      isPlayed = false;
    } catch (IOException e) {
      System.out.println("IOException");
      e.printStackTrace();
    }
  }

  /**
   * Parses an RTP packet into a Frame object.
   * 
   * @param packet
   *          the byte representation of a frame, corresponding to the RTP packet.
   * @return A Frame object.
   */
  private static Frame parseRTPPacket(byte[] packet, int length) {
    // From 9 - 15 bits
    byte payloadType = (byte) ((byte) packet[1] & 0x7F);

    // Only 8th bit
    boolean marker = false;
    if ((packet[1] >> 7) == 1) {
      marker = true;
    }

    // From [16-31] bits. This required for signed bits to change it to be unsigned bits.
    short sequenceNumber = (short) ((short) (packet[2] & 0xff) << 8 | (short) (packet[3] & 0xff));
    // From [32-63] bits. This required for signed bits to change it to be unsigned bits.
    int timeStamp = (int) ((int) (packet[4] & 0xff) << 24 | (int) (packet[5] & 0xff) << 16
        | (int) (packet[6] & 0xff) << 8 | (int) (packet[7] & 0xff));

    // Read SSRC Not really needed in thise case.
    int ssrc = (((packet[8] & 0xff) << 24) | ((packet[9] & 0xff) << 16) | ((packet[10] & 0xff) << 8) | (packet[11] & 0xff));

    // This is just a value to indicate where the data of the RTP packet starts.
    int headerByteSize = 12;
    // Create the frame.
    Frame frame = new Frame(payloadType, marker, sequenceNumber, timeStamp, packet, headerByteSize,
        packet.length - headerByteSize);
    return frame;
  }

  /**
   * Method that helps to send the String Command.
   */
  private void sendServerInput(String cmd) {
    if (socket != null) {
      writer.print(cmd);
      writer.flush();
      cSeq++;
    }
  }

  /**
   * Cancel all Timers and its repective TimerTasks RTPFrameReceiver RTPFrameReceiverTask RTPPlayer
   * RTPPlayerTask.
   */
  private void cancelAllTimers() {
    if (rtpFrameReceiveTimer != null)
      rtpFrameReceiveTimer.cancel();
    if (rtpFrameReceiveTimerTask != null)
      rtpFrameReceiveTimerTask.cancel();
    if (rtpPlayTimer != null)
      rtpPlayTimer.cancel();
    if (rtpPlayTimer != null)
      rtpPlayTimerTask.cancel();
    

  }

  /**
   * A inner class that extends TimerTask and that takes out a frame one by one from a priority queue that has
   * stored frames received from the RTSP server.
   */
  private class RTPPlayTimerTask extends TimerTask {
    /**
     * Previous Sequence Number.
     */
    private short previousSeqNum = 0;
    /**
     * Grace period that the client let the frame play. In addition to the frame timestamp, this considers both upper bound (Timestamp + GRACE_PRIOD_TO_PLAY)
     * and lower bound (Timestamp - GRACE_PRIOD_TO_PLAY) as a valid time to play.
     */
    private static final int GRACE_PRIOD_TO_PLAY = 5;
    /**
     * Exponential Back off value.
     */
    private long expoBackOffTime = 500;
    /**
     * Default exponential Back off ratio.
     */
    private long expoBackOffRatio = 2;
    
    @Override
    public void run() {
      long currMillis = System.currentTimeMillis();
      if (startPause != 0) {
        startMillis = startMillis + (currMillis - startPause);
        startPause = 0;
      }
      Frame frame = rtpBuffer.peek();
      if (frame != null) {
        short seqNum = frame.getSequenceNumber();
        int timeStamp = frame.getTimestamp();
        // First check for initial frame.
        if (seqNum == 0) {
          frame = rtpBuffer.poll();
          previousSeqNum = seqNum;
          session.processReceivedFrame(frame);
          // only if the current sequence Number is greater than the previous Sequence Number.
          // (i.e check for out of order.)
        } else if (seqNum > previousSeqNum) {
          // Since MillisSeconds doesn't go up 1 at a time give some possible
          if (currMillis - startMillis + GRACE_PRIOD_TO_PLAY >= timeStamp
              && currMillis - startMillis - GRACE_PRIOD_TO_PLAY <= timeStamp) {
            frame = rtpBuffer.poll();
            previousSeqNum = seqNum;
            session.processReceivedFrame(frame);
          } else if ((currMillis - startMillis) + GRACE_PRIOD_TO_PLAY > timeStamp) {
            System.out.println("Remove Frame deltaRealTimeStamp: " + (currMillis - startMillis) + " FrameSeqNum: "
                + seqNum + " FrameTimeStamp: " + timeStamp);
            // If the Time has already pass the point where it is too late the show the Frame then just remove
            // the frame.
            rtpBuffer.remove();
          }
        } else {
          // if the seqNum is out of order then just remove the Frame.
          rtpBuffer.remove();
        }
      } else {
        // If we get null Frame which means the buffer is all used then do exponential BackOff in order to get
        // more Frames into the Queue.
        expoBackOffTime = expoBackOffTime * expoBackOffRatio;
        System.out.println("Exponential Back Off Time: " + expoBackOffTime);
        try {
          Thread.sleep(expoBackOffTime);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        // Set the exponential backoff.
        startMillis += expoBackOffTime;
      }
    }
    
    public void pause() {
      startPause = System.currentTimeMillis();
    }
    
  }

  /**
   * A inner class which compares two frame sequence numbers for determining their priorities in priority
   * queue. Smaller sequence number means having higher priority. The class implements Comparator<Frame>
   * interface.
   */
  private class FrameSeqNumComparator implements Comparator<Frame> {
    @Override
    public int compare(Frame x, Frame y) {
      if (x == null && y != null) {
        return -1;
      }
      if (x != null && y == null) {
        return 1;
      }
      if (x.getSequenceNumber() < y.getSequenceNumber()) {
        return -1;
      }
      if (x.getSequenceNumber() > y.getSequenceNumber()) {
        return 1;
      }
      return 0;
    }
  }
}
