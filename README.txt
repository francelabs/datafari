-------------------------------------------------- DATAFARI V. 1.0 --------------------------------------------------

Datafari is the perfect product for anyone who needs to search within its corporate big data,
based on the most advanced open source technologies.
Datafari combines both the Apache ManifoldCF and Solr products, and proposes to its users to search into file shares,
cloud shares (dropbox, google drive), databases, but also emails and many more sources. 
Available as community and enterprise edition, Datafari is different from the competition : 
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it,
you just need to mention that you are using it. 
- It combines two renowned Apache projects, namely ManifoldCF and Solr, which gives Datafari a long term vision.

Pre-Requirements:

- Windows Environment 64 bits or Debian Environment 64 bits
- Processor : 1GHZ and RAM : 2GB
- Ports 8080 and 5432 are opened
- Debian environment : requires curl

How to install and  start Datafari :

You can build the Windows installer with the ant script Datafari/windows/installer/build.xml or Datafari/debian7/build.xml 
for the Debian installer. You can download both installers from www.datafari.com.

Start Datafari server with start-datafari.bat. Always stop it with stop-datafari.bat. You don't have to (but you can) use the init-datafari.bat
script if you installed Datafari with the installer.

- Search UI :
http://localhost:8080/Datafari/

- Admin UI :
http://localhost:8080/Datafari/admin

You can find video tutorials on how to install and start Datafari from the installer :
- Debian : https://www.youtube.com/watch?v=cekFICeTTTs
- Windows : https://www.youtube.com/watch?v=BB95WFtL7n4

If you want to use the jcifs connector in ManifoldCF, download  jcifs-1.3.xx.jar from http://jcifs.samba.org/src/ to DATAFARI_SOURCE_DIR\mcf\mcf_home\connector-lib-proprietary

You have to configure your Repository connector and job to add documents to Datafari.
You can find a video tutorial on how to index local file share here :
https://www.youtube.com/watch?v=w0FtsvZO9SI
You can find documentation on how to create connectors and jobs here : 
http://manifoldcf.apache.org/release/trunk/en_US/end-user-documentation.html


Enjoy :-)
