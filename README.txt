# Datafari

I've been trying to install [datafari](https://github.com/francelabs/datafari). They propose packages for debian distribution or windows, but no straightforward solution for developers.

In this project, you can install their solution with maven commands. Dependencies have been reduced to a bare minimum. As there are more than two commands to launch, I wrapped them in a simple Makefile command.

Please execute the following, and then browse to http://localhost:8080/Datafari (with the uppercase 'D'). Pay attention that you can change your host and port in the pom.xml, but some web pages have these values hardcoded in the datafari project. Also, I'm not an expert on solr or datafari, therefore I don't know how to populate solr. Contact francelabs team for this ;)


```
make run
```

### Dependencies

So far, I'd say you just need to have maven and a jdk installed on your computer.

### Optimization

Effective project size from the original project has been reduced from around 164 MB to around 109MB (I've excluded the windows, debian7 and mcf folders to compare sizes).

### Not addressed
I haven't looked at manifold deployement. Datafari and solr only are deployed in tomcat thanks to mvn.

Gabriel
