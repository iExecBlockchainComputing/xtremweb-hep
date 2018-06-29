FROM resin/raspberrypi3-openjdk:8-jre

ENV DEBIAN_FRONTEND=noninteractive

RUN [ "cross-build-start" ]

RUN apt-get update && apt-get install -y --no-install-recommends \
        apt-transport-https \
        ca-certificates \
        curl \
        openssl \
        software-properties-common \
        unzip \
        wget \
        zip \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN curl -fsSL get.docker.com | sh

RUN mkdir -p /iexec/bin /iexec/certificate /iexec/wallet

# this script will set up the configuration in the container
COPY docker/worker-arm32/start_worker.sh /iexec/start_worker.sh
RUN chmod +x /iexec/start_worker.sh

RUN [ "cross-build-end" ]

COPY lib                        /iexec/lib
COPY bin/xtremweb               /iexec/bin/
COPY bin/xtremweb.worker        /iexec/bin/
COPY bin/xtremwebconf.sh        /iexec/bin/
COPY bin/xwstartdocker.sh       /iexec/bin/
COPY bin/xwstopdocker.sh        /iexec/bin/
COPY conf/xtremweb.worker.conf  /iexec/conf/

WORKDIR /iexec

ENTRYPOINT [ "/iexec/start_worker.sh" ]
EXPOSE 4321 4322 4323 4324 4327 4328 443