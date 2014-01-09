-------------------------------------------------- DATAFARI V. 0.6.9.1-ALPHA --------------------------------------------------

Datafari is the perfect product for anyone who needs to search within its corporate big data,
based on the most advanced open source technologies.
Datafari combines both the Apache ManifoldCF and Solr products, and proposes to its users to search into file shares,
cloud shares (dropbox, google drive…), databases, but also emails and many more sources. 
Available as community and enterprise edition, Datafari is different from the competition : 
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it,
you just need to mention that you are using it. 
- It combines two renowned Apache projects, namely ManifoldCF and Solr, which gives Datafari a long term vision.

How to install Datafari :

+ Pre-Requirements:

- An empty remote machine that will run Datafari (recommended configuration : Processor : 1GHZ and RAM : 2GB)
- A development machine with Eclipse and Ant installed


This folder (DATAFARI_SOURCE_DIR) is an Eclipse project. You can import it directly in the IDE of the development machine.
It contains the source code of Datafari, binaries and specific configurations for Solr and Manifold CF.
It also contains an ant script that enables the compilation of Datafari and deployment on a remote Debian server though pscp.

+ Install Dafari in 5 steps:

1) Install a Debian 7 OS on the empty remote machine

2) Copy jcifs-1.3.xx.jar from http://jcifs.samba.org/src/ to DATAFARI_SOURCE_DIR\ManifoldCF\ManifoldCFHome\connector-lib-proprietary

3) Modify buildAndDeployLinux script in DATAFARI_SOURCE_DIR : specify the DATAFARI_HOST in ftp-server and root password in ftp-password

4) Run the ant script buildAndDeployLinux

5) Run datafari (first run can be quite long) :
service datafari start

You can now access to :

- Search UI :
http://DATAFARI_HOST:8080/Datafari/

- Manifold cf (Crawler) admin :
http://DATAFAR_HOST:8080/datafari-mcf-crawler-ui

- Solr (Search Engine) admin  :
http://DATAFARI_HOST:8080/datafari-solr


You have to configure your Repository connector and job to add documents to Datafari
You can find documentation on how to create connectors and jobs here : 
http://manifoldcf.apache.org/release/trunk/en_US/end-user-documentation.html

Enjoy :-)
