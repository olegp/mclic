
/**
 * This class is used to describe a recyclable data buffer which
 * can expand on demand.
 * 
 * @author Oleg Podsechin
 * @version 1.0
 */
public class DataBuffer
{
  final static int DEFAULT_BUFFER_SIZE = 512;
  byte[] buffer = null;

  /**
   * The actual size of the DataBuffer (must be less than or equal to
   * buffer.length). This value is used for keeping track of the buffer
   * size when writing to the DataBuffer.
   */
  int size = 0;

  /**
   * The position we are currently reading the data from.
   */
  int readPosition;

  /**
   * Creates a new buffer with a buffer of default size (4096 bytes).
   */
  public DataBuffer() {
    buffer = new byte[DEFAULT_BUFFER_SIZE];
  }

  /**
   * Creates a buffer of size initialsize.
   * @param initialsize the initial size of the internal buffer
   */
  public DataBuffer(int initialsize) {
    buffer = new byte[initialsize];
  }

  public DataBuffer(byte[] initialBuffer)
  {
	  buffer = initialBuffer;
	  size = initialBuffer.length;
  }
  /**
   * Expands the internal buffer, if necessary. 
   * This does not update the size variable.
   * 
   * @param size the new size of the buffer
   */
  public void expandBuffer(int size) {
    if (size > 0) {
      if (size > buffer.length) {
        int newsize = buffer.length;
        while (size > newsize)
          newsize *= 2;
        
        byte[] newBuffer = new byte[newsize];
        if(this.size > 0)
        	System.arraycopy(buffer, 0, newBuffer, 0, this.size);
        buffer = newBuffer;
      }
      //this.size = size;
    }
  } 

  /**
   * Returns the number of bytes at the beginning of the internal buffer
   * that are guaranteed to contain valid data.
   * @return the specified size of this DataBuffer
   */
  public int getSize() {
    return size;
  }

  /**
   * Returns the actual size of the internal buffer.
   * @return the size of the internal buffer
   */
  public int getActualSize() {
    return buffer.length;
  }

  /**
   * returns number of bytes available for reading on the buffer
   * @return
   */
  public int available()
  {
	  return size-readPosition;
  }
  
  /**
   * Returns the byte buffer contained in this DataBuffer object. Please
   * note that the size of the buffer (i.e. buffer.length) is not guaranteed
   * to equal the size of the buffer and the rest of the array may therefore
   * contain undefined data.
   * @return the byte buffer contained in this object
   */
  public byte[] getBuffer() {
    return buffer;
  }
  
  public byte[] getTrimmedBuffer()
  {
	  byte[] out = new byte[size];
	  System.arraycopy(buffer, 0, out, 0, size);
	  return out;
  }

  /**
   * Resets the DataBuffer by setting size to 0.
   */
  public void startWriting() {
    size = 0;
  }

  public void write(byte[] value)
  {
	  write(value, 0, value.length);
  }

  public void write(byte[] value, int offset, int length)
  {
	  if(size+length > buffer.length)
		  expandBuffer(size+length);

	  System.arraycopy(value, offset, buffer, size, length);
	  size += length;
  }
  /**
   * Writes a byte to the DataBuffer.
   * @param value byte
   */
  public void writeByte(byte value) {
    if(size + 1 > buffer.length)
      expandBuffer(size + 1);

    buffer[size ++] = value;
  }

  /**
   * Writes a short to the DataBuffer.
   * @param value short
   */
  public void writeShort(short value) {
    if(size + 2 > buffer.length)
      expandBuffer(size + 2);

    buffer[size ++] = (byte)((value >> 8) & 0xff);
    buffer[size ++] = (byte)(value & 0xff);
  }

  /**
   * Writes an int to the DataBuffer.
   * @param value int
   */
  public void writeInt(int value) {
    if(size + 4 > buffer.length)
      expandBuffer(size + 4);

    buffer[size ++] = (byte)((value >> 24) & 0xff);
    buffer[size ++] = (byte)((value >> 16) & 0xff);
    buffer[size ++] = (byte)((value >> 8) & 0xff);
    buffer[size ++] = (byte)(value & 0xff);
  }

  /**
   * Writes a String to the DataBuffer.
   * @param value String
   */
  public void writeString(String value) {
    if(value != null) {
      byte[] bytes = value.getBytes();
      writeInt(bytes.length);
      if (size + bytes.length> buffer.length)
    	  expandBuffer(size + bytes.length );
      else	
    	  size+=bytes.length;
      System.arraycopy(bytes, 0, buffer, size-bytes.length, bytes.length);
      
    } else
      writeInt(0);
  }
  
  public void writeByteArray(byte[] value)
  {				
	writeInt(value.length);        
	write(value);		
  }
  
  public byte[] readByteArray()
  {
	int length = readInt();
	byte[]  bytes = new byte[length];   
    read(bytes);    
    return bytes;
  }
  
  /**
   * Resets the DataBuffer by setting readPosition to 0.
   */
  public void startReading() {
    readPosition = 0;
  }

  public void read(byte[] sink)
  {
	  read(sink, 0, sink.length);
  }
  
  public void read(byte[] sink, int offset, int length)
  {
	  System.arraycopy(buffer, readPosition, sink, offset, length);
	  readPosition += length;
  }

  /**
   * Reads a byte from the DataBuffer.
   * @return byte
   */
  public byte readByte() {
    return buffer[readPosition ++];
  }

  /**
   * Reads a short from the DataBuffer.
   * @return short
   */
  public short readShort() {
    short ret = 0;
    ret |= (buffer[readPosition++] & 0xff) << 8;
    ret |= (buffer[readPosition++] & 0xff);
    return ret;
  }

  /**
   * Reads an int from the DataBuffer.
   * @return int
   */
  public int readInt() {
    int ret = 0;
    ret |= (buffer[readPosition++] & 0xff) << 24;
    ret |= (buffer[readPosition++] & 0xff) << 16;
    ret |= (buffer[readPosition++] & 0xff) << 8;
    ret |= (buffer[readPosition++] & 0xff);
    return ret;
  }

  /**
   * Reads in a String from a DataBuffer. Returns null for String of length 0.
   * @return String
   */
  public String readString() {
    int size = readInt();
    if(size > 0) {
      String ret = new String(buffer, readPosition, size);
      readPosition += size;
      return ret;
    } else
      return null;
  }

}