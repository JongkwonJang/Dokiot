# Dokiot (장종권, 원경필, 양희규, 유한상)
실시간 다중 얼굴인식을 위한 Docker 기반 플랫폼

* [Introduction](#introduction)
* [Install](#install)
* [How to use](#how-to-use)
* [Contact](#contact)
* [Used open source](#used-open-source)
* [License](#license)

## Introduction
저희는 **실시간 다중 얼굴인식 플랫폼**을 만들었습니다.
* Docker Container를 활용하여 분산처리
* 감지한 얼굴과 정보를 표시
* 유명인 얼굴인식과 최근 근황 표시 (NAVER에서 제공하는 CFR API와 News API 사용)
* 스마트폰 화면에 있는 정보를 숨기고 싶을 시 화면을 1번 터치
* 스마트폰 화면을 지속적으로 누르면 웹사이트 주소를 알려줌
* 관리자 모드 제공(Website를 통해 DB내에 있는 정보 관리) 

플랫폼은 **Server**와 **Client**로 구성됩니다.
* Server는 Docker Container(DB, Soket, Web Server)와 Server Controller로 구성
* DB는 얼굴 특징값과 인물 정보가 저장되어 있습니다.
* Controller는 Client마다 Docker Container를 할당합니다.
* Client는 스마트폰, 태블릿 등 과 같은 스마트기기로 구성됩니다.
* Client는 Controller에 Port 요청한 뒤 할당 받은 Port로 Docker Container에 연결합니다. (1 Client - 1 Docker Container)

#### 스마트폰에서 보이는 실시간 다중 얼굴인식 모습
![2](https://user-images.githubusercontent.com/46180332/50539769-44ac8600-0bc9-11e9-8d39-f17f59a7d24a.gif)

#### 웹사이트에서 보이는 유명인 인식 API와 News API를 적용했을 때의 모습
![-](https://user-images.githubusercontent.com/46180332/50533598-1f3f5e00-0b70-11e9-9a64-cd5dd3962c83.gif)

#### 스마트폰에서 보이는 유명인 인식 API와 News API를 적용했을 떄의 모습
![1](https://user-images.githubusercontent.com/46180332/50539526-0d3bda80-0bc5-11e9-8fad-75eac7e1bf8e.gif)

#### 웹사이트에서 인식된 사람의 정보를 수정할 때의 모습
![- -](https://user-images.githubusercontent.com/46180332/50538309-d360d900-0bb0-11e9-8ae8-bec183d41be4.gif)

#### 웹사이트에서 인식이 안된 사람의 정보를 삽입할 때의 모습
![-](https://user-images.githubusercontent.com/46180332/50538322-1cb12880-0bb1-11e9-98c5-a4c7cb09e822.gif)

## Install
먼저 mysql를 설치하고 설정해야합니다.
#### [mysql](https://github.com/mysqljs/mysql)
1. # apt-get update
2. # apt-get install mysql-server
3. # cd sever
4. mysql 접속
5. create database client_information / create database face_recognition // 데이터 베이스 생성
6. mysql -u root -p client_information < client_information.sql / mysql -u root -p face_recognition < face_recognition.sql

Docker를 설치한 뒤 이미지 파일을 만들어야 합니다.

1. # curl -fsSL https://get.docker.com/ | sudo sh
2. # sudo usermod -aG docker $USER
3. # sudo init 6
4. # docker version
5. # docker build -t server .
6. # docker images

## How to use
사용 방법은 다음과 같습니다.
1. **Install** 과정 완료 (**만약 네이버 서버에서 실행 시 1번 과정을 생략해도됨**)
2. server 파일 내에 있는 serverController.js( **node serverController.js** )를 실행시킨 뒤 client의 스마트폰에서 해당 앱을 실행
3. 관리자 모드를 사용하고 싶을 때는 스마트폰 화면을 지속적으로 누르면 웹사이트 주소를 알려줌. 이 때, 이 주소로 접속하면 관리자 모드를 사용할 수 있음

## Contact
#### rhantls2279@naver.com

## Used open source

#### [face-recognition](https://github.com/justadudewhohacks/face-recognition.js)

#### [mysql](https://github.com/mysqljs/mysql)

#### [request](https://github.com/request/request)

#### [websocket](https://github.com/theturtle32/WebSocket-Node)

## License
Copyright(c)2019 Jongkwon Jang. All rights reserved.
