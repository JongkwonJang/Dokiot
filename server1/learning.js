var net = require('net');
var fs = require("fs");
var fr = require('face-recognition');
var mysql = require('mysql');
var path = require('path');

var commonPath2 = 'public/images/';
var commonPath3 = 'public/';
var detector = fr.FaceDetector();
var recognizer = fr.FaceRecognizer();
var readCount = 0;
var temp = [];
var loadCount = 0;
var learnCount = 0;
var connection = mysql.createConnection({
	host : '127.0.0.1',
	user : 'root',
	password : 'root',
	database : 'face_recognition',
	ssl : false
});

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
learnFace();
function learnFace(){
	fs.readdir('public/images/faceDetectStorage/', function(err, files){
		if(!err){
			console.log('파일 개수 : ' + files.length);
			readImg(files);
		}
	});
		
}
function readImg(files){
	fs.readdir('public/images/faceDetectStorage/' + files[learnCount] + '/', function(err, img){
		if(!err){
			console.log(img[loadCount]);
			var dirFile = fr.loadImage('public/images/faceDetectStorage/' + files[learnCount] + '/' + img[loadCount]);
			var faceImages = detector.detectFaces(dirFile);
			
			if(faceImages.length > 0 ){
				
				if(loadCount !== img.length - 1){
					recognizer.addFaces(faceImages, files[learnCount]);
					loadCount++;
					readImg(files);
				}
				else{
					dbInsert(files);
				}
			}
		}
	});
}

function dbInsert(files){
	var querys = 'INSERT INTO info (korName, beforeInfo, afterInfo) VALUES(?, ?, ?)';
	var name = files[learnCount];
	connection.query(querys, [name, learnCount, learnCount], function(error, results, fields){
		if(error){
			console.log('mysql error : ' + error);
                }
                else{
			console.log('LearnC : ' + learnCount);
			console.log('LoadC : ' + loadCount);
			console.log(files[learnCount]);
			learnCount++;
			loadCount = 0;
			if(learnCount !== files.length){
				readImg(files);
			}
			else{
					var modelStateSerial = recognizer.serialize();
					var wStream = fs.createWriteStream('./model.json');
					wStream.write(JSON.stringify(modelStateSerial));
					wStream.end();
			}
		}
	});	
}

/*
faceDetect();
function faceDetect(){
	var dirFile = fr.loadImage('public/images/Park.jpg');
	var faceImages = detector.detectFaces(dirFile);
	if(faceImages.length < 2){
		console.log('얼굴감지완료');
       		fs.mkdir('public/images/faceDetectStorage/Park', function(err) {
               		if (err) {
                       		console.log('Dont make folder in faceDetectStorage');
               		}
               		else {
				fr.saveImage('public/images/faceDetectStorage/Park/' + getCurrentDate() + '.png', faceImages[0]);
				readCount++;
				if(readCount !== temp.length){
					console.log(readCount);
					readDir(temp[readCount]);
               			}
			} 
       		});
	}
	else{
		readCount++;
		if(readCount !== temp.length){
			console.log(readCount);
			readDir(temp[readCount]);
		}
	}
}*/
/*
function readDir(files){
	fs.readdir('public/images/faceDetectStorage/lfw/' + files, function(err, img){
		if(!err){
			console.log(files);
			console.log(img[0]);
			var dirFile = fr.loadImage('public/images/faceDetectStorage/lfw/' + files + '/' + img[0]);
			faceDetect(files, dirFile)
		}
	});

}
	fs.readdir('public/images/faceDetectStorage/lfw/', function(err, files){
		if(!err){
			temp = files;
			console.log('파일 개수 : ' + files.length);
			console.log(files[0]);
			if(readCount !== files.length){
				readDir(files[readCount]);
			}
			//for(var i=0; i<3; i++){
			//	fs.readdir('public/images/faceDetectStorage/lfw/' + files[i], function(err, img){
			//		if(!err){
			//			console.log(i + ' ' + files[i]);
			//			console.log(img[0]);
			//			var dirFile = fr.loadImage('public/images/faceDetectStorage/lfw/' + files[i] + '/' + img[0]);
			//		}
			//	});
			//}
		}
	});*/
