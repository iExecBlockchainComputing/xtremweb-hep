FROM ubuntu:16.04
EXPOSE 4321 4322 4323 4324 4327 4328 443


RUN apt-get update && export DEBIAN_FRONTEND=noninteractive && \
    apt-get install -y \
        curl \
        iputils-ping \
        openjdk-8-jre \
        openssl \
        unzip \
        wget \
        zip

# Add the script that will set up the configuration in the container
COPY docker/client/start_client.sh /iexec/start_client.sh
RUN chmod +x /iexec/start_client.sh && \
    mkdir /iexec/certificate

COPY docker/xtremweb.client.conf /iexec/conf/
COPY lib /iexec/lib
COPY bin /iexec/bin/

WORKDIR /iexec/bin

# remove non necessary scripts from /iexec/bin
# remove all subfolders:
RUN find . -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} +

# remove files that don't match xw* or xtr*
RUN find . -type f ! -name 'xw*' ! -name 'xtr*' -exec rm -rf {} +

CMD bash -C '/iexec/start_client.sh';'bash'
