FROM maven:3.5-jdk-8 AS BUILD

ENV ANT_VERSION=1.10.3
ENV ANT_HOME=/opt/ant

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
WORKDIR ./datafari
COPY ./pom.xml .
COPY ./datafari-core/pom.xml ./datafari-core/pom.xml
COPY ./datafari-dependencies/pom.xml ./datafari-dependencies/pom.xml
COPY ./datafari-dependencies/ ./datafari-dependencies/
RUN ls -la ./datafari-dependencies
COPY ./datafari-solr/pom.xml ./datafari-solr/pom.xml
COPY ./datafari-zookeeper/pom.xml ./datafari-zookeeper/pom.xml
COPY ./datafari-jena/pom.xml ./datafari-jena/pom.xml
COPY ./datafari-mcf/pom.xml ./datafari-mcf/pom.xml
COPY ./datafari-cassandra/pom.xml ./datafari-cassandra/pom.xml
COPY ./datafari-tomcat/pom.xml ./datafari-tomcat/pom.xml
COPY ./datafari-elk/pom.xml ./datafari-elk/pom.xml
COPY ./datafari-updateprocessor/pom.xml ./datafari-updateprocessor/pom.xml
COPY ./datafari-tika/pom.xml ./datafari-tika/pom.xml
COPY ./datafari-handler/pom.xml ./datafari-handler/pom.xml
COPY ./datafari-realm/pom.xml ./datafari-realm/pom.xml
COPY ./datafari-mcf-scripts/pom.xml ./datafari-mcf-scripts/pom.xml
COPY ./datafari-git-plugin/pom.xml ./datafari-git-plugin/pom.xml
RUN mvn --log-file log1.txt -P ci -f pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
COPY . .

RUN mvn --log-file log2.txt -f pom.xml -s /usr/share/maven/ref/settings-docker.xml --quiet clean install
RUN ls -lah ./debian7
RUN ant clean-build -f ./debian7/build.xml
RUN ls /tmp/datafari/debian7/installer/dist/datafari.deb


FROM openjdk:8-jdk-stretch
MAINTAINER Olivier Tavard FRANCE LABS <olivier.tavard@francelabs.com>

RUN     apt-get update && apt-get install -y \
                wget \
                curl \
                jq \
                debconf \
                python-minimal \
                sudo \
                vim \
                nano \
                netcat \
                libc6-dev \
                unzip \
                lsof \
                procps \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=BUILD /tmp/datafari/debian7/installer/dist/datafari.deb .
RUN DEBIAN_FRONTEND=noninteractive dpkg -i datafari.deb && rm -rf datafari.deb
EXPOSE 8080 8983 5601 9200
RUN useradd -m demo && echo "demo:demo" | chpasswd && adduser demo sudo
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
WORKDIR /opt/datafari
RUN  sed -i -e 's/sleep 10/sleep 30/g' /opt/datafari/bin/start-datafari.sh
RUN  sed -i "/server.host:/c\server.host: 0.0.0.0" /opt/datafari/elk/kibana/config/kibana.yml
USER demo
CMD ["/bin/bash", "-c", "echo 'wait 120 seconds to let Datafari start and then connect to http://IP:EXPOSE_PORT/Datafari' && cd /opt/datafari/bin && bash start-datafari.sh; sleep infinity"]