package com.francelabs.datafari.utils.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

/**
 * A temporary file to store data.
 *
 */
public class FileDestinationStorage extends DestinationStorage {
  protected final File outputFile;
  protected final OutputStream outputStream;

  
  public FileDestinationStorage(Class<?> classUsing) throws ManifoldCFException {
    File outputFile;
    OutputStream outputStream;
    
    String prefix;
    if (classUsing != null) {
      prefix = classUsing.getSimpleName();
    } else {
      prefix = FileDestinationStorage.class.getSimpleName();
    }
    
    try {
      outputFile = File.createTempFile(prefix, "tmp");
      outputStream = new FileOutputStream(outputFile);
    } catch (final IOException e) {
      handleIOException(e);
      outputFile = null;
      outputStream = null;
    }
    this.outputFile = outputFile;
    this.outputStream = outputStream;
  }

  @Override
  public OutputStream getOutputStream() throws ManifoldCFException {
    return outputStream;
  }

  @Override
  public long getBinaryLength() throws ManifoldCFException {
    return outputFile.length();
  }

  @Override
  public InputStream getInputStream() throws ManifoldCFException {
    try {
      return new FileInputStream(outputFile);
    } catch (final IOException e) {
      handleIOException(e);
      return null;
    }
  }

  @Override
  public void close() throws ManifoldCFException {
    outputFile.delete();
  }

  private int handleIOException(final IOException e) throws ManifoldCFException {
    // IOException reading from our local storage...
    if (e instanceof InterruptedIOException) {
      throw new ManifoldCFException(e.getMessage(), e, ManifoldCFException.INTERRUPTED);
    }
    throw new ManifoldCFException(e.getMessage(), e);
  }

}
