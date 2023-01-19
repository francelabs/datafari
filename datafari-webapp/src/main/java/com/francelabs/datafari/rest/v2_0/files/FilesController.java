package com.francelabs.datafari.rest.v2_0.files;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FilesController {

  @GetMapping("/rest/v2.0/files/universalconnector")
  public ResponseEntity<List<FileInfo>> getUploadedFiles() {
    try {
      final IStorageService storageService = StorageServiceFactory.getUniversalConnectorStorageService();
      final List<FileInfo> filesInfo = storageService.getAllFilesInfo();
      return ResponseEntity.status(HttpStatus.OK).body(filesInfo);
    } catch (final Exception e) {
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ArrayList<FileInfo>());
    }
  }

  @PostMapping("/rest/v2.0/upload/universalconnector")
  public ResponseEntity<String> uploadFiles(@RequestParam("files") final MultipartFile[] files) {
    String message = "";
    String additionnalInfos = "";
    final List<String> fileNames = new ArrayList<>();

    try {
      final IStorageService storageService = StorageServiceFactory.getUniversalConnectorStorageService();
      for (final MultipartFile file : Arrays.asList(files)) {
        try {
          storageService.save(file);
          fileNames.add(file.getOriginalFilename());
        } catch (final Exception e) {
          if (additionnalInfos.isEmpty()) {
            additionnalInfos = "The following files could not be uploaded:" + System.lineSeparator();
          }
          additionnalInfos += file.getOriginalFilename() + " ; Reason: " + e.getMessage() + System.lineSeparator();
        }
      }
    } catch (final Exception e) {
      message = "Fail to upload files: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
    }

    message = "The following files have been successfully uploaded: " + fileNames;
    if (!additionnalInfos.isEmpty()) {
      message += System.lineSeparator() + additionnalInfos;
    }
    return ResponseEntity.status(HttpStatus.OK).body(message);
  }

  @DeleteMapping("/rest/v2.0/delete/universalconnector/{fileNames}")
  public ResponseEntity<String> deleteFiles(@PathVariable("fileNames") final String[] fileNames) {
    String message = "";
    String additionnalInfos = "";
    final List<String> filesToDelete = Arrays.asList(fileNames);
    final List<String> filesDeleted = new ArrayList<>();
    try {
      final IStorageService storageService = StorageServiceFactory.getUniversalConnectorStorageService();
      for (final String fileName : filesToDelete) {
        try {
          storageService.delete(fileName);
          filesDeleted.add(fileName);
        } catch (final Exception e) {
          if (additionnalInfos.isEmpty()) {
            additionnalInfos = "The following files could not be deleted:" + System.lineSeparator();
          }
          additionnalInfos += fileName + " ; Reason: " + e.getMessage();
        }
      }
    } catch (final Exception e) {
      message = "Fail to delete files: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
    }

    message = "AThe following files have been successfully deleted: " + filesDeleted;
    if (!additionnalInfos.isEmpty()) {
      message += System.lineSeparator() + additionnalInfos;
    }
    return ResponseEntity.status(HttpStatus.OK).body(message);
  }
}
