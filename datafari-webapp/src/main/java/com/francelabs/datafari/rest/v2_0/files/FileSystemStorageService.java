package com.francelabs.datafari.rest.v2_0.files;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileSystemStorageService implements IStorageService {

  /** Path to the storage folder on the file system **/
  private final Path fsStorageFolder;

  private static final Logger logger = LogManager.getLogger(FileSystemStorageService.class.getName());

  public FileSystemStorageService(final String storageFolderPath) {
    fsStorageFolder = Paths.get(storageFolderPath);
  }

  @Override
  public void init() throws Exception {
    Files.createDirectories(fsStorageFolder);
  }

  @Override
  public void save(final MultipartFile file) throws Exception {
    try {
      Files.copy(file.getInputStream(), fsStorageFolder.resolve(file.getOriginalFilename()));
    } catch (final FileAlreadyExistsException e) {
      throw new Exception("File already exists", e);
    }
  }

  @Override
  public void deleteAll() throws Exception {
    FileSystemUtils.deleteRecursively(fsStorageFolder.toFile());
  }

  @Override
  public void delete(final String filename) throws Exception {
    Files.delete(fsStorageFolder.resolve(filename));
  }

  @Override
  public Resource load(final String filename) throws Exception {
    final Path file = fsStorageFolder.resolve(filename);
    final Resource resource = new UrlResource(file.toUri());

    if (resource.exists() || resource.isReadable()) {
      return resource;
    } else {
      throw new IOException("File " + file.toFile().getAbsolutePath() + " does not exist or could not be read !");
    }
  }

  @Override
  public List<FileInfo> getAllFilesInfo() throws Exception {
    final List<FileInfo> filesInfo = new ArrayList<>();
    Files.walk(fsStorageFolder, 1).filter(path -> !path.equals(fsStorageFolder)).forEach(filePath -> {
      try {
        final FileInfo fileInfo = new FileInfo(filePath.getFileName().toString(), filePath.toFile().getPath(), Files.size(filePath));
        filesInfo.add(fileInfo);
      } catch (final IOException e) {
        logger.error("Unable to get file size of file " + filePath.toFile().getAbsolutePath(), e);
      }
    });
    ;
    return filesInfo;
  }

}
