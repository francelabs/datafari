<%@ page import="java.io.*,java.util.*, javax.servlet.*"%>
<%@ page import="javax.servlet.http.*"%>
<%@ page import="org.apache.commons.fileupload.*"%>
<%@ page import="org.apache.commons.fileupload.disk.*"%>
<%@ page import="org.apache.commons.fileupload.servlet.*"%>
<%@ page import="org.apache.commons.io.output.*"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="java.nio.charset.StandardCharsets"%>
<%@ page import="java.nio.charset.Charset"%>
<%@ page import="java.nio.file.Path"%>
<%@ page import="java.nio.file.Paths"%>
<%@ page import="java.nio.file.Files"%>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
<title>Alerts Configuration</title>
<link rel="icon" type="image/png" href="images/bullet.png">
<link rel="stylesheet" type="text/css" href="../css/main.css"
	media="screen" />
<link rel="stylesheet" type="text/css" href="../css/admin.css"
	media="screen" />
<script type="text/javascript" src="../js/jquery-1.8.1.min.js"></script>
<script type="text/javascript" src="../js/menu.js"></script>
</head>
<body class="gecko win">
	<jsp:include page="../header.jsp" />
	<jsp:include page="menu.jsp" />

	<%
if (request.getContentType()!=null) {

	ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale()); 
   File file ;
   int maxFileSize = 5000 * 1024;
   int maxMemSize = 5000 * 1024;
   ServletContext context = pageContext.getServletContext();
   String filePath = request.getParameter("file").toString();
    if(request.getParameter("content")!=null){							//Case : modification of a file
    	String content = request.getParameter("content").toString();	//Get the new content
    	try {   
    	    PrintWriter pw = new PrintWriter(new FileOutputStream(filePath));	
    	    pw.println(content);										//Replace the old content with the new one
    	    //clean up
    	    pw.close();
    	} catch(IOException e) {
    	   out.println(e.getMessage());
    	}
    	 out.println(resourceBundle.getString("alertModifSuccess"));
    }
    else{															//Case : adding of an alert
		String core = request.getParameter("core").toString();		//
		String keyword = request.getParameter("keyword").toString();//
		String mail = request.getParameter("mail").toString();   	// 
		String object = request.getParameter("object").toString();  //Get all the parameters     
		try{
            FileWriter fstream = new FileWriter(filePath, true);	//
            BufferedWriter fbw = new BufferedWriter(fstream);		//Write in the specified file (true is for append and not replace)
            fbw.write("core = "+core+";");							
            fbw.newLine();											
            fbw.write("keyword = "+keyword+";");					
            fbw.newLine();				
            fbw.write("address = "+mail+";");
            fbw.newLine();
            fbw.write("object = "+object+";");	
            fbw.newLine();											//Write all the parameters on individual lines
            fbw.close();
		}catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
		out.println(resourceBundle.getString("alertAddSuccess"));
    }
   }
   else {
		out.println("erreur");
		String site = new String("http://localhost:8080/Datafari/");
		response.setStatus(response.SC_MOVED_TEMPORARILY);
		response.setHeader("Location", site); 
		}
%>

	<jsp:include page="../footer.jsp" />
</body>
</html>

