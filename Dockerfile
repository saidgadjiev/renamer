FROM ubuntu:20.04

RUN useradd -ms /bin/bash bot
USER bot

RUN mkdir -p /home/bot/app
WORKDIR /home/bot/app

USER root

ENV DEBIAN_FRONTEND noninteractive 
ENV USE_SANDBOX false

RUN apt-get update -y
RUN apt-get install -y openjdk-11-jre build-essential curl wget gdebi locales

# Locale
ENV LC_ALL C.UTF-8

USER root

RUN sed -Ei 's/^# deb-src /deb-src /' /etc/apt/sources.list
RUN apt-get update -y
RUN apt-get -y build-dep imagemagick
RUN apt-get install -y libwebp-dev libopenjp2-7-dev librsvg2-dev libde265-dev libheif-dev

RUN echo 'Imagemagick 7.0.10-19'
RUN cd /usr/src/ && \
wget https://www.imagemagick.org/download/ImageMagick.tar.bz2 && \
tar xvf ImageMagick.tar.bz2 && cd ImageMagick* && \
./configure && make -j 4 && \
make install && \
make distclean && ldconfig

RUN apt-get clean -y && apt-get autoclean -y && apt-get autoremove -y && rm -rf /var/lib/apt/lists/* /var/tmp/*

USER bot

COPY ./target/app.jar .


EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
#CMD ["-jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "app.jar"]
