--------------------------- DATAFARI ------------------------

Datafari is an open source enterprise search solution enriched with AI. It is the perfect product for anyone who needs to search, analyze and discuss with its corporate knowledge (be it data and documents), both within the content and the metadata. Plus, with its genAI modules, it allows to easily leverage mistral, openai, or local LLMs for your company data.

Available as community (open source) and enterprise (proprietary) edition, Datafari is different from the competition :
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it, you just need to mention that you are using it.
- It is well documented, both from a functional and technical perspective.
- It exists since 2015, proof that it is sustainable on the long run.


The complete documentation (for users, admins and developers) is available here : https://datafari.atlassian.net/wiki/display/DATAFARI/Datafari

I. HOW TO INSTALL DATAFARI :

i. DOCKER:
The easiest is probably to use our docker container: https://hub.docker.com/r/datafari/datafari
To avoid most crashes, be sure to assign enough resources to your container:
See https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/1662451718/Hardware+requirements for information about the requirements

ii. For any other installation modes, refer to: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/66125825/Quick+Start+Guide

II. HOW TO PLAY WITH DATAFARI :

i. Search UI :
https://localhost/datafariui/

ii. Admin UI :
https://localhost/Datafari/admin/?lang=en

NOTE 1: Java 21 is REQUIRED
NOTE 2: to crawl files, you need to install an extra jar that we cannot distribute for open source licence conflict rationales (Apachev2 and LGPL). Please follow this documentation: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/662700036/Add+the+JCIFS-NG+Connector+to+Datafari+-+Community+Edition

* Major new features compared to v6 :

- New Datafari AI Assistant with agentic services
- Several search modes available (BM25, vector, hybrid)
- Major upgrades, in particular Spring and Solr
- Lower hardware footprint by migrating Cassandra to PostgreSQL 

* Version of major components :

- Solr 10.0
- ManifoldCF 2.28
- Tomcat 10.1.48
- PostgreSQL 18.3
- Zookeeper 3.9.4
- Tika Server 3.2.3
- DatafariUI-v2 2.0.1
- AdminUI 1.0
- Assistant-UI 1.0


The complete list of changes and bugfixes can be found in CHANGES.TXT

Enjoy 🙂
