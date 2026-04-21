FROM maven:3.9.14-eclipse-temurin-21-noble AS BUILD
ENV ANT_VERSION=1.10.9
ENV ANT_HOME=/opt/ant

# Temporary Workaround to Surefire issue 
ENV _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true

# change to tmp folder

WORKDIR /tmp

# Download and extract apache ant to opt folder
RUN wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz \
    && wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz.sha512 \
    && echo "$(cat apache-ant-${ANT_VERSION}-bin.tar.gz.sha512) apache-ant-${ANT_VERSION}-bin.tar.gz" | sha512sum -c \
    && tar -zxf apache-ant-${ANT_VERSION}-bin.tar.gz -C /opt/ \
    && ln -s /opt/apache-ant-${ANT_VERSION} /opt/ant \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz.sha512

# add executables to path
RUN update-alternatives --install "/usr/bin/ant" "ant" "/opt/ant/bin/ant" 1 && \
    update-alternatives --set "ant" "/opt/ant/bin/ant" 

RUN     apt-get update && apt-get install -y \
               git \
	&& rm -rf /var/lib/apt/lists/*

# TODO : add specific COPY
COPY . .

# ---- Maven build (uses Nexus + Central/Snapshots) ----
RUN mvn -f pom.xml -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B clean install 

# ---- Ant build ----
RUN ant clean-build -f ./linux/build.xml


# =========================================================
# === Stage 2: Runtime image with built Datafari package ===
# =========================================================
FROM maven:3.9.14-eclipse-temurin-21-noble
MAINTAINER Olivier Tavard FRANCE LABS <olivier.tavard@francelabs.com>

ENV RESTORE=false
ENV visilia=false
ENV DATAFARIUIDEV=false

# temporary allow unauthenticatedparameter due to debian repo issue
RUN echo iptables-persistent iptables-persistent/autosave_v4 boolean true | debconf-set-selections
RUN echo iptables-persistent iptables-persistent/autosave_v6 boolean true | debconf-set-selections
RUN     apt-get update && apt-get install --allow-unauthenticated -y \
                wget \
                curl \
                jq \
                debconf \
                python3 \
                python-is-python3 \
                sudo \
                vim \
                nano \
                netcat-traditional \
                libc6-dev \
                unzip \
                lsof \
                procps \
                apache2 \
                libapache2-mod-jk \
				nftables \
                systemd \
                zip \
                procps \
                iputils-ping \
                bc \
				netcat-openbsd \
				libaprutil1-dbd-pgsql \
				libpq5 \
	&& rm -rf /var/lib/apt/lists/*
# For dev
RUN echo "export LANG=C.UTF-8" >> /etc/profile
#RUN echo "export JAVA_HOME=/usr/local/openjdk-11" >> /etc/profile
#RUN echo "export PATH=$JAVA_HOME/bin:$PATH" >> /etc/profile
RUN echo "export LOG4J_FORMAT_MSG_NO_LOOKUPS=true" >> /etc/profile
WORKDIR /var/datafari
RUN useradd datafari -m -s /bin/bash
#COPY --chown=datafari:root --from=BUILD /tmp/datafari/linux/installer/build/datafari/opt/datafari .
COPY --from=BUILD /tmp/linux/installer/dist/datafari.deb /var/datafari/datafari.deb
RUN DEBIAN_FRONTEND=noninteractive dpkg -i datafari.deb
EXPOSE 80 443
WORKDIR /opt/datafari
VOLUME /opt/datafari/bin/backup
#RUN chmod -R 775 /opt/datafari/bin/deployUtils/docker
RUN echo "SOLR_JAVA_HOME=$JAVA_HOME" >> /opt/datafari/solr/bin/solr.in.sh
RUN  sed -i -e 's/sleep 10/sleep 30/g' /opt/datafari/bin/start-datafari.sh
CMD ["/bin/bash", "-c", "/opt/datafari/bin/deployUtils/docker/debian-start-datafari.sh"]