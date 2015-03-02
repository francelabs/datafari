package com.francelabs.datafari.utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LocalFileReader {

    private static int BUFSIZE = 4096;
    public static boolean isLocalFile(String surl){
    	return surl.startsWith("file:/");
    }
    
    
    
	public static long readFile(String surl, OutputStream outStream) throws IOException{
	    /** File Display/Download -->
	    <!-- Written by Rick Garcia -->
	    */
		String fileNameA[] = surl.split(":");
	    String filePath = fileNameA[1];
		
		File file = new File(filePath);
        int length   = 0;
        byte[] byteBuffer = new byte[BUFSIZE];
        DataInputStream in = new DataInputStream(new FileInputStream(file));
     
        // reads the file's bytes and writes them to the response stream
        while ((in != null) && ((length = in.read(byteBuffer)) != -1))
        {
            outStream.write(byteBuffer,0,length);
        }
        
        in.close();
        return file.length();
	}

}
