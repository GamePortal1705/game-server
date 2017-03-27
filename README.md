# GamePortal backend server

## server side event specification

EventName  | Type | Description
------------- | ------------- | ------------- 
systemInfo  | emit | send Message that contains ID valued -1 and data a system log String
joinGame  | on | wait for Message contains playerName and JoinGameMsg
dispatchRole  | emit | send Message contains ID, playerName, sessionID and DispatchRoleMsg
night  | emit | send Message contains ID, playerName, sessionID and round number
kill  | on | wait for Message that contains data valued the player ID to kill
killDecision  | emit | send Message contains data valued the killed player ID
makeStatement  | emit | send Message contains current statement playerName and ID, and null data
finishStatement  | on | wait for Message that contains ID identifying the player that finished statement
vote  | emit | send broadcast Message contains null data


