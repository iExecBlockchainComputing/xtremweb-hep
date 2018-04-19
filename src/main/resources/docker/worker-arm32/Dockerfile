FROM resin/rpi-raspbian
EXPOSE 4321 4322 4323 4324 4327 4328 443


RUN [ "cross-build-start" ]

# Install requirements
RUN export DEBIAN_FRONTEND=noninteractive && \
    apt-get update && \
    apt-get install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        openjdk-8-jre \
        openssl \
        software-properties-common \
        unzip \
        wget \
        zip \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# install docker
RUN curl -sSL https://get.docker.com | sh

# Add the script that will set up the configuration in the container
COPY docker/worker/start_worker.sh /iexec/start_worker.sh

RUN mkdir /iexec/certificate && \
    chmod +x /iexec/start_worker.sh

RUN [ "cross-build-end" ]

COPY conf/xtremweb.worker.conf /iexec/conf/
COPY bin/xtremweb.worker /iexec/bin/
COPY bin/xtremweb /iexec/bin/
COPY bin/xtremwebconf.sh /iexec/bin/
COPY lib /iexec/lib/


ENTRYPOINT [ "/iexec/start_worker.sh" ]