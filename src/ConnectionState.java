

public class ConnectionState implements Runnable {

  public final static long CONNECTION_TIMEOUT = 16000;

  // these are the default values, if the Debug property is set to true in the jad,
  // localhost and port 8080 are used instead
  public static String SERVER_NAME = "62.164.255.237"; //"127.0.0.1"; //
  public static int SERVER_PORT = 80;

  public static String SERVER_URI = "ImageServer/upload";

  public final static byte PROTOCOL_VERSION_MAJOR = 0;
  public final static byte PROTOCOL_VERSION_MINOR = 1;

  public final static short PROTOCOL_VERSION = (PROTOCOL_VERSION_MAJOR << 8) | PROTOCOL_VERSION_MINOR;

  //*** Packet types
  //*** Generic packets
  public final static byte PACKET_ERROR = 0;

  public final static byte PACKET_LOGIN = 1;
  public final static byte PACKET_LOGIN_RESPONSE = 2;

  //*** Error codes
  // generic errors
  public final static byte ERROR_NONE = -1;
  public final static byte ERROR_UNKNOWN = 0;
  public final static byte ERROR_OLD_CLIENT = 1;
  public final static byte ERROR_FALSE_CREDENTIALS = 2; // also sent when registering and the username is taken
  public final static byte ERROR_UNRECOGNIZED_REQUEST = 3;
  public final static byte ERROR_UNEXPECTED_REQUEST = 4;
  public final static byte ERROR_UNEXPECTED_EOS = 5;

  //*** States
  public final static int STATE_CLOSED = 0;
  public final static int STATE_CONNECTING = 1;
  public final static int STATE_REQUEST_PREPARED = 2;
  public final static int STATE_REQUEST_SENT = 3;
  public final static int STATE_RESPONSE_RECEIVED = 4;
  public final static int STATE_PACKET_RECEIVED = 5;
  public final static int STATE_ERROR = 6;

  //** Constants
  public static DataBuffer dataBuffer = new DataBuffer();
  public static Connection connection;

  public static ConnectionState connectionState;
  public static Thread connectionThread;

  public static byte currentPacket = -1;

  public static int currentState;
  public static long connectionStartTime;

  public static int errorCode = ERROR_NONE;
  public static String errorMessage;

  public static boolean isRunning;

  public static void init() {
    connection = new Connection(SERVER_NAME, SERVER_PORT, false, SERVER_URI);
    connectionState = new ConnectionState(); // used for the run method

    isRunning = true;
    connectionThread = new Thread(connectionState);
    connectionThread.start();
  }

  public static void writePacketHeader(byte packetType) {
    dataBuffer.startWriting();
    dataBuffer.writeShort(PROTOCOL_VERSION);
    dataBuffer.writeByte(packetType);
  }

  /**
   * Checks the dataBuffer to see if it contains the expected packet header.
   * @param expectedPacket byte
   * @throws Exception
   */
  public static void checkPacketHeader(byte expectedPacket) throws Exception {
    dataBuffer.startReading();

    short protocolVersion = dataBuffer.readShort();
    byte packetType = dataBuffer.readByte();

    if (protocolVersion > PROTOCOL_VERSION) {
      errorCode = ERROR_OLD_CLIENT;
      throw new Exception(App.textStrings[App.STRING_ERROR_OLD_CLIENT]);
    }

    if (packetType == PACKET_ERROR) {
      errorCode = dataBuffer.readByte();
      if(errorCode >= 0 && errorCode < 10) // only 9 recognized errors
        throw new Exception(App.textStrings[App.STRING_ERROR_UNKNOWN + errorCode]);
      else
        throw new Exception(App.textStrings[App.STRING_ERROR_UNKNOWN] + ": " + errorCode);
    }

    if (packetType != expectedPacket) {
      throw new Exception(App.textStrings[App.STRING_ERROR_UNRECOGNIZED_RESPONSE]);
    }
  }

  public static void doLogin() throws Exception {

	//byte[] data = DisplayCanvas.getInstance().imageBytes;
	
	// expand buffer in advance
	//dataBuffer.expandBuffer(3 + 4 + data.length); 
	
    writePacketHeader(PACKET_LOGIN);
    //dataBuffer.writeInt(data.length);
    //dataBuffer.write(data);
    dataBuffer.writeString(App.codeString);
    
    //System.out.println(dataBuffer.getSize());

    connection.send(dataBuffer, dataBuffer);

    // throws an exception if it fails
    checkPacketHeader(PACKET_LOGIN_RESPONSE);

    // read the rest of the contents
    String serverResponse = dataBuffer.readString();
    App.getInstance().displayCanvasBack();
    App.getInstance().showAlert("Received", serverResponse);		
  }


  public synchronized int consume() {
    while (currentPacket == -1) {
      try {
          wait();
      } catch (InterruptedException e) {
      }
    }
    int packet = currentPacket;
    currentPacket = -1;
    notifyAll();
    return packet;
  }


  /**
   * run
   */
  public void run() {

    while(isRunning) {
      int packet = consume();
      currentState = STATE_CONNECTING;
      connectionStartTime = System.currentTimeMillis();

      try {
        switch (packet) {
          case PACKET_LOGIN:
            doLogin();
            break;
        }

        currentState = STATE_CLOSED;
      }
      catch (Exception e) {
        //e.printStackTrace();
        // if we haven't been forcefully terminated, display an error message
        if (currentState != STATE_CLOSED) {
          currentState = STATE_ERROR;
          errorMessage = e.getMessage() != null ? e.getMessage() :
        	  App.textStrings[App.STRING_ERROR_CONNECT_FAILED];

          // app specific code
          App.getInstance().showAlert("Connection Error", errorMessage);
        }
      }
    }
  }

  public static void close() {
	  if(connection != null) {
	    currentState = STATE_CLOSED;
	    connection.close();
	    errorMessage = null;
	  }
  }

  /**
   * Call this function periodically from another Thread to
   * allow the connection to timeout independently of the network.
   */
  public static void updateTimeout() {
    if(currentState != STATE_CLOSED) {
      long currentTime = System.currentTimeMillis();
      if (currentTime - connectionStartTime > CONNECTION_TIMEOUT) {
        currentState = STATE_ERROR;
        errorMessage = App.textStrings[App.STRING_ERROR_CONNECTION_TIMED_OUT];
        connection.close();
      }

    }

  }

  public synchronized void produce(byte packet) {
    currentPacket = packet;
    connectionState.notifyAll();
   }

   public static void sendPacket(byte packet) {
     connectionState.produce(packet);
   }

   public static void destroy() {
     isRunning = false;
     connection.close();
     connectionState.notifyAll();
   }

}