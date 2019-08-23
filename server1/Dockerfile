FROM node:dubnium

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY . .
RUN apt-get update -y
RUN apt-get update
RUN apt-get install cmake -y
RUN apt-get install libx11-dev -y
RUN apt-get install libpng-dev -y
RUN apt-get install libopenblas-dev -y
RUN npm install

EXPOSE 6100-17100
CMD [ "npm", "start" ]
