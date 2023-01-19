package com.francelabs.datafari.rest.v2_0.files;

import com.francelabs.datafari.utils.UniversalConnectorConfiguration;

public class StorageServiceFactory {

  /**
   * Get the storage service used for the Universal Connector feature
   *
   * @return the ALREADY INITIALIZED storage service used for the Universal Connector feature
   * @throws Exception
   */
  public static IStorageService getUniversalConnectorStorageService() throws Exception {
    final String storageFolderPath = UniversalConnectorConfiguration.getInstance().getProperty(UniversalConnectorConfiguration.STORAGE_FOLDER_PATH);
    return getFileSystemStorageService(storageFolderPath);
  }

  /**
   * Get a file system storage service
   *
   * @param storageFolderPath the absolute path to the storage folder on the file system
   * @return An ALREADY INITIALIZED file system storage service
   * @throws Exception
   */
  public static IStorageService getFileSystemStorageService(final String storageFolderPath) throws Exception {
    final FileSystemStorageService storageService = new FileSystemStorageService(storageFolderPath);
    storageService.init();
    return storageService;
  }

}
