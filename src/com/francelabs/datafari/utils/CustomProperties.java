package com.francelabs.datafari.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

public class CustomProperties extends Properties{
	
	private static final long serialVersionUID = 1L;
	public void store(OutputStream out)
	        throws IOException{
	        store0(new BufferedWriter(new OutputStreamWriter(out, "UTF8")));
	    }
	 private void store0(BufferedWriter bw)
		        throws IOException
		    {
		        bw.write("#" + new Date().toString());
		        bw.newLine();
		        synchronized (this) {
		            for (Enumeration e = keys(); e.hasMoreElements();) {
		                String key = (String)e.nextElement();
		                String val = (String)get(key);
		                bw.write(key + "=" + val);
		                bw.newLine();
		            }
		        }
		        bw.flush();
		    }

}
