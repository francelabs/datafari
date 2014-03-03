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

- Windows Environment
- Processor : 1GHZ and RAM : 2GB)
- Ports 8080 and 5432 are opened

Start Datafari server with start-datafari.bat. Always stop it with stop-datafari.bat. You don't have to (but you can) use the init-datafari.bat
script if you installed Datafari with the installer.

- Search UI :
http://localhost:8080/Datafari/

- Admin UI :
http://localhost:8080/Datafari/admin

You have to configure your Repository connector and job to add documents to Datafari
You can find documentation on how to create connectors and jobs here : 
http://manifoldcf.apache.org/release/trunk/en_US/end-user-documentation.html

Enjoy :-)
