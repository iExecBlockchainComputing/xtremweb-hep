FROM arm64v8/ubuntu:16.04

EXPOSE 4321 4322 4323 4324 4327 4328 443

COPY ./docker/worker-arm32/qemu-arm-static /usr/bin/qemu-arm-static
COPY conf/xtremweb.worker.conf /iexec/conf/
COPY bin/xtremweb.worker /iexec/bin/
COPY bin/xtremweb /iexec/bin/
COPY bin/xtremwebconf.sh /iexec/bin/
COPY lib /iexec/lib/

# Add the script that will set up the configuration in the container
COPY docker/worker/start_worker.sh /iexec/start_worker.sh
RUN [ "/usr/bin/qemu-arm-static", "/bin/sh", "-c", "chmod +x /iexec/start_worker.sh" ]

# Install requirements
RUN [ "/usr/bin/qemu-arm-static", "/bin/sh", "-c",
    "export DEBIAN_FRONTEND=noninteractive && \
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
        zip" ]

# install docker
RUN [ "/usr/bin/qemu-arm-static", "/bin/sh", "-c", "curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - " ]

RUN [ "/usr/bin/qemu-arm-static", "/bin/sh", "-c", "add-apt-repository \"deb [arch=armhf] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable\" " ]

RUN [ "/usr/bin/qemu-arm-static", "/bin/sh", "-c", "apt-get update && apt-get install -y docker-ce" ]


ENTRYPOINT [ "/iexec/start_worker.sh" ]