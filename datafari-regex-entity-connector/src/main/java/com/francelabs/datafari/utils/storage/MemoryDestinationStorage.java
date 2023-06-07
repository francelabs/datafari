package com.francelabs.datafari.utils.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

/**
 * An in-memory stream
 *
 */
public class MemoryDestinationStorage extends DestinationStorage {
  private final ByteArrayOutputStream outputStream;
  

  public MemoryDestinationStorage(final int sizeHint) {
    outputStream = new ByteArrayOutputStream(sizeHint);
  }


  @Override
  public OutputStream getOutputStream() throws ManifoldCFException {
    return outputStream;
  }

  @Override
  public long getBinaryLength() throws ManifoldCFException {
    return outputStream.size();
  }

  @Override
  public InputStream getInputStream() throws ManifoldCFException {
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  @Override
  public void close() throws ManifoldCFException {
  }
  
}
