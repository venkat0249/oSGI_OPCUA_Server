# oSGI_OPCUA_Server
OSGI compatible OPCUA Server based on Milo library

**Notes:**
* with default code without any customization,
Upon activating the bundle, the opcua server builds and start itself on this address: opc.tcp://127.0.0.1:12000/iEMS_OPCUA_Server
* Application name or the port can be changed by passing desired values to 'ActuatorClientImpl' constructor. 
**ActuatorClientImpl(String serverEndpointName, int port, String jsonFile)**
* Nodes of the server can be customized in by drafting a json file and pass it to the 'ActuatorClientImpl' constructor
* If no JSON file is passed, then the server will start with default nodes that were hardcoded in the SensorsNamespace class
* *org.fortiss.smg.oSGI.opcuaserver.impl* package contains all OPCUA related classes and *org.fortiss.smg.oSGI.opcuaserver.jsonreader* contains json parsing/decoding classes
