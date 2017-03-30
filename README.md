# GamePortal backend server

## Usage

~~~bash
$ mvn clean 
$ mvn package
$ java -jar ./target/game-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar NUM_OF_PLAYERS
~~~

## Server Side Event Specification

EventName  | Type | Description
------------- | ------------- | ------------- 
systemInfo  | emit | send Message that contains ID valued -1 and data a system log String
joinGame  | on | wait for Message contains playerName and JoinGameMsg
dispatchRole  | emit | send Message contains ID, playerName, sessionID and DispatchRoleMsg
night  | emit | send broadcast Message that contains a HashMap including <id, isAlive> mapping
vote  | emit | send broadcast Message contains a HashMap including <id, isAlive> mapping
kill  | on | wait for Message that contains data valued the player ID to kill
killDecision  | emit | send Message contains data valued the killed player ID
makeStatement  | emit | send broadcast Message contains current statement playerName and ID, and round number
finishStatement  | on | wait for Message that contains ID identifying the player that finished statement
gameOver  | emit | send broadcast Message contains data of GameOverMsg. GameOverMsg includes list of players (Person object) and winning side.


