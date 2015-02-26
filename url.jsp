<!-- File Display/Download -->
<!-- Written by Rick Garcia -->
<!-- -->
<!-- Licensed under the Apache License, Version 2.0 (the "License");                                    -->
<!-- you may not use this file except in compliance with the License.                                   -->
<!-- You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0                 -->
<!-- Unless required by applicable law or agreed to in writing, software                                -->
<!-- distributed under the License is distributed on an "AS IS" BASIS,                                  -->
<!-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.                           -->
<!-- See the License for the specific language governing permissions and limitations under the License. -->
<!-- -->
<!-- This file replaces the url.jsp located in /tomcat/webapps/Datafari/url.jsp -->

<html>
<head>
<meta HTTP-EQUIV="Pragma" CONTENT="no-cache" />
</head>
<body>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="javax.servlet.*" %>
<%
	String surl = request.getParameter("url");
	String fileNameA[] = surl.split(":");
	String fileName=fileNameA[1];
	String fileType = fileName.substring(fileName.indexOf(".")+1,fileName.length());
 
    int BUFSIZE = 4096;
    String filePath;
    
    filePath = fileName;
    
        File file = new File(filePath);
        int length   = 0;
        ServletOutputStream outStream = response.getOutputStream();
        ServletContext context  = getServletConfig().getServletContext();
        String mimetype = context.getMimeType(filePath);
        
        // sets response content type
        if (mimetype == null) {
            mimetype = "application/octet-stream";
        }
        response.setContentType(mimetype);
        response.setContentLength((int)file.length());
        
        // sets HTTP header
        response.setHeader("Content-Disposition", "inline; fileName=\"" + fileName + "\"");
        
        byte[] byteBuffer = new byte[BUFSIZE];
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        
        // reads the file's bytes and writes them to the response stream
        while ((in != null) && ((length = in.read(byteBuffer)) != -1))
        {
            outStream.write(byteBuffer,0,length);
        }
        
        in.close();
        outStream.close();
%>
</body>
</html>
