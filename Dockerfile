# =========================================================
# === Stage 1: Build Datafari using Nexus + direct repos ===
# =========================================================
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

# ---- Nexus parameters (override with --build-arg as needed) ----
ARG NEXUS_BASE=https://nexus.datafari.com
ARG NEXUS_GROUP_PATH=/repository/maven-public
ARG NEXUS_INTERNAL_PATH=/repository/maven-releases

ENV ANT_VERSION=1.10.9
ENV ANT_HOME=/opt/ant
# Avoid classpath URL checks in some build plugins
ENV _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true

WORKDIR /tmp

# ---- Create Maven settings.xml: disable "maven-default-http-blocker" and define repositories ----
RUN mkdir -p /root/.m2 && \
    printf '%s\n' \
      '<settings xmlns="https://maven.apache.org/SETTINGS/1.2.0">' \
      '  <servers>' \
      '    <server>' \
      '      <id>nexus-internal</id>' \
      "      <username>${NEXUS_USER}</username>" \
      "      <password>${NEXUS_PASS}</password>" \
      '    </server>' \
      '  </servers>' \
      '  <mirrors>' \
      '    <!-- Neutralize Maven 3.8+ HTTP blocker for legacy/internal use cases -->' \
      '    <mirror>' \
      '      <id>maven-default-http-blocker</id>' \
      '      <mirrorOf>none</mirrorOf>' \
      '      <url>http://0.0.0.0/</url>' \
      '    </mirror>' \
      '  </mirrors>' \
      '  <profiles>' \
      '    <profile>' \
      '      <id>direct-and-internal</id>' \
      '      <repositories>' \
      '        <!-- Maven Central -->' \
      '        <repository>' \
      '          <id>maven-central</id>' \
      '          <url>https://repo1.maven.org/maven2</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </repository>' \
      '        <!-- Apache Snapshots (Solr/Lucene/platform SNAPSHOTs) -->' \
      '        <repository>' \
      '          <id>apache-snapshots</id>' \
      '          <url>https://repository.apache.org/snapshots</url>' \
      '          <releases><enabled>false</enabled></releases>' \
      '          <snapshots><enabled>true</enabled></snapshots>' \
      '        </repository>' \
      '        <!-- Internal Nexus (hosted releases) for private artifacts -->' \
      '        <repository>' \
      '          <id>nexus-internal</id>' \
      "          <url>${NEXUS_BASE}${NEXUS_INTERNAL_PATH}</url>" \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </repository>' \
      '      </repositories>' \
      '      <pluginRepositories>' \
      '        <pluginRepository>' \
      '          <id>maven-central</id>' \
      '          <url>https://repo1.maven.org/maven2</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </pluginRepository>' \
      '        <pluginRepository>' \
      '          <id>apache-snapshots</id>' \
      '          <url>https://repository.apache.org/snapshots</url>' \
      '          <releases><enabled>false</enabled></releases>' \
      '          <snapshots><enabled>true</enabled></snapshots>' \
      '        </pluginRepository>' \
      '        <pluginRepository>' \
      '          <id>nexus-internal</id>' \
      "          <url>${NEXUS_BASE}${NEXUS_INTERNAL_PATH}</url>" \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </pluginRepository>' \
      '      </pluginRepositories>' \
      '    </profile>' \
      '  </profiles>' \
      '  <activeProfiles><activeProfile>direct-and-internal</activeProfile></activeProfiles>' \
      '</settings>' > /root/.m2/settings.xml

# ---- System tools ----
RUN apt-get update && apt-get install -y git wget curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# ---- Install Apache Ant ----
RUN wget --no-check-certificate --no-cookies \
      http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz \
 && wget --no-check-certificate --no-cookies \
      http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz.sha512 \
 && echo "$(cat apache-ant-${ANT_VERSION}-bin.tar.gz.sha512) apache-ant-${ANT_VERSION}-bin.tar.gz" | sha512sum -c \
 && tar -zxf apache-ant-${ANT_VERSION}-bin.tar.gz -C /opt/ \
 && ln -s /opt/apache-ant-${ANT_VERSION} /opt/ant \
 && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz apache-ant-${ANT_VERSION}-bin.tar.gz.sha512

RUN update-alternatives --install "/usr/bin/ant" "ant" "/opt/ant/bin/ant" 1 \
 && update-alternatives --set "ant" "/opt/ant/bin/ant"

# ---- Pre-install a minimal org.apache:platform:10.0.0-SNAPSHOT POM locally to avoid descriptor resolution failures ----
RUN printf '%s\n' \
  '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">' \
  '  <modelVersion>4.0.0</modelVersion>' \
  '  <groupId>org.apache</groupId>' \
  '  <artifactId>platform</artifactId>' \
  '  <version>10.0.0-SNAPSHOT</version>' \
  '  <packaging>pom</packaging>' \
  '</project>' > /tmp/platform-10.0.0-SNAPSHOT.pom \
 && mvn -q -s /root/.m2/settings.xml install:install-file \
      -DgroupId=org.apache \
      -DartifactId=platform \
      -Dversion=10.0.0-SNAPSHOT \
      -Dpackaging=pom \
      -Dfile=/tmp/platform-10.0.0-SNAPSHOT.pom

# ---- Copy Datafari sources ----
COPY . .

# ---- Maven build (uses Nexus + Central/Snapshots) ----
RUN mvn -s /root/.m2/settings.xml -f pom.xml \
       -DskipTests \
       -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
       -U -B clean install

# ---- Ant build ----
RUN ant clean-build -f ./linux/build.xml


# =========================================================
# === Stage 2: Runtime image with built Datafari package ===
# =========================================================
FROM maven:3.9.9-eclipse-temurin-21-jammy
LABEL maintainer="Olivier Tavard FRANCE LABS <olivier.tavard@francelabs.com>"

ENV RESTORE=false
ENV visilia=false
ENV DATAFARIUIDEV=false

# ---- System dependencies ----
RUN echo iptables-persistent iptables-persistent/autosave_v4 boolean true | debconf-set-selections \
 && echo iptables-persistent iptables-persistent/autosave_v6 boolean true | debconf-set-selections \
 && apt-get update && apt-get install --allow-unauthenticated -y \
      wget curl jq debconf python3 python-is-python3 sudo vim nano netcat libc6-dev unzip \
      lsof procps apache2 libapache2-mod-jk nftables systemd zip procps iputils-ping bc netcat-openbsd libaprutil1-dbd-pgsql libpq5 \
 && rm -rf /var/lib/apt/lists/*

RUN echo "export LANG=C.UTF-8" >> /etc/profile
RUN echo "export LOG4J_FORMAT_MSG_NO_LOOKUPS=true" >> /etc/profile

WORKDIR /var/datafari
RUN useradd datafari -m -s /bin/bash

# ---- Copy Datafari Debian package from the build stage ----
COPY --from=build /tmp/linux/installer/dist/datafari.deb /var/datafari/datafari.deb
RUN DEBIAN_FRONTEND=noninteractive dpkg -i datafari.deb

EXPOSE 8080 8983 9080 5601 9200 80 443
WORKDIR /opt/datafari
VOLUME /opt/datafari/bin/backup

# Ensure Solr uses the same JDK
RUN echo "SOLR_JAVA_HOME=$JAVA_HOME" >> /opt/datafari/solr/bin/solr.in.sh

# Slightly delay startup to accommodate services ordering in containers
RUN sed -i -e 's/sleep 10/sleep 30/g' /opt/datafari/bin/start-datafari.sh

CMD ["/bin/bash", "-c", "/opt/datafari/bin/deployUtils/docker/debian-start-datafari.sh"]
