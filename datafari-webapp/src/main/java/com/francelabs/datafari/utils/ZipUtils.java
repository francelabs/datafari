package com.francelabs.datafari.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

  private static void zipFile(final File fileToZip, final String filename, final ZipOutputStream zos) throws IOException {
    if (fileToZip.isHidden()) {
      return;
    }
    if (fileToZip.isDirectory()) {
      if (filename.endsWith("/")) {
        zos.putNextEntry(new ZipEntry(filename));
        zos.closeEntry();
      } else {
        zos.putNextEntry(new ZipEntry(filename + "/"));
        zos.closeEntry();
      }
      final File[] children = fileToZip.listFiles();
      for (final File childFile : children) {
        zipFile(childFile, filename + "/" + childFile.getName(), zos);
      }
      return;
    }
    final FileInputStream fis = new FileInputStream(fileToZip);
    final ZipEntry ze = new ZipEntry(filename);
    zos.putNextEntry(ze);
    final byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
      zos.write(bytes, 0, length);
    }
    fis.close();
  }

  public static void zipFiles(final File[] filesToZip, final String zipFilePath) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(zipFilePath); ZipOutputStream zos = new ZipOutputStream(fos);) {
      for (final File fileToZip : filesToZip) {
        zipFile(fileToZip, fileToZip.getName(), zos);
      }
    }
  }

}
