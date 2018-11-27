FROM iexechub/ubuntu:16.04
EXPOSE 4321 4322 4323 4324 4327 4328 443

COPY docker/xtremweb.client.conf /iexec/conf/
COPY lib /iexec/lib
COPY bin /iexec/bin/ 
WORKDIR /iexec/bin
RUN mkdir -p /iexec/certificate

# remove non necessary scripts from /iexec/bin
# remove all subfolders:
RUN find . -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} +

# remove files that don't match xw* or xtr*
RUN find . -type f ! -name 'xw*' ! -name 'xtr*' -exec rm -rf {} +

# Add the script that will set up the configuration in the container
ADD docker/client/start_client.sh /iexec/start_client.sh

RUN chmod +x /iexec/start_client.sh

WORKDIR /iexec/bin
CMD bash -C '/iexec/start_client.sh';'bash'
