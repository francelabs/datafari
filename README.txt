--------------------------- DATAFARI 6.0-DEV ------------------------

Datafari is an open source enterprise search solution. It is the perfect product for anyone who needs to search and analyze its corporate data and documents, both within the content and the metadata.

Available as community (open source) and enterprise (proprietary) edition, Datafari is different from the competition :
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it, you just need to mention that you are using it.
- It combines three renowned Apache projects, namely Cassandra, Solr and ManifoldCF, which gives Datafari a long term vision.

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

NOTE 1: Java 11 is REQUIRED
NOTE 2: to crawl files, you need to install an extra jar that we cannot distribute for open source licence conflict rationales (Apachev2 and LGPL). Please follow this documentation: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/662700036/Add+the+JCIFS-NG+Connector+to+Datafari+-+Community+Edition

* Major new features compared to v5.5 :
New regex transformation connector, for simple declaration of entities extracted based on regular expressions
New CSV connector, to index each live of a CSV as a separate line in the index
Reduced hardware requirements by removing Apache Zeppelin for the dashboards
Atomic updates to remove the need to reindex the full data of a document when only some fields have changed
Capacity to ingest and index content hosted on Apache Solr 9.x
Graphical admin to boost relevancy based on a field values

* Major components versions
- Solr 9.5
- ManifoldCF 2.26
- Tomcat 9.0.81
- Cassandra 4.1.3
- PostgreSQL 15.4
- Zookeeper 3.9.1
- Tika Server 2.9.1
- DatafariUI 1.4
- AdminUI 1.0

The complete list of changes and bugfixes can be found in CHANGES.TXT

Enjoy 🙂
