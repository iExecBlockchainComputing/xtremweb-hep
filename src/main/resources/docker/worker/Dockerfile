FROM ubuntu:16.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
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
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - && \
    add-apt-repository \
        "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
        $(lsb_release -cs) \
        stable" && \
    apt-get update && apt-get install -y --no-install-recommends docker-ce

RUN mkdir -p /iexec/bin /iexec/certificate /iexec/wallet

# Add the script that will set up the configuration in the container
COPY docker/worker/start_worker.sh /iexec/start_worker.sh
RUN chmod +x /iexec/start_worker.sh

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