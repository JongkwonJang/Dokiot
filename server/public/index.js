var IP = '106.10.38.88';
//var PORT = 17000;
var noDetectFaceDirForDB;
var recogFaceDirForDB;

$('#center-button').on('click', function() {
  var name = $('#center-name').val();
  var beforeInformation = $('#center-beforeInformation').val();
  var afterInformation = $('#center-afterInformation').val();
  console.log(name + ", " + beforeInformation + ", " + afterInformation + " ");
  var object = new Object();
  object.currentState = "insertNoRecogFaceInfo";
  object.name = name;
  object.beforeInfo = beforeInformation;
  object.afterInfo = afterInformation;
  object.fileDir = noDetectFaceDirForDB;
  var json = JSON.stringify(object);
  send(json);
})

$('#bottom-button').on('click', function() {
  var name = $('#bottom-name').val();
  var beforeInformation = $('#bottom-beforeInformation').val();
  var afterInformation = $('#bottom-afterInformation').val();
  console.log(name + ", " + beforeInformation + ", " + afterInformation + " ");
  var object = new Object();
  object.currentState = "updateRecogFaceInfo";
  object.name = name;
  object.beforeInfo = beforeInformation;
  object.afterInfo = afterInformation;
  object.fileDir = recogFaceDirForDB;
  var json = JSON.stringify(object);
  send(json);
})

$(document).ready(function() {
  var temp1 = document.location.href;
  var temp2 = temp1.split(':');
  console.log(temp2[1] + 'aa  ' + temp2[2]);
  var temp3 = temp2[2].split('/');
  console.log(temp3[0] + 'bb ' + temp3[1]);
  var port = parseInt(temp3[0]);
  console.log(port + ' E');
  webSocket = new WebSocket("ws://" + IP + ":" + port, 'face-recognition');
  $('#center-name').val('');
  $('#center-beforeInformation').val('');
  $('#center-afterInformation').val('');
  $('#bottom-center-label1').val('');
  $('#bottom-center-label2').val('');
  $('#bottom-center-label3').val('');
  $('#bottom-name').val('');
  $('#bottom-beforeInformation').val('');
  $('#bottom-afterInformation').val('');
  webSocket.onopen = function(open) {
    console.log("WebSocket Connected - Port : " + port);
  };
  webSocket.onerror = function() {
    console.log("WebSocket Error");
  };
  webSocket.onclose = function(close) {
    console.log("WebSocket Close : " + close.code);
  };
  webSocket.onmessage = function(data) {
    console.log(data.data);
    changeImg(data.data);
    /*readData(data.data);*/
  };
});

function send(message) {
  webSocket.send(message, 'utf8');
}

function startJSON(){
  var object = new Object();
  object.name = "Start";
  object.beforeInformation = "Start";
  object.afterInformation = "start";
  var json = JSON.stringify(object);
  send(json);
}

function changeImg(data){
  console.log("test " + data);
  var data = JSON.parse(data);
  if(data.currentState === "realTimeImage"){
    $('#center-left-img').attr('src', data.fileDir);
  }
  else if(data.currentState === "noDetectFaceImage"){
    $('#center-center-img').attr('src', data.fileDir);
    noDetectFaceDirForDB = data.fileDir;
  }
  else if(data.currentState === "recogFaceImage"){
    $('#center-center-img').attr('src', "images/default.png");
    $('#bottom-left-img').attr('src', data.fileDir);
    $('#bottom-name').val(data.currentName);
    $('#bottom-center-label1').val(data.currentName);
    $('#bottom-center-label2').val(data.beforeInfo);
    $('#bottom-center-label3').val(data.afterInfo);
    recogFaceDirForDB = data.fileDir;
  }
  else if(data.currentState === "successWebInsertDB"){
    $('#center-center-img').attr('src', data.fileDir);
    $('#toast').addClass('show');
    $('#toast').text("입력이 성공적으로 완료되었습니다.");
    $('#center-name').val('');
    $('#center-beforeInformation').val('');
    $('#center-afterInformation').val('');
    setTimeout(function (){
       $('#toast').removeClass('show');
    }, 3000);
  }
  else if(data.currentState === "successWebUpdateDB"){
    $('#bottom-left-img').attr('src', data.fileDir);
    $('#toast').addClass('show');
    $('#toast').text("업데이트가 성공적으로 완료되었습니다.");
    $('#bottom-center-label1').val('');
    $('#bottom-center-label2').val('');
    $('#bottom-center-label3').val('');
    $('#bottom-name').val('');
    $('#bottom-beforeInformation').val('');
    $('#bottom-afterInformation').val('');
    setTimeout(function (){
       $('#toast').removeClass('show');
    }, 3000);
  }
  else if(data.currentState === "failWebInsertDB"){
    $('#toast').text("해당 이름이 있습니다. 다시 입력하세요.");
    $('#toast').addClass('show');
    setTimeout(function (){
       $('#toast').removeClass('show');
    }, 3000);
  }
  else if(data.currentState === "failWebUpdateDB"){
    $('#toast').text("업데이트가 실패했습니다. 다시 입력하세요.");
    $('#toast').addClass('show');
    setTimeout(function (){
       $('#toast').removeClass('show');
    }, 3000);
  }
  else if(data.currentState === "successNaverUpdateDB"){
    $('#center-center-img').attr('src', data.fileDir);
    $('#toast').addClass('show');
    $('#toast').text("Naver API가 성공적으로 완료되었습니다.");
    setTimeout(function (){
       $('#toast').removeClass('show');
    }, 3000);
  }
}
