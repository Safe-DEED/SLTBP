FROM ubuntu:18.04
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    maven \
    make
WORKDIR /home/SLTBP
ADD . /home/SLTBP
RUN mvn clean install
CMD make run