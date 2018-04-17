FROM ubuntu:16.04
EXPOSE 4321 4322 4323 4324 4327 4328 443

ADD conf/xtremweb.worker.conf /iexec/conf/
RUN mkdir -p /iexec/bin
RUN mkdir -p /iexec/certificate
ADD bin/xtremweb.worker /iexec/bin
ADD bin/xtremweb /iexec/bin
ADD bin/xtremwebconf.sh /iexec/bin
ADD bin/xwstartdocker.sh /iexec/bin
ADD bin/xwstopdocker.sh /iexec/bin

ADD lib /iexec/lib

# Add the script that will set up the configuration in the container
ADD docker/worker/start_worker.sh /iexec/start_worker.sh

WORKDIR /iexec

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive && apt-get install -y openjdk-8-jre zip unzip wget

RUN apt-get install -y curl openssl apt-transport-https ca-certificates software-properties-common 

# install docker
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
RUN apt-get update
RUN apt-get install -y docker-ce

RUN chmod +x /iexec/start_worker.sh

ENTRYPOINT [ "/iexec/start_worker.sh" ]
