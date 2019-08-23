var express = require('express');
var app = express();
var exec = require('child_process').exec;
var mysql = require('mysql');

var connection = mysql.createConnection({
        host : '127.0.0.1',
        user : 'root',
        password : 'root',
        database : 'client_information',
        ssl : false
});

app.get('/', (req, res) => {
  	//res.params.id
  	var reqType = req.query.type;
	var reqPort = req.query.port;
	var resResult = 'error';
  

 	if(reqType == 'connect'){
    	searchMinPort('con', res);
		resResult = 'conn';
  	}
	else if(reqType == 'disconnect'){
		stopContainer(reqPort, res);
		resResult = 'discon';
  	}
	else{
    	resResult = 'error_undefinedType';
		res.send(resResult);
  	}
  	//console.log(reqType);

  	//res.send(resResult); // 나중에 느리면 이 기능을 뒤에 함수에 넣자
});

function requestWebpage(cPort){
	exec("curl -d http://106.10.38.88:" + cPort +'/', function(error, stdout, stderr){
		console.log('stdout : ' + stdout);
	});
}

function createContainer(cPort, res){
	exec("docker run -d --name " + cPort + " --net host server node server.js " + cPort, function(error, stdout, stderr){
		console.log('stdout : ' + stdout);
		//console.log('stderr : ' + stderr);

		if(error){
			console.log('exec error : ' + error);
			res.send('error_createContainer');
		}
		else{
			requestWebpage(cPort);
			insertDB(cPort, res);
			res.send(cPort.toString());
			//insertDB(cPort);
		}
	});
}
function stopContainer(cPort, res){
	exec("docker stop " + cPort, function(error, stdout, stderr){
            console.log('stdout : ' + stdout);
            //console.log('stderr : ' + stderr);

            if(error){
                console.log('exec error : ' + error);
				res.send('error_stopContainer');
            }
			else{
				//res.send(cPort);
                deleteContainer(cPort, res);
				
			}
        });
}

function deleteContainer(cPort, res){
	exec("docker rm " + cPort, function(error, stdout, stderr){
		console.log('stdout : ' + stdout);
		//console.log('stderr : ' + stderr);

		if(error){
			console.log('exec error : ' + error);
			res.send('error_deleteContainer');
		}
		else{
			deleteDB(cPort, res);
		}
	});
}

function searchMinPort(state, res){
	var querys = 'SELECT min(port+1) as cPort FROM clientPort WHERE (port+1) NOT IN (SELECT port FROM clientPort)';
        connection.query(querys, function(error, results, fields){
		if(error){
            console.log("DB error : " + error);
		}
		else{
			console.log(results[0].cPort);
    		createContainer(results[0].cPort, res);
		}
	});
}

function insertDB(cPort, res){
    var querys = 'INSERT INTO clientPort (port) VALUES(?)';
	connection.query(querys, [cPort], function(error, results, fields){
		if(error){
            console.log('mysql error : ' + error);
            res.send('error_insertDB');
        }
        else{
			console.log('연결과정 완료');
        }
    });
}

function deleteDB(cPort, res){
        var querys = 'DELETE FROM clientPort WHERE port = ?';
		connection.query(querys, [cPort], function(error, results, fields){
			if(error){
            	console.log('mysql error : ' + error);
            	res.send('error_deleteDB');
        	}
        	else{
				console.log('종료과정 완료');
				res.send('disconnect complete');
       		}
    	});
}
app.listen(9990, () => {
  console.log('Example app listening on port 9990!');
});
