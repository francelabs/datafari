#

dependency: lib/j2ep-datafari.jar 
	mkdir -p mvn-repo
	mvn install:install-file -Dfile=lib/j2ep-datafari.jar -DgroupId=net.sf.j2ep -DartifactId=j2ep-datafari -Dversion=1.0.0 -Dpackaging=jar -DlocalRepositoryPath=mvn-repo

run: dependency
	mvn -Dsolr.solr.home=solr/solr_home clean tomcat7:run

clean:
	mvn clean
