package com.francelabs.datafari.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

public class FileUtils {

  /**
   * Get the file content as a String
   *
   * @param file
   * @return the file content as a String
   * @throws IOException
   */
  public static String getFileContent(final File file) throws IOException {
    final Path filePath = Paths.get(file.getAbsolutePath());
    final Charset charset = StandardCharsets.UTF_8;
    return new String(Files.readAllBytes(filePath), charset);
  }

  /**
   * Save a String as a File
   *
   * @param file
   *          the file to save
   * @param fileContent
   *          the String content of the file
   * @throws Exception
   */
  public static void saveStringToFile(final File file, final String fileContent) throws Exception {
    final Path configPath = Paths.get(file.getAbsolutePath());
    Files.write(configPath, fileContent.getBytes(StandardCharsets.UTF_8));
  }

  public static void deleteFolder(final File folderToDelete) throws IOException {
    org.apache.commons.io.FileUtils.deleteDirectory(folderToDelete);
  }

  public static void deleteFile(final File fileToDelete) throws IOException {
    org.apache.commons.io.FileUtils.forceDelete(fileToDelete);
  }

  public static void cleanFilesFromFolder(final File directory, final String listFilesString) throws IOException {
    final List<File> files = (List<File>) org.apache.commons.io.FileUtils.listFiles(directory, null, true);
    final List<String> listFiles = Arrays.asList(listFilesString.split(","));
    for (final File file : files) {
      for (int i = 0; i < listFiles.size(); i++) {
        if (file.getName().equals(listFiles.get(i))) {
          deleteFile(file);
        }
      }
    }
  }

  public static void changePassApache(final String filePath, final String user, final String realm, final String pass) throws Exception {
    final File f = new File(filePath);
    final String currentFilePass = getFileContent(f);
    String newFilePass = "";
    final String[] lines = currentFilePass.split(System.getProperty("line.separator"));
    for (String s : lines) {

      if (s.startsWith(user)) {
        s = user + ":" + realm + ":" + digest(user, realm, pass);
      }

      newFilePass += s + "\n";
    }
    System.out.println(newFilePass);

    if (newFilePass != null) {
      deleteFile(f);
      saveStringToFile(f, newFilePass);
    }
  }

  public static String digest(final String user, final String realm, final String password) {
    final String stringToHash = user + ":" + realm + ":" + password;
    String hashPass = null;
    try {

      final MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(stringToHash.getBytes());
      final byte[] digest = md.digest();
      hashPass = DatatypeConverter.printHexBinary(digest);
    } catch (final Exception e) {
      e.getMessage();
      return null;
    }
    return hashPass.toLowerCase();
  }


  public static void replaceAndCopy(String sourceFile, String targetFile, String regex, String stringReplaced) throws IOException {
    try (PrintWriter pw = new PrintWriter(Paths.get(targetFile).toFile(), StandardCharsets.UTF_8)) {
      Files.readAllLines(Path.of(sourceFile), StandardCharsets.UTF_8)
      .stream()
      .map(s -> s.replaceAll(regex, stringReplaced)
          )
      .forEachOrdered(pw::println);
    }

  }
  
  public static void appendUsingFileWriter(String filePath, String text) {
    File file = new File(filePath);
    FileWriter fr = null;
    try {
      fr = new FileWriter(file, true);
      fr.write(text);

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        fr.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }



}
