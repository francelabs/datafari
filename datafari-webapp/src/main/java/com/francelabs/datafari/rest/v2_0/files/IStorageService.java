package com.francelabs.datafari.rest.v2_0.files;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {

  public void init() throws Exception;

  /**
   * Get the file info of all stored files
   *
   * @return the list of file info for all files
   * @throws Exception
   */
  public List<FileInfo> getAllFilesInfo() throws Exception;

  public void save(MultipartFile file) throws Exception;

  public Resource load(String filename) throws Exception;

  public void deleteAll() throws Exception;

  public void delete(String filename) throws Exception;

}
