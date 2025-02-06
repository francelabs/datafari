package com.francelabs.datafari.transformation.llm.utils.storage;

import com.francelabs.datafari.transformation.llm.utils.storage.DestinationStorage;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
