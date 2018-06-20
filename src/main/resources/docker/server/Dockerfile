FROM ubuntu:16.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
        iputils-ping \
        make \
        openjdk-8-jdk \
        unzip \
        wget \
        zip \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Add the script that will set up the configuration in the container
COPY docker/server/start_server.sh /iexec/start_server.sh
RUN chmod +x /iexec/start_server.sh

COPY bin                        /iexec/bin
COPY lib                        /iexec/lib
COPY conf/iexec-scheduler.yml   /iexec/conf/
COPY conf/xtremweb.server.conf  /iexec/conf/
COPY conf/xwconfigure.values    /iexec/conf/

WORKDIR /iexec

ENTRYPOINT [ "/iexec/start_server.sh" ]
EXPOSE 4321 4322 4323 443