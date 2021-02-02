--------------------------- DATAFARI 5.0 ------------------------

Datafari is an open source enterprise search solution. It is the perfect product for anyone who needs to search and analyze its corporate data and documents, both within the content and the metadata.

Available as community (open source) and enterprise (proprietary) edition, Datafari is different from the competition :
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it, you just need to mention that you are using it.
- It combines three renowned Apache projects, namely Cassandra, Solr and ManifoldCF, which gives Datafari a long term vision.
- It leverages open distro ELK the reference open source stack to do business intelligence on logs.

The complete documentation (for users, admins and developers) is available here : https://datafari.atlassian.net/wiki/display/DATAFARI/Datafari

I. HOW TO INSTALL DATAFARI :

i. DOCKER:
The easiest is probably to use our docker container: https://hub.docker.com/r/datafari/datafari
To avoid most crashes, be sure to assign enough resources to your container:
- 2+ GHz Quad Core recommended
  -- Without ELK - no OCR: Min 12GB, recommended 16 GB
  -- With ELK: Min 24GB, Recommended 32 GB
- At least 20GB on an SSD (recommended)

ii. OVA:
If you don't fancy docker, you can opt for a Virtual Machine, which is fully preconfigured: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/66125825/Quick+Start+Guide
To avoid most crashes, be sure to assign enough resources to your container:
- 2+ GHz Quad Core recommended
- 16GB RAM without ELK, 32 GB with ELK recommended
- At least 20GB on an SSD (recommended)

iii. For any other installation modes, refer to: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/1878753289/Install+Datafari

II. HOW TO PLAY WITH DATAFARI :

i. Search UI :
https://localhost/Datafari/

ii. Admin UI :
https://localhost/Datafari/admin

NOTE: to crawl files, you need to install an extra jar that we cannot distribute for open source licence conflict rationales (Apachev2 and LGPL). Please follow this documentation: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/662700036/Add+the+JCIFS-NG+Connector+to+Datafari+-+Community+Edition
NOTE BIS: Java 11 is now REQUIRED

* New features compared to v4.3
- New tag cloud widget
- New help page
- Adding German and Spanish as preconfigured languages
- New user menu
- API mode to directly query Datafari without the UI
- Aggregator mode to aggregate several Datafaris
- Move from the official ELK stack to the opendistro stack
- SSL/TLS activated by default between the client browser and the Datafari server
- SSL/TLS activated by default between the client browser and the opendistro component
- Simplified installation procedure
- A connector to index Solr
- Various bugfixes

* Major changes of components compared to v4.4
- Datafari requires Java 11
- Solr updated to version 8.5.2
- ELK updated to 7.8.0 (with OpenDistro 1.9.0)
- MCF updated to 2.18
- Cassandra updated to 4.0-beta1
- PostgreSQL updated to 12.4

The complete list of changes and bugfixes can be found in CHANGES.TXT

Enjoy :-)
