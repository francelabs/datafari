/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.francelabs.datafari.connectors.share;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;

/**
 * This class contains test code that is useful for performing test operations
 * using JCifs from the appliance. Basic operations are: addDocument,
 * deleteDocument, addFolderUser, and deleteFolderUser.
 */
public class SharedDriveHelpers {
  public static final String _rcsid = "@(#)$Id: SharedDriveHelpers.java 988245 2010-08-23 18:39:35Z kwright $";

  private CIFSContext ctx;
  private SmbFile smbconnection;

  /**
   * Construct the helper and initialize the connection.
   * 
   * @param serverName
   *          is the DNS name of the server.
   * @param userName
   *          is the name to use to log in.
   * @param password
   *          is the password.
   */
  public SharedDriveHelpers(final String serverName, final String userName, final String password) throws ManifoldCFException {
    try {
      // make the smb connection to the server
      // use NtlmPasswordAuthentication so that we can reuse credential for DFS
      // support
      final SingletonContext sc = SingletonContext.getInstance();
      ctx = sc.withCredentials(new NtlmPasswordAuthenticator(userName, password));
      smbconnection = new SmbFile("smb://" + serverName + "/", ctx);
    } catch (final MalformedURLException e) {
      throw new ManifoldCFException("Unable to access SMB/CIFS share: " + serverName, e, ManifoldCFException.SETUP_ERROR);
    }
  }

  /**
   * Close the connection.
   */
  public void close() throws ManifoldCFException {
    // Just let stuff go
    ctx = null;
    smbconnection = null;
  }

  /**
   * See if a document exists.
   * 
   * @param targetPath
   *          is the document's path, beginning with the share name and
   *          separated by "/" characters.
   * @return the target path if the document is found, or "" if it is not.
   */
  public String lookupDocument(final String targetPath) throws ManifoldCFException {
    try {
      final String identifier = mapToIdentifier(targetPath);
      final SmbFile file = new SmbFile(identifier, ctx);
      if (file.exists())
        return targetPath;
      return "";
    } catch (final IOException e) {
      throw new ManifoldCFException("IO exception: " + e.getMessage(), e);
    }
  }

  /**
   * Add a document.
   * 
   * @param targetPath
   *          is the target path, beginning with the share name and separated by
   *          "/" characters.
   * @param sourceFile
   *          is the local source file name to copy to the target.
   * @return the target path.
   */
  public String addDocument(final String targetPath, final String sourceFile) throws ManifoldCFException {
    try {
      final String identifier = mapToIdentifier(targetPath);
      SmbFile file = new SmbFile(identifier, ctx);
      // Open source file for read
      final InputStream is = new FileInputStream(sourceFile);
      try {
        // Open smbfile for write
        if (!file.exists()) {
          file.createNewFile();
          file = new SmbFile(identifier, ctx);
        }
        final OutputStream os = file.getOutputStream();
        try {
          final byte[] bytes = new byte[65536];
          while (true) {
            final int amt = is.read(bytes, 0, bytes.length);
            if (amt == -1)
              break;
            if (amt > 0)
              os.write(bytes, 0, amt);
          }
        } finally {
          os.close();
        }
      } finally {
        is.close();
      }
      return targetPath;
    } catch (final IOException e) {
      throw new ManifoldCFException("IO exception: " + e.getMessage(), e);
    }
  }

  /**
   * Delete a document.
   * 
   * @param targetPath
   *          is the file path to delete, beginning with the share name and
   *          separated by "/" characters.
   */
  public void deleteDocument(final String targetPath) throws ManifoldCFException {
    try {
      final String identifier = mapToIdentifier(targetPath);
      final SmbFile file = new SmbFile(identifier, ctx);
      file.delete();
    } catch (final IOException e) {
      throw new ManifoldCFException("IO exception: " + e.getMessage(), e);
    }
  }

  /**
   * Add user ACL to folder.
   * 
   * @param targetPath
   *          is the folder path to add the acl to, beginning with the share
   *          name and separated by "/" characters.
   * @param userName
   *          is the user to add.
   */
  public void addUserToFolder(final String targetPath, final String userName) throws ManifoldCFException {
    // MHL
  }

  /**
   * Remove user ACL from folder.
   * 
   * @param targetPath
   *          is the folder path to add the acl to, beginning with the share
   *          name and separated by "/" characters.
   * @param userName
   *          is the user to remove.
   */
  public void removeUserFromFolder(final String targetPath, final String userName) throws ManifoldCFException {
    // MHL
  }

  /**
   * Map a "path" specification to a full identifier.
   */
  protected String mapToIdentifier(final String path) throws IOException {
    final String smburi = smbconnection.getCanonicalPath();
    final String uri = smburi + path + "/";
    return new SmbFile(uri, ctx).getCanonicalPath();
  }

}
