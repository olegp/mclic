import javax.microedition.io.*;
import java.io.*;

/**
 * This class describes an HTTP connection to a server. HTTP connections
 * are used for mobile communication because this is the only protocol
 * which is guaranteed to work on all MIDP 1.0 devices. The majority of
 * devices do not support other protocols.
 * 
 * NOTE: this file can be replaced as is with the SecureConnection (SSH)
 * Ionsquare libraries.
 *
 * @author Oleg Podsechin
 * @version 1.0
 */
public class Connection {
  /**
   * This is the IP or full address of the server to connect to.
   */
  String server;

  /**
   * This is the port to connect to. The default HTTP port is 80,
   * but most MIDP servers listen on other ports to distinguish themselves
   * from web servers.
   */
  int port;


  /**
   * The JSESSIONID.
   */
  byte[] sessionId;

  //String cookies;

  /**
   * This is the full url, containing the protocol, server address and port.
   */
  String url;

  HttpConnection connection = null;
  boolean connectionFailed = false;
  
  //int writePosition;

  /**
   * This is used to notify anyone interested, what the current state of
   * the connection is.
   */
  //ConnectionTracker tracker = null;


  /**
   * Creates a new connection to the specified server on the specified port.
   * @param server the server to connect to
   * @param port the port to connect to
   */
  public Connection(String server, int port, boolean issecure, String uri) {
    this.server = server;
    this.port = port;

    // store URL
    url = (issecure == true ? "https" : "http") + "://" + server + ":" + port + "/" + uri;
    sessionId = null;
    //cookies = null;
  }

  /**
   * Disconnects from a session.
   */
  public void removeSession() {
    this.sessionId = null;
  }

/*
  public void addSessionIdToUrl(String sessionId) {
    url += "?jsessionid=" + sessionId;
  }

  static String getCookies(HttpConnection connection) throws IOException {
    StringBuffer stringBuffer = new StringBuffer();
    int k = 0;
    while (connection.getHeaderFieldKey(k) != null) {
      String key = connection.getHeaderFieldKey(k);
      String value = connection.getHeaderField(k);
      if (key.equals("set-cookie")) {
        // Parse the header and get the cookie.
        int j = value.indexOf(";");
        String cValue = value.substring(0, j);
        stringBuffer.append(cValue);
        stringBuffer.append("; ");
      }
      k ++;
    }
    String cookies = stringBuffer.toString();
    if(cookies.length() > 0)
      return cookies;
    return null;
  }
*/

  /**
   * Sends data along to the server and returns the result.
   * @param data the data sent to the server
   * @param close set to true to send the close connection message to the server
   * @return the result returned from the server
   */
  public void send(DataBuffer write, DataBuffer read) throws Exception {
    OutputStream out = null;
    InputStream in = null;

    Exception exception = null;

    try {
      Object test = Connector.open(url, Connector.READ_WRITE);
      //if(tracker != null) tracker.connectionOpened();

      connection = (HttpConnection) test;
      connection.setRequestMethod(HttpConnection.POST);
    //  if(cookies != null)
    //     connection.setRequestProperty("cookie", cookies);
      //System.out.println("Cookies: " + cookies);

      // these are optional headers which we leave out to conserve bandwidth

      // connection.setRequestProperty("User-Agent", "Profile/MIDP-1.0 Configuration/CLDC-1.0");
      // connection.setRequestProperty("Connection", "Close");
      // connection.setRequestProperty("Content-Type", "application/octet-stream");
      
      //int sessionIdLength = sessionId == null ? 1 : (sessionId.length + 1);
      //connection.setRequestProperty("Content-Length", String.valueOf(write.getSize() + sessionIdLength));

      // we do this through the url
//      connection.setRequestProperty("Authorization",
//        "Basic " + Client.getInstance().user + ":" + Client.getInstance().password);

      ConnectionState.currentState = ConnectionState.STATE_REQUEST_PREPARED;

      if(write != null) {
        out = connection.openOutputStream();
        if(sessionId != null) {
          out.write(sessionId.length);
          out.write(sessionId);
        } else
          out.write((byte)0);

        out.write(write.getBuffer(), 0, write.getSize());
        /*
        // write out in 4KB chunks
        final int WRITE_INCREMENT = 4096;
        byte[] buffer = write.getBuffer();
        int size = write.getSize();
        
        for(writePosition = 0; writePosition < size; writePosition += WRITE_INCREMENT) {
        	int left = size - writePosition;
        	out.write(buffer, writePosition, left > WRITE_INCREMENT ? WRITE_INCREMENT : left);
        	//out.flush();
        	DisplayCanvas.getInstance().repaint();
        }
        
        writePosition = size;
        */
      }

      ConnectionState.currentState = ConnectionState.STATE_REQUEST_SENT;


      // out.flush();           // Optional, getResponseCode will flush

      int rc = connection.getResponseCode();

      ConnectionState.currentState = ConnectionState.STATE_RESPONSE_RECEIVED;

//      if(tracker != null) tracker.connectionSent();

      if (rc != HttpConnection.HTTP_OK) {
        throw new IOException("HTTP " + rc);
      }

   //   if(cookies == null) {
   //     cookies = getCookies(connection);
   //     if(cookies == null) // something is wrong
   //       throw new IOException("No cookies");
   //   }

      in = connection.openInputStream();

      // read the result in
      // unless we've just closed the connection, in which case we don't
      // expect anything back
      if(write != null) {

        // read the session id, this is sent before the main packet
        int size = in.read();
        // the id is sent only once, the subsequent responses set size to 0
        if(size > 0) {
          if (sessionId == null || size != sessionId.length) {
            sessionId = new byte[size];
            readFully(in, sessionId, 0, size);
            //addSessionIdToUrl(new String(sessionId));
          } else
            readFully(in, sessionId, 0, size);
        }

          int currentSize = 0;
          while(true) {
            currentSize +=
                readFully(in, read.getBuffer(), currentSize,
                          read.getActualSize() - currentSize);
            if(currentSize == read.getActualSize()) {
              // expand buffer
              read.expandBuffer(read.getActualSize() * 2);
              // try again
            } else {
              // we are done
              read.expandBuffer(currentSize);
              break;
            }
          }
      }
      
     // writePosition = 0;

      ConnectionState.currentState = ConnectionState.STATE_PACKET_RECEIVED;


    } catch (Exception e) {
      App.warning(e.toString());
      exception = e;

    } finally {
//      if(tracker != null) tracker.connectionClosed();

      // close all the connections
      try {
        if (in != null)
          in.close();
      }
      catch (IOException e) {
        App.severe(e.getMessage());
      }

      try {
        if (out != null)
          out.close();
      }
      catch (IOException e) {
        App.severe(e.getMessage());
      }

      try {
        if (connection != null) {
          connection.close();
          connection = null;
        }
      }
      catch (IOException e) {
        App.severe(e.getMessage());
      }
    }

    // if there's an exception to throw, throw it
    if(exception != null)
      throw exception;
  }

  /**
     * Same as the normal <tt>in.read(b, off, len)</tt>, but tries to ensure that
     * the entire len number of bytes is read.
     * <p>
     * @returns the number of bytes read, or -1 if the end of file is
     *  reached before any bytes are read
     */
    public static int readFully(InputStream in, byte[] b, int off, int len)
    throws IOException
    {
        int total = 0;
        for (;;) {
            int got = in.read(b, off + total, len - total);
            if (got < 0) {
                return (total == 0) ? -1 : total;
            } else {
                total += got;
                if (total == len)
                    return total;
            }
        }
    }


  public void close() {
    try {
      if (connection != null) {
        connection.close();
        connection = null;
      }
    }
    catch (IOException e) {
      App.severe(e.getMessage());
    }
  }
}
