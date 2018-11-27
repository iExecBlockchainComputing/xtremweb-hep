FROM iexechub/ubuntu:16.04-docker
EXPOSE 4321 4322 4323 4324 4327 4328 443

ADD conf/xtremweb.worker.conf /iexec/conf/
RUN mkdir -p /iexec/bin
RUN mkdir -p /iexec/certificate
RUN mkdir -p /iexec/wallet
ADD bin/xtremweb.worker /iexec/bin
ADD bin/xtremweb /iexec/bin
ADD bin/xtremwebconf.sh /iexec/bin
ADD bin/xwstartdocker.sh /iexec/bin
ADD bin/xwstartpdo.sh /iexec/bin
ADD bin/xwstopdocker.sh /iexec/bin
ADD lib /iexec/lib

# Add the script that will set up the configuration in the container
ADD docker/worker/start_worker.sh /iexec/start_worker.sh

WORKDIR /iexec

RUN chmod +x /iexec/start_worker.sh

ENTRYPOINT [ "/iexec/start_worker.sh" ]
