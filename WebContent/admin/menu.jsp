<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
	<div id="menu">
	<ul id="sddm">
    <li><a href="#" 
        onmouseover="mopen('m1')" 
        onmouseout="mclosetime()">Search Engine</a>
        <div id="m1" 
            onmouseover="mcancelclosetime()" 
            onmouseout="mclosetime()">
        <a href="solr.jsp">Solr Admin</a>
		<a href="banana.jsp">Graphical Stats</a>
        <a href="stats.jsp">Stats</a>
		<a href="capsulesConfig.jsp">Capsules</a>
		<a href="synonymConfig.jsp">Synonyms</a>
		<a href="stopWordsConfig.jsp">StopWords</a>
		<a href="alertsConfig.jsp">Alerts</a>
        </div>
    </li>
    <li><a href="#" 
        onmouseover="mopen('m2')" 
        onmouseout="mclosetime()">Crawler</a>
        <div id="m2" 
            onmouseover="mcancelclosetime()" 
            onmouseout="mclosetime()">
        <a href="manifoldcf.jsp">ManifoldCF Admin</a>
        </div>
    </li>
    <li><a href="#" 
        onmouseover="mopen('m3')" 
        onmouseout="mclosetime()">Server</a>
        <div id="m3" 
            onmouseover="mcancelclosetime()" 
            onmouseout="mclosetime()">
        <a href="tomcat.jsp">Tomcat</a>
        </div>
    </li>
    <!-- 
    <li><a href="#">Order</a></li>
    <li><a href="#">Help</a></li>
    <li><a href="#">Contact</a></li> -->
</ul>
</div>

