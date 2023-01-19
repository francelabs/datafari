package com.francelabs.datafari.rest.v2_0.files;

public class FileInfo {

  private final String name;
  private final String path;
  private final long bytes;

  public FileInfo(final String fileName, final String filePath, final long fileSize) {
    name = fileName;
    path = filePath;
    bytes = fileSize;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  /**
   * Return the file size in bytes
   *
   * @return file size in bytes
   */
  public long getSize() {
    return bytes;
  }

}
