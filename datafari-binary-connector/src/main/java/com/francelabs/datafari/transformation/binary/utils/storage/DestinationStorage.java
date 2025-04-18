package com.francelabs.datafari.transformation.binary.utils.storage;

import com.francelabs.datafari.transformation.binary.utils.storage.FileDestinationStorage;
import com.francelabs.datafari.transformation.binary.utils.storage.MemoryDestinationStorage;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A suitable storage object (File, in memory...) to write to.
 *
 */
public abstract class DestinationStorage {
  /** We handle up to 64K in memory; after that we go to disk. */
  private static final long IN_MEMORY_MAXIMUM_FILE = 65536;

  
  /**
   * Get the output stream to write to. Caller should explicitly close this stream when done writing.
   */
  public abstract OutputStream getOutputStream() throws ManifoldCFException;

  /**
   * Get new binary length.
   */
  public abstract long getBinaryLength() throws ManifoldCFException;

  /**
   * Get the input stream to read from. Caller should explicitly close this stream when done reading.
   */
  public abstract InputStream getInputStream() throws ManifoldCFException;

  /**
   * Close the object and clean up everything. This should be called when the data is no longer needed.
   */
  public abstract void close() throws ManifoldCFException;
  
  /**
   * @param binaryLength content length
   * @param classUsing class using this storage.
   * 
   * @return the created storage object
   * 
   * @throws ManifoldCFException 
   */
  public static DestinationStorage getDestinationStorage(long binaryLength, Class<?> classUsing) throws ManifoldCFException {
    DestinationStorage ds;
    if (binaryLength <= IN_MEMORY_MAXIMUM_FILE) {
      ds = new MemoryDestinationStorage((int)binaryLength);
    } else {
      ds = new FileDestinationStorage(classUsing);
    }
    return ds;
  }

}
