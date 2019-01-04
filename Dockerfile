FROM maven:3.5-jdk-8 AS BUILD

ENV ANT_VERSION=1.10.3
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
WORKDIR ./datafari
COPY . .
RUN mvn -f pom.xml --quiet clean install | egrep -v "(^[0-9])|(^\[INFO\]|^\[DEBUG\])"
RUN ant clean-build -f ./debian7/build.xml

FROM openjdk:8-jdk-stretch
MAINTAINER Olivier Tavard FRANCE LABS <olivier.tavard@francelabs.com>

# temporary allow unauthenticatedparameter due to debian repo issue
RUN     apt-get update && apt-get install --allow-unauthenticated -y \
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
WORKDIR /var/datafari
COPY --from=BUILD /tmp/datafari/debian7/installer/dist/datafari.deb .
RUN DEBIAN_FRONTEND=noninteractive dpkg -i datafari.deb
EXPOSE 8080 9080 8983 5601 9200
RUN useradd -m demo && echo "demo:demo" | chpasswd && adduser demo sudo
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
WORKDIR /opt/datafari
RUN  sed -i -e 's/sleep 10/sleep 30/g' /opt/datafari/bin/start-datafari.sh
RUN  sed -i "/server.host:/c\server.host: 0.0.0.0" /opt/datafari/elk/kibana/config/kibana.yml
USER demo
CMD ["/bin/bash", "-c", "echo 'wait 120 seconds to let Datafari start and then connect to http://IP:EXPOSE_PORT/Datafari' && cd /opt/datafari/bin && bash start-datafari.sh; sleep infinity"] 


