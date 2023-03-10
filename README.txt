--------------------------- DATAFARI 6.0-DEV ------------------------

Datafari is an open source enterprise search solution. It is the perfect product for anyone who needs to search and analyze its corporate data and documents, both within the content and the metadata.

Available as community (open source) and enterprise (proprietary) edition, Datafari is different from the competition :
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it, you just need to mention that you are using it.
- It combines three renowned Apache projects, namely Cassandra, Solr and ManifoldCF, which gives Datafari a long term vision.
- It leverages Apache Zeppelin to care about the analytics dashboards (search behavior and system monitoring).

The complete documentation (for users, admins and developers) is available here : https://datafari.atlassian.net/wiki/display/DATAFARI/Datafari

I. HOW TO INSTALL DATAFARI :

i. DOCKER:
The easiest is probably to use our docker container: https://hub.docker.com/r/datafari/datafari
To avoid most crashes, be sure to assign enough resources to your container:
- 2+ GHz Quad Core recommended
-- Without Zeppelin - no OCR: Min 12GB, recommended 16 GB
- At least 20GB on an SSD (recommended)

ii. OVA:
If you don't fancy docker, you can opt for a Virtual Machine, which is fully preconfigured: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/66125825/Quick+Start+Guide
To avoid most crashes, be sure to assign enough resources to your container:
- 2+ GHz Quad Core recommended
- 16GB RAM without Zeppelin, 24 GB with Zeppelin recommended
- At least 20GB on an SSD (recommended)

iii. For any other installation modes, refer to: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/66125825/Quick+Start+Guide

II. HOW TO PLAY WITH DATAFARI :

i. Search UI :
https://localhost/datafariui/

ii. Admin UI :
https://localhost/Datafari/admin/?lang=en

NOTE: to crawl files, you need to install an extra jar that we cannot distribute for open source licence conflict rationales (Apachev2 and LGPL). Please follow this documentation: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/662700036/Add+the+JCIFS-NG+Connector+to+Datafari+-+Community+Edition
NOTE BIS: Java 11 is now REQUIRED

* New features compared to v5.3
- OCR and Spacy options in connectors simplified mode
- New admin UI to deploy a Tika Server
- New duplicate detector functionality
- Various bugfixes

* Major components versions
- Solr 8.11.2
- ManifoldCF 2.24
- Tomcat 9.0.56
- Cassandra 4.0.1
- PostgreSQL 12.4
- Zookeeper 3.6.2
- Tika Server 2.7.0
- Zeppelin 0.10.1

The complete list of changes and bugfixes can be found in CHANGES.TXT

Enjoy 🙂
