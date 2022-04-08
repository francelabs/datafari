FROM maven:3.6.3-jdk-11 AS BUILD

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
#COPY datafari-ce datafari-ce
#COPY datafari-ee/datafari-zookeeper datafari-ee/datafari-zookeeper
#COPY datafari-ee/datafari-zookeeper-mcf datafari-ee/datafari-zookeeper-mcf
#COPY .drone.yml .drone.yml
#COPY datafari-ee/CHANGES.txt datafari-ee/CHANGES.txt
#COPY datafari-ee/LICENSE.txt datafari-ee/LICENSE.txt
#COPY datafari-ee/README.txt datafari-ee/README.txt
#COPY datafari-ee/pom.xml datafari-ee/pom.xml
#COPY .git .git
RUN mvn -f pom.xml -DfailIfNoTests=false -Dtest='!TestDataServices' -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B clean install 
RUN ls
#COPY datafari-ee/apache apache
RUN ls
#COPY datafari-ee/bin/common bin/common
#COPY datafari-ee/linux linux
#COPY datafari-ee/opensearch opensearch
#COPY datafari-ee/ssl-keystore ssl-keystore
RUN ant clean-build -f ./linux/build.xml

FROM openjdk:11.0.8-jdk-buster
MAINTAINER Olivier Tavard FRANCE LABS <olivier.tavard@francelabs.com>

# temporary allow unauthenticatedparameter due to debian repo issue
RUN echo iptables-persistent iptables-persistent/autosave_v4 boolean true | debconf-set-selections
RUN echo iptables-persistent iptables-persistent/autosave_v6 boolean true | debconf-set-selections
RUN     apt-get update && apt-get install --allow-unauthenticated -y \
                wget \
                curl \
                jq \
                debconf \
                python \
                sudo \
                vim \
                nano \
                netcat \
                libc6-dev \
                unzip \
                lsof \
                procps \
                apache2 \
                libapache2-mod-jk \
                iptables \
                iptables-persistent \
                systemd \
                zip \
	&& rm -rf /var/lib/apt/lists/*
# For dev
RUN echo "export LANG=C.UTF-8" >> /etc/profile
RUN echo "export JAVA_HOME=/usr/local/openjdk-11" >> /etc/profile
RUN echo "export PATH=$JAVA_HOME/bin:$PATH" >> /etc/profile
RUN echo "export LOG4J_FORMAT_MSG_NO_LOOKUPS=true" >> /etc/profile

WORKDIR /var/datafari
RUN useradd datafari -m -s /bin/bash
#COPY --chown=datafari:root --from=BUILD /tmp/datafari/linux/installer/build/datafari/opt/datafari .
COPY --from=BUILD /tmp/linux/installer/dist/datafari.deb /var/datafari/datafari.deb
RUN DEBIAN_FRONTEND=noninteractive dpkg -i datafari.deb
EXPOSE 8080 8983 9080 5601 9200 80 443
WORKDIR /opt/datafari
#RUN chmod -R 775 /opt/datafari/bin/deployUtils/docker
RUN  sed -i -e 's/sleep 10/sleep 30/g' /opt/datafari/bin/start-datafari.sh
CMD ["/bin/bash", "-c", "/opt/datafari/bin/deployUtils/docker/debian-start-datafari.sh"]


