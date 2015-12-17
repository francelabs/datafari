--------------------------- DATAFARI V. 2.1 ------------------------

NOTE: For the major changes compared to DATAFARI V1.x, please check at the bottom of this page.

Datafari is the perfect product for anyone who needs to search within its corporate big data, based on the most advanced open source technologies.
Datafari 2.1 combines the Apache Solr, Cassandra and ManifoldCF products. It allows its users to search into file shares, cloud shares (dropbox, google drive), databases, but also emails and many more sources. 

Available as community and enterprise edition, Datafari is different from the competition : 
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it,
you just need to mention that you are using it. 
- It combines three renowned Apache projects, namely Cassandra, Solr and ManifoldCF, which gives Datafari a long term vision.

Pre-Requirements:

- Debian Environment 64 bits (a Docker image is available if you are on Windows environment)
- Processor : 1GHZ and RAM : 2GB
- Ports 8080 and 5432 are opened
- Debian environment : requires curl, debconf, unzip

How to install and start Datafari :

You can build the Debian installer with the ant script Datafari/debian7/build.xml. You can download Debian installer and Docker image from www.datafari.com.

1) Install Datafari :
# dpkg -i datafari.deb
2) Start Datafari : 
# cd /opt/datafari/bin
# bash start-datafari.sh
3) Stop Datafari :
# cd /opt/datafari/bin
# bash stop-datafari.sh

- Search UI :
http://localhost:8080/Datafari/

- Admin UI :
http://localhost:8080/Datafari/admin

You can find video tutorials on how to install and start Datafari from the installer :
- Debian : https://www.youtube.com/watch?v=cekFICeTTTs


If you want to use the jcifs connector in ManifoldCF, download  jcifs-1.3.xx.jar from http://jcifs.samba.org/src/ to DATAFARI_SOURCE_DIR\mcf\mcf_home\connector-lib-proprietary
Then edit the file Datafari/mcf/mcf_home/connectors.xml and uncomment the line :
 <!--repositoryconnector name="Windows shares" class="org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector"/-->
 And restart Datafari

You have to configure your Repository connector and job to add documents to Datafari.
You can find a video tutorial on how to index local file share here :
https://www.youtube.com/watch?v=w0FtsvZO9SI
You can find documentation on how to create connectors and jobs here : 
http://manifoldcf.apache.org/release/trunk/en_US/end-user-documentation.html

Major changes compared to v1.0
- Integration of Apache Cassandra
- Proper user management including an admin UI
- Complete overhaul of the admin UI, using the great Devoops v2 template.
- Complete overhaul of the Ajaxfrancelabs search UI, with new widgets and a cool responsive design
- Migration to Apache Solr 5
- Admin UI to configure connection to an Active Directory
- Admin UI to manage promolinks
- Admin UI to boost Solr fields at search time
- Admin UI to configure the autocomplete
- Admin UI to configure the synonyms

Enjoy :-)