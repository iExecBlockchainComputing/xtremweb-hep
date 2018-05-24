FROM ubuntu:16.04
EXPOSE 4321 4322 4323 443

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive && \
    apt-get install -y \
        iputils-ping \
        make \
        openjdk-8-jdk \
        unzip \
        wget \
        zip

# Add the script that will set up the configuration in the container
COPY docker/server/start_server.sh /iexec/start_server.sh
RUN chmod +x /iexec/start_server.sh

COPY conf/xtremweb.server.conf /iexec/conf/
COPY conf/xwconfigure.values /iexec/conf/
COPY conf/iexec-scheduler.yml /iexec/conf/
COPY lib /iexec/lib
COPY bin /iexec/bin/

ENTRYPOINT [ "/iexec/start_server.sh" ]


