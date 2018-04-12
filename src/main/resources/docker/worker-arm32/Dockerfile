FROM resin/rpi-raspbian

EXPOSE 4321 4322 4323 4324 4327 4328 443

COPY conf/xtremweb.worker.conf /iexec/conf/
COPY bin/xtremweb.worker /iexec/bin/
COPY bin/xtremweb /iexec/bin/
COPY bin/xtremwebconf.sh /iexec/bin/
COPY lib /iexec/lib/

RUN [ "cross-build-start" ]

RUN mkdir /iexec/certificate

# Add the script that will set up the configuration in the container
COPY docker/worker/start_worker.sh /iexec/start_worker.sh
RUN chmod +x /iexec/start_worker.sh

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
        zip

# install docker
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | sudo apt-key add -
RUN echo "deb [arch=armhf] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list
RUN apt-get update && apt-get install -y docker-ce

RUN [ "cross-build-end" ]  

ENTRYPOINT [ "/iexec/start_worker.sh" ]