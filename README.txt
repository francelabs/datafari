--------------------------- DATAFARI ------------------------

Datafari is an open source enterprise search solution enriched with AI. It is the perfect product for anyone who needs to search and analyze its corporate data and documents, both within the content and the metadata. Plus, with its genAI modules, it allows to easily leverage mistral, openai, or local LLMs for your company data.

Available as community (open source) and enterprise (proprietary) edition, Datafari is different from the competition :
- Its open source license is not aggressive, as it uses the Apache v2 license: you are free to do whatever you want with it, you just need to mention that you are using it.
- It combines three renowned Apache projects, namely Cassandra, Solr and ManifoldCF, which gives Datafari a long term vision.

The complete documentation (for users, admins and developers) is available here : https://datafari.atlassian.net/wiki/display/DATAFARI/Datafari

I. HOW TO INSTALL DATAFARI :

i. DOCKER:
The easiest is probably to use our docker container: https://hub.docker.com/r/datafari/datafari
To avoid most crashes, be sure to assign enough resources to your container:
See https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/1662451718/Hardware+requirements for information about the requirements

ii. OVA:
If you don't fancy docker, you can opt for a Virtual Machine, which is fully preconfigured: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/66125825/Quick+Start+Guide
To avoid most crashes, be sure to assign enough resources to your container:
See https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/1662451718/Hardware+requirements for more information about the requirements

iii. For any other installation modes, refer to: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/66125825/Quick+Start+Guide

II. HOW TO PLAY WITH DATAFARI :

i. Search UI :
https://localhost/datafariui/

ii. Admin UI :
https://localhost/Datafari/admin/?lang=en

NOTE 1: Java 11 is REQUIRED
NOTE 2: to crawl files, you need to install an extra jar that we cannot distribute for open source licence conflict rationales (Apachev2 and LGPL). Please follow this documentation: https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/662700036/Add+the+JCIFS-NG+Connector+to+Datafari+-+Community+Edition

* Major new features compared to v6.0 :
New genAI functionalities: 
*** RAG at search time including vector search for retrieval
*** Vector collection with automatic chunking at indexing time
*** Transformation connectors at indexing time
AI agent (external repo but open source) to prototype local usage of LLMs
New UI Framework allowing for easier updates
Autosuggestion to directly access to documents from the search dropdown

* Major components versions
- Solr 9.8.1
- ManifoldCF 2.28
- Tomcat 9.0.105
- Cassandra 4.1.3
- PostgreSQL 15.4
- Zookeeper 3.9.2
- Tika Server 2.9.4
- DatafariUI-v2 1.0
- AdminUI 1.0

The complete list of changes and bugfixes can be found in CHANGES.TXT

Enjoy 🙂
