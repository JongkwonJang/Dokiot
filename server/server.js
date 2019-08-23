var net = require('net');
var fs = require("fs");
var fr = require('face-recognition');
var mysql = require('mysql');
var request = require('request');
var http = require('http');//http서버 만들기 위해 사용
var path = require('path');// 경로 설정
var express = require('express');//웹서버를 만들기 위해 사용
var WebSocketServer = require('websocket').server; // 웹통신을 하기 위해 사용

var app = express();
app.use(express.static(path.join(__dirname, 'public')));
var webServer = http.createServer(app);
var webSocketClient;

// naver face api
var face_client_id = '4WJapEhMses36Y2YmBnR';
var face_client_secret = '68kLxV0viR';
var face_uri = 'https://openapi.naver.com/v1/vision/celebrity'; // 유명인 인식

var face_headers = {
        'X-Naver-Client-Id': face_client_id,
        'X-Naver-Client-Secret': face_client_secret
};

// naver news api
var news_client_id = 'MENuopwTfjYgP_Gt1IF4';
var news_client_secret = 'a7wsPDQGhp';
var news_headers = {
        'X-Naver-Client-Id': news_client_id,
        'X-Naver-Client-Secret': news_client_secret
};
//
var argvs = process.argv;
var port = argvs[2];
var webPort = parseInt(port) + 10000;

var tryCount = 0;
var commonPath2 = 'public/images/';
var commonPath3 = 'public/';
var detector = fr.FaceDetector();
var recognizer = fr.FaceRecognizer();
var SFRCount = 0;

var noRecCount = 0;

//JSON Information
var tempResult = [];
var tempPredictions = [];
var faceLocation = [];
//
var noDetectCount = 0;


var connection = mysql.createConnection({
	host : '127.0.0.1',
	user : 'root',
	password : 'root',
	database : 'face_recognition',
	ssl : false
});

//현재 시간 리턴
function getCurrentDate() {
	var currentDate = new Date();
	var year = currentDate.getFullYear();
	var month = currentDate.getMonth() + 1;
	var date = currentDate.getDate();
	var hour = currentDate.getHours();
	var minute = currentDate.getMinutes();
	var second = currentDate.getSeconds();
	if (month < 10) {
		month = "0" + month;
	}
	if (date < 10) {
		date = "0" + date;
	}
	if (hour < 10) {
		hour = "0" + hour;
	}
	if (minute < 10) {
		minute = "0" + minute;
	}
	if (second < 10) {
		second = "0" + second;
	}
	return year + "-" + month + "-" + date + " " + hour + ":" + minute + ":" + second;
}

//얼굴 이미지 학습
function learningFace(dataTemp, dataCount, files){
        var dirFile = [];
	console.log("point : " + dataTemp[dataCount]);
        for(var i=0; i<files.length; i++){
                dirFile[i] = fr.loadImage(commonPath2 +'faceDetectStorage/' + dataTemp[dataCount] + '/' + files[i]);
		console.log(dataTemp[dataCount] + " ");
        }
	recognizer.addFaces(dirFile, dataTemp[dataCount]);
	var modelStateSerial = recognizer.serialize();
	var wStream = fs.createWriteStream('./model.json');
	wStream.write(JSON.stringify(modelStateSerial));
	wStream.end(function(err){
		console.log(dataTemp[dataCount] + " learning Done --------------------------------------------------");
		dataCount++;
        	if(dataCount < dataTemp.length){
                	readFileForLearning(dataTemp, dataCount);
        	}
		else{
			learningStart = false;
			noDetectCount = 0;
		}
	});

}

//학습을 하기 위해 mysql에 있는 등록된 사용자 이름 추출
function searchDBForLearning(){
        var querys = 'SELECT korName From info';
        var dataCount = 0;
        var dataTemp = [];
        connection.query(querys, function(error, results, fields){
                if(error){
                        console.log('mysql error : ' + error);
                }
                else{
                        for(var i=0; i<results.length; i++){
                                dataTemp[i] = results[i].korName;
                        }
			console.log(dataTemp[dataCount]);
			readFileForLearning(dataTemp, dataCount);
                }
        });
}

//파일명(등록된 사용자 이름)에 있는 이미지를 모두 읽어옴
function readFileForLearning(dataTemp, dataCount){
	fs.readdir(commonPath2 + 'faceDetectStorage/' + dataTemp[dataCount] + '/', function(err, files){
		if(!err){
			learningFace(dataTemp, dataCount, files);
		}
	}); 
}

//naverFace API 사용
function naverFace(loadImages, fileName, faceImages){
	var temp;
	var naverImagePath = commonPath2 + 'faceStorageFromClient/' + fileName;
	var face_formData = {
  		image:'image',
  		image: fs.createReadStream(naverImagePath) // FILE 이름
	};

	request({
        method: 'POST',
        uri: face_uri,
        formData: face_formData,
        headers: face_headers
	}, function(error, response, body){
        	if(!error && response.statusCode === 200){
			temp = JSON.parse(body);
			if(temp.faces.length > 0 && temp.faces[0].celebrity.confidence > 0.7){
				console.log("naver result : " + JSON.stringify(temp));
				var naverKorName = temp.faces[0].celebrity.value;
				naverFaceDBCount(naverKorName, loadImages, fileName, faceImages);
			}
		}
	});
}

//CFR API에서 나온 유명인 이름을 갖는 파일명 생성
function naverMakeFolder(naverKorName, loadImages, fileName, faceImages){
	var afterNaverDir = commonPath2 + 'faceDetectStorage/' + naverKorName + '/';
	fs.mkdir(commonPath2 + 'faceDetectStorage/' + naverKorName, function(err) {
                if(err){
			console.log('Dont make folder in faceDetectStorage');
		} 
		else{		
			naverNews(naverKorName, afterNaverDir, loadImages, fileName, faceImages);
		}
        });
}

//naverFace API에서 나온 유명인 이름의 이미지를 학습
function naverFaceLearning(naverKorName, afterNaverDir, loadImages, fileName, faceImages){
	//var faceImages = detector.detectFaces(loadImages);
        var fileNameSplit = fileName.split('.');
	
	for(var i=0; i<faceImages.length; i++){
		fr.saveImage(afterNaverDir + i + '_' + fileNameSplit[0] + '.png', faceImages[i]);
	}
		
	recognizer.addFaces(faceImages, naverKorName);

	var modelStateSerial = recognizer.serialize();
	var wStream = fs.createWriteStream('./model.json');
	wStream.write(JSON.stringify(modelStateSerial));
	wStream.end();
	noRecCount = 0;
	SFRCount = 5;	
	if(webSocketClient !== undefined){
		naverUpdateDB();
	}
}

//1명 인식할 때 자동 학습
function singleFaceLearning(faceImages, learnName){
	recognizer.addFaces(faceImages, learnName);
	var modelStateSerial = recognizer.serialize();
	var wStream = fs.createWriteStream('./model.json');
	wStream.write(JSON.stringify(modelStateSerial));
	wStream.end();
	noRecCount = 0;
	SFRCount--;
}

//Naver New API에서 나온 정보를 DB에 삽입
function naverFaceDBInsert(naverKorName, info1, info2, afterNaverDir, loadImages, fileName, faceImages){
	var querys = 'INSERT INTO info (korName, beforeInfo, afterInfo) VALUES(?, ?, ?)';
	connection.query(querys, [naverKorName, info1, info2], function(error, results, fields){
		if(error){
			console.log('mysql error : ' + error);
                }
                else{
			naverFaceLearning(naverKorName, afterNaverDir, loadImages, fileName, faceImages);
		}	
	});
}

//CFR API에서 나온 유명인 이름이 DB에 있는 이름인지 확인
function naverFaceDBCount(naverKorName, loadImages, fileName, faceImages){
	var querys = 'SELECT korName From info';
	var dataTemp = [];
	connection.query(querys, function(error, results, fields){
		if(error){
			console.log('mysql error : ' + error);
		}
		else{
			for(var i=0; i<results.length; i++){
				dataTemp[i] = results[i].korName;		
			}
			if(!dataTemp.includes(naverKorName)){	
				naverMakeFolder(naverKorName, loadImages, fileName, faceImages);
			}	
		}
	});
}

//Naver New API 사용
function naverNews(naverKorName, afterNaverDir, loadImages, fileName, faceImages){
	var j=0;
	var news_uri = 'https://openapi.naver.com/v1/search/news?query=';
	news_uri = news_uri + encodeURI(naverKorName);
	news_uri = news_uri + "&display=100";
	console.log("url : " + news_uri);
	request({
        	method: 'GET',
        	uri: news_uri,
        	headers: news_headers
	}, function(error, response, body){
		var temp = JSON.parse(body);
		if(!error && response.statusCode === 200 && temp.items.length > 1){
			var info = [];
			for(var i=0; i<temp.items.length; i++){
				if(temp.items[i].title.indexOf(naverKorName) !== -1){
					info[j] = temp.items[i].title;
					info[j] = info[j].replace('<b>','');
					info[j] = info[j].replace('</b>','');
					info[j] = info[j].replace('&quot;',' ');
					info[j] = info[j].replace('&quot',' ');
					j++;
				}
				if(j===2){
					break;
				}
			}
			for(var k=0; k<2; k++){
				if(info[k]=== undefined){
					info[k] = naverKorName + "의 정보를 직접입력하세요.";
				}
			}
			naverFaceDBInsert(naverKorName, info[0], info[1], afterNaverDir, loadImages, fileName, faceImages);
		}
	});
}

// 얼굴감지가 안됐을 때 스마트폰으로 JSON 파일 전송
function noDetectSendJSON(client){	
	// --------------- JSON ---------------
	var object = new Object();
	object.currentDate = getCurrentDate();
	object.currentStat = "Detection Failure";
	var json = JSON.stringify(object);
	client.write(json + "\n", 'utf8');
	// --------------- JSON ---------------
}

// 얼굴인식이 됐을 때 스마트폰으로 JSON 파일 전송
function recognitionSendJSON(client){
	// --------------- JSON ---------------
        var object = new Object();
        object.currentDate = getCurrentDate();
        object.currentStat = "Recognition Success";
        object.predictions = tempPredictions;
        object.information = tempResult;
	object.faceLocation = faceLocation;
       	var json = JSON.stringify(object);
        client.write(json + "\n", 'utf8');
        // --------------- JSON ---------------
} 

// 얼굴 감지 함수
function faceDetection(loadImages, fileName, client){
	var j=0;

	faceLocation = [];

	var result = detector.locateFaces(loadImages).map(res => res.rect);
	
	if(result.length > 0){
		for(var i=0; i<result.length; i++){
			faceLocation[j] = (result[i].top)*(1440/360);
			faceLocation[j+1] = (result[i].bottom)*(1440/360);
			faceLocation[j+2] = (result[i].left)*(2768/480);
			faceLocation[j+3] = (result[i].right)*(2768/480);
			j = j+4; 
		}
	}
	
	var faceImages = detector.getFacesFromLocations(loadImages, result);
	console.log('faceImages count : ' + faceImages.length);

	if(faceImages.length > 0){
		noDetectCount = 0;
		console.log('Face Detection completed');
		faceRecognition(faceImages, loadImages, fileName, client);	
	}
	else{
		console.log('No Face Detection');
		noDetectCount++;
		if(noDetectCount === 200){
			searchDBForLearning(); 
		}
		noDetectSendJSON(client);		
	}
}

// 인식된 사람의 이름이 DB에 있는지 확인
function searchFaceDB(predictions, faceImages, loadImages, fileName, client, tempDir){
	var querys = 'SELECT * FROM info WHERE korName = ?';
	var noPredictions = {className : 'None', distance : 'None'};
	var noResult = {name : 'None', korName : '모름', beforeInfo : '모름', afterInfo : '모름'};
	console.log(predictions.className);
	connection.query(querys, [predictions.className], function(error, results, fields){
		if(error){
			console.log("DB error : " + error);
		}
		else{
			if(results[0] === undefined){
				tempResult[tryCount -1] = noResult;
				tempPredictions[tryCount -1] = predictions;	
			}
			else{
				tempResult[tryCount -1] = results[0];
				tempPredictions[tryCount -1] = predictions;
			}
			
			if(faceImages.length === 1){
				if(webSocketClient !== undefined){
					recogFaceSendImage(tempDir, predictions.className, tempResult[0].beforeInfo, tempResult[0].afterInfo);
				}
				if(SFRCount > 1){
					singleFaceLearning(faceImages, predictions.className);
				}
			}
			if(faceImages.length === tryCount){
				recognitionSendJSON(client);
				clearInformationJSON();		
			}
			else{
				faceRecognition(faceImages, loadImages, fileName, client);
			}
		}
	});	
}

// JSON 전송 시 사용한 변수들 초기화
function clearInformationJSON(){
	tryCount = 0;
        tempPredictions = [];
        tempResult = [];
}

// 얼굴 인식 함수
function faceRecognition(faceImages, loadImages, fileName, client){
	var noPredictions = {className : 'None', distance : 'None'};
	var noResult = {name : 'None', korName : '모름', beforeInfo : '모름', afterInfo : '모름'};
	var fileNameSplit;
	
	tryCount++;

	var predictions = recognizer.predictBest(faceImages[tryCount - 1]);
	
	if(faceImages.length > 0){
		if(predictions.distance <= 0.4){
			noRecCount = 0;
                	console.log("Recognition Success");
                	fileNameSplit = fileName.split('.');
			var tempDir = predictions.className + '/' + predictions.className + '_' + fileNameSplit[0] + '.png';
			fr.saveImage(commonPath2 + 'faceDetectStorage/' + predictions.className + '/' + predictions.className +  '_' + fileNameSplit[0] + '.png', faceImages[tryCount - 1]);
			searchFaceDB(predictions, faceImages, loadImages, fileName, client, tempDir);
			
		}
		else{
			console.log('This face is not existed');
			if(faceImages.length === 1){
				fileNameSplit = fileName.split('.');		
				fr.saveImage(commonPath2 + 'faceDetectStorage/None/' + '_' + fileNameSplit[0] + '.png', faceImages[0]);	
				if(webSocketClient !== undefined){
					noFaceSendImage('_' + fileNameSplit[0] + '.png');
				}
				console.log("------------------------------naver search start-------------------------------------------------- ");
				naverFace(loadImages, fileName, faceImages); 
				noRecCount = 0;
			}
			noRecCount++;
			tempResult[tryCount - 1] = noResult;
			tempPredictions[tryCount -1] = noPredictions;
			if(faceImages.length === tryCount){
                                recognitionSendJSON(client);
				clearInformationJSON();
			}
			else{
				faceRecognition(faceImages, loadImages, fileName, client);
			}
		}	
	}
}

// client로부터 이미지 파일 다운
var server = net.createServer(function(client) {
	console.log('Client connected');

	var writeStream;
	var fileName;
	var fileSize;
	var check;
	var isFileData = false;

	client.on('data', function(data) {
		if(isFileData){
			writeStream.write(data);
			check += data.length;
			if(check === fileSize){
				isFileData = false;
				writeStream.end(function(){
					console.log("다운로드 완료 : " + fileName);
					var loadImages = fr.loadImage(commonPath2 + 'faceStorageFromClient/' + fileName);
					if(webSocketClient !== undefined){
						liveSendImage(fileName);
					}
					faceDetection(loadImages, fileName, client);
				});
			}
		}else{
			isFileData = true;
			check = 0;
			data = JSON.parse(data);
			fileName = data.fileName;
			fileSize = data.fileSize;
			writeStream = fs.createWriteStream(commonPath2 + 'faceStorageFromClient/' + fileName);
			var object = new Object();
			object.fileName = fileName;
			object.fileSize = fileSize;
			var json = JSON.stringify(object);
			client.write(json + "\n", 'utf8');
			
			console.log("전체 크기 : " + fileSize);
		}
	});
	client.on('end', function() {
		console.log('Client disconnected');
	});
});

// 서버 연결과 얼굴 특징값들이 저장되어 있는 model.json 파일 업로드
server.listen(port, function(){
	console.log('connection');
	var start = new Date();
	var modelState = require('./model.json');
	recognizer.load(modelState);
	var end = new Date();
	console.log('JSON upload time : ' + (end.getTime() - start.getTime()) + ' msec');
});

// 웹서버 설정
var webSocketServer = new WebSocketServer({
	httpServer: webServer,
	keepaliveInterval: 10000
});

// 웹페이지에서 넘어온 데이터 처리
webSocketServer.on('request', function(request){
	webSocketClient = request.accept('face-recognition', request.origin);
	webSocketClient.on('message', function(message) {
		var data = JSON.parse(message.utf8Data);
		var tempDir = JSON.stringify(data.fileDir);
		if(tempDir != undefined){
			var fileNameSplit = tempDir.split('/');
			if(data.currentState === "insertNoRecogFaceInfo"){
				if(webSocketClient !== undefined){
					webSearchDuplicateDB(data.name, data.beforeInfo, data.afterInfo, data.fileDir, fileNameSplit[3]);	
				}
			}
			else if(data.currentState === "updateRecogFaceInfo"){
				if(webSocketClient !== undefined){
					webUpdateInfoDB(data.name, data.beforeInfo, data.afterInfo);
				}
			}
		}
  	});
  	webSocketClient.on('error', function(error) {
    		console.log(error); //ignore
  	});
  	webSocketClient.on('close', function(close) {
		console.log(close);
  	});	
});

// client가 전송한 이미지를 웹페이지로 전송(스마트폰이 촬영한 이미지 그대로 전송)
function liveSendImage(fileName){
	var object = new Object();
	object.currentState = "realTimeImage";
	object.fileDir = 'images/faceStorageFromClient/' + fileName;
	console.log(object.fileDir);
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
}

// client가 전송한 이미지에서 얼굴인식 과정을 거친 후 인식이 안된 이미지를 전송
function noFaceSendImage(fileName){
	var object = new Object();
	object.currentState = "noDetectFaceImage";
	object.fileDir = 'images/faceDetectStorage/None/' + fileName;
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
}

// cilent가 전송한 이미지에서 얼굴인식 과정을 거친 후 인식이 된 이미지를 전송
function recogFaceSendImage(fileName, name, info1, info2){
	var object = new Object();
	object.currentState = "recogFaceImage";
	object.currentName = name;
	object.beforeInfo = info1;
	object.afterInfo = info2;
	object.fileDir = 'images/faceDetectStorage/' + fileName;
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
}

// 웹페이지에서 정보 입력 시 입력된 사람 이름이 DB에 있는지 확인
function webSearchDuplicateDB(nameFromWeb, beforeInfo, afterInfo, fileDir, fileName){
        var querys = 'SELECT korName From info';
	var dataTemp = [];
        connection.query(querys, function(error, results, fields){
                if(error){
                        console.log('mysql error : ' + error);
                }
                else{
                        for(var i=0; i<results.length; i++){
                                dataTemp[i] = results[i].korName;
                        }
			if(!dataTemp.includes(nameFromWeb)){
				var loadImages = fr.loadImage(commonPath3 + fileDir);
				if(webSocketClient !== undefined){
					webMakeFolder(nameFromWeb, beforeInfo, afterInfo, fileName, loadImages);
				}
			}
			else{
				if(webSocketClient !== undefined){
					faileWebInsertDB();	
				}
			}	
                }
        });
}

// 웹페이지에서 정보 입력 시 입력된 사람의 파일을 생성(이미지 저장을 위해)
function webMakeFolder(nameFromWeb, beforeInfo, afterInfo, fileName, loadImages) {
	var afterWebDir = commonPath2 + 'faceDetectStorage/' + nameFromWeb + '/';
	fs.mkdir(commonPath2 + 'faceDetectStorage/' + nameFromWeb, function(err) {
		if (err) {
			console.log('Dont make folder in faceDetectStorage');
		}
		else {
			if(webSocketClient !== undefined){
				webInsertInfoDB(nameFromWeb, beforeInfo, afterInfo, afterWebDir, fileName, loadImages);
			}
		}	
	});
}

// 웹페이지에서 정보 입력 시 입력된 정보를 DB에 저장
function webInsertInfoDB(nameFromWeb, beforeInfo, afterInfo, afterWebDir, fileName, loadImages){
	var querys = 'INSERT INTO info (korName, beforeInfo, afterInfo) VALUES(?, ?, ?)';
	connection.query(querys, [nameFromWeb, beforeInfo, afterInfo], function(error, results, fields){
		if(error){
			console.log('mysql error : ' + error);
                }
                else{
			if(webSocketClient !== undefined){
				successWebInsertDB();
				webFaceLearning(nameFromWeb, afterWebDir, loadImages, fileName);
			}
		}	
	});
}

// DB에 정보 삽입 성공 시 웹페이지로 JSON 전송
function successWebInsertDB(){
	var object = new Object();
	object.currentState = "successWebInsertDB";
	object.fileDir = 'images/default.png';
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
} 

// DB에 정보 삽입 실패 시 웹페이지로 JSON 전송
function faileWebInsertDB(){
	var object = new Object();
	object.currentState = "failWebInsertDB";
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
}

// 웹페이지에서 입력된 정보를 통해 이미지 학습
function webFaceLearning(nameFromWeb, afterWebDir, loadImages, fileName){
	var faceImages = detector.detectFaces(loadImages);
	var fileNameSplit = fileName.split('.');

	for(var i=0; i<faceImages.length; i++){
		fr.saveImage(afterWebDir + i + '_' + fileNameSplit[0] + '.png', faceImages[i]);
	}
	recognizer.addFaces(faceImages, nameFromWeb);

	var modelStateSerial = recognizer.serialize();
	var wStream = fs.createWriteStream('./model.json');
	wStream.write(JSON.stringify(modelStateSerial));
	wStream.end();
}

// 웹페이지에서 정보 업데이트 시 업데이트된 정보를 DB에 저장
function webUpdateInfoDB(nameFromWeb, beforeInfo, afterInfo){
	var querys = 'UPDATE info set korName = ?, beforeInfo = ?, afterInfo = ? WHERE korName = ?';
        connection.query(querys, [nameFromWeb, beforeInfo, afterInfo, nameFromWeb], function(error, results, fields){
                if(error){
                        console.log('mysql error : ' + error);
			if(webSocketClient !== undefined){
				failWebUpdateDB();	
                	}
		}
                else{
			if(webSocketClient !== undefined){
				successWebUpdateDB();
                	}
		}
        });
}

// DB에 정보 업데이트 성공 시 웹페이지로 JSON 전송
function successWebUpdateDB(){
	var object = new Object();
	object.currentState = "successWebUpdateDB";
	object.fileDir = 'images/default.png';
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
} 

// DB에 정보 업데이트 실패 시 웹페이지로 JSON 전송
function failWebUpdateDB(){
	var object = new Object();
	object.currentState = "failWebUpdateDB";
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
}

// naver API가 모두 성공적으로 작동 시 웹페이지로 JSON 전송 
function naverUpdateDB(){
	var object = new Object();
	object.currentState = "successNaverUpdateDB";
	object.fileDir = 'images/default.png';
	var json = JSON.stringify(object);
	webSocketClient.sendUTF(json);
}

webServer.listen(webPort, function(){
	console.log("웹 서버 시작");
});

