package org.fortiss.smg.oSGI.opcuaserver.impl;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import com.google.common.collect.Lists;

import org.fortiss.smg.oSGI.opcuaserver.jsonreader.MyFolderNode;
import org.fortiss.smg.oSGI.opcuaserver.jsonreader.MyVariableNode;
import org.fortiss.smg.oSGI.opcuaserver.jsonreader.ConfigInterface;
import org.fortiss.smg.oSGI.opcuaserver.jsonreader.GenericJsonReader;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.Namespace;
import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.NodeFactory;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.slf4j.LoggerFactory;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import com.google.gson.stream.JsonReader;


//this is the namespace where we store nodes related to sensors
public class SensorsNamespace implements Namespace {
	
	public static final String NAMESPACE_URI = "urn:smg:actuatorclient:opcua:server:sensors";
	
	private static org.slf4j.Logger logger = LoggerFactory
			.getLogger(SensorsNamespace.class);
	private final Random random = new Random();

    private final SubscriptionModel subscriptionModel;

    private final NodeFactory nodeFactory;

    private final OpcUaServer server;
    private final UShort namespaceIndex;
    private final String filepath;
    private ConfigInterface configInt;
    private ArrayList<AnalogItemNode> analogNodeList = new ArrayList<AnalogItemNode>();
    private List<UaFolderNode> folderNodesList;

    //constructor of the class. build namespace with default nodes
    public SensorsNamespace(OpcUaServer server, UShort namespaceIndex) {
        this.server = server;
        this.namespaceIndex = namespaceIndex;
        this.filepath = null;

        subscriptionModel = new SubscriptionModel(server, this);

        nodeFactory = new NodeFactory(
            server.getNodeMap(),
            server.getObjectTypeManager(),
            server.getVariableTypeManager()
        );

        try {
            // Create a "Sensors" folder and add it to the node manager
            NodeId folderNodeId = new NodeId(namespaceIndex, "Sensors");

            UaFolderNode folderNode = new UaFolderNode(
                server.getNodeMap(),
                folderNodeId,
                new QualifiedName(namespaceIndex, "Sensors"),
                LocalizedText.english("Sensors")
            );

            server.getNodeMap().addNode(folderNode);

            // Make sure our new folder shows up under the server's Objects folder
            server.getUaNamespace().addReference(
                Identifiers.ObjectsFolder,
                Identifiers.Organizes,
                true,
                folderNodeId.expanded(),
                NodeClass.Object
            );

            // Add the nodes/objects to this namespace
            addNodes(folderNode);
            
        } catch (UaException e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
}
    
    //Second constructor, used if json build file path is provided
    public SensorsNamespace(OpcUaServer server, UShort namespaceIndex, String filepath) {
        this.server = server;
        this.namespaceIndex = namespaceIndex;
        this.filepath = /*Paths.get(System.getProperty("user.dir")) .toString() 
        the commented line might return a different string depending how and where
        the application is executed, therefore its better to provide ABSOLUTE 
        and not relative paths.*/ filepath;
        
        logger.debug("Received filepath: " + this.filepath);

        subscriptionModel = new SubscriptionModel(server, this);

        nodeFactory = new NodeFactory(
            server.getNodeMap(),
            server.getObjectTypeManager(),
            server.getVariableTypeManager()
        );

        try {
            
        	//Read the json file and create a configuration interface with it
        	
            JsonReader jsonReader = new JsonReader(new FileReader(this.filepath));	
        	this.configInt = GenericJsonReader.createConfig(jsonReader);
            
        	//these two methods use the configInt to generate the folder nodes and the subnodes
        	
            addFolderNodes();
            addJsonNodes();
        } catch (Exception e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
}    
  
    
    private void addFolderNodes() {
    	folderNodesList = new ArrayList<UaFolderNode>();
    	
    	
    	// System.out.println("Begin configuration of server from created Configuration");
    	// achtung reihenfolge wird wichtig sein!!!
    	
    	
    	try {
    		
	    	for(MyFolderNode node : configInt.getFolderNodeList() )	{ //configInt created with the JsonReader
	    		
		    	//System.out.println("Folder: nodeID="+node.nodeID+" namespace=" + node.namespace);
		    	//System.out.println("Folder: nodeDescription="+node.description);	

				NodeId parent = null;
				if (node.nodeID.contains("/")) {
					String[] buffer = node.nodeID.split("/");
					
					String lookUpString = "";
					
					if (buffer.length > 1) {
						lookUpString = buffer[0];
					}
					for (int i = 1; i < buffer.length-1; i++) {
						lookUpString += "/" + buffer[i];
					}					
					
					ServerNodeMap serverNodeMap = server.getNodeMap();
					for (NodeId _nodeId : serverNodeMap.keySet()) {
						if (_nodeId.getIdentifier().toString().contentEquals(lookUpString)) {
							parent = _nodeId;
						}
					}
				}
			
            	NodeId folderNodeId = new NodeId(namespaceIndex, node.nodeID);

            	// DisplayName is not the full path
            	String testString = node.nodeID;            	
            	String[] testBuffer = testString.split("/");
            	testString = testBuffer[testBuffer.length-1];
            	
            	UaFolderNode folderNode = new UaFolderNode(
	                server.getNodeMap(),
	                folderNodeId,
	                new QualifiedName(namespaceIndex, node.displayName),
	                LocalizedText.english(testString)
	            );

            	server.getNodeMap().addNode(folderNode);

	        	// Make sure our new folder shows up under the server's Objects folder                
	            NodeId source = parent == null ? Identifiers.ObjectsFolder : parent;                
	            server.getUaNamespace().addReference(
	                source,
	                Identifiers.Organizes,
	                true,
	                folderNodeId.expanded(),
	                NodeClass.Object
	            );
            
            	folderNodesList.add(folderNode);
            	
            	//System.out.println();
	    	}
    	} catch (UaException e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
    	
    }
    
    
    private void addJsonNodes() {   	
    	try {

    		for(MyVariableNode node : configInt.getVariableNodeList()){	  //configInt created with the JsonReader		
    			//logger.info("Variable nodeID: " + node.nodeID);
    			NodeId nodeId = new NodeId(namespaceIndex, node.nodeID);

    			AnalogItemNode itemNode = nodeFactory.createVariable(
    					nodeId,
    					new QualifiedName(namespaceIndex, node.displayName),
    					LocalizedText.english(node.displayName),
    					Identifiers.AnalogItemType,
    					AnalogItemNode.class
    					);

    			itemNode.setAccessLevel( ubyte(node.getAccessLevelAsInt()) );
    			itemNode.setUserAccessLevel( ubyte(node.getAccessLevelAsInt()) );
    			itemNode.setDataType(node.getDataType()); //TODO: this might be an example on how to dynamically get the datatype for the nodes
    			itemNode.setDescription(LocalizedText.english(node.description));
    			itemNode.setValue(new DataValue(new Variant(node.getValueWithDataType())));
    			//node1.setEURange(new Range(0.0, 100.0));
    			itemNode.setMinimumSamplingInterval(node.getMinSamplingInterval());
    			itemNode.setHistorizing(node.isHistorizing());	

    			server.getNodeMap().addNode(itemNode); 
    			// this list directly contains all itemNodes, to allow access from any JAVA class
    			analogNodeList.add(itemNode);

    			// Make sure our new folder shows up under the server's Objects folder

    			NodeId parent = null;
    			if (node.nodeID.contains("/")) {
    				String[] buffer = node.nodeID.split("/");

    				String lookUpString = "";

    				if (buffer.length > 1) {
    					lookUpString = buffer[0];
    				}
    				for (int i = 1; i < buffer.length-1; i++) {
    					lookUpString += "/" + buffer[i];
    				}					

    				ServerNodeMap serverNodeMap = server.getNodeMap();
    				for (NodeId _nodeId : serverNodeMap.keySet()) {
    					if (_nodeId.getIdentifier().toString().contentEquals(lookUpString)) {
    						parent = _nodeId;
    					}
    				}
    			}

    			NodeId source = parent == null ? Identifiers.ObjectsFolder : parent;
    			server.getUaNamespace().addReference(
    					source,
    					Identifiers.Organizes,
    					true,
    					nodeId.expanded(),
    					NodeClass.Object
    					);
    		}
    	} catch (UaException e) {
    		e.printStackTrace();
    	}    	
    } 

private void addNodes(UaFolderNode rootNode) {
	// DataAccess folder
	UaFolderNode TemperaturesFolder = new UaFolderNode(
			server.getNodeMap(),
			new NodeId(namespaceIndex, "Temperatures"),
			new QualifiedName(namespaceIndex, "Temperatures"),
			LocalizedText.english("Temperatures")
			);

	server.getNodeMap().addNode(TemperaturesFolder);
	rootNode.addOrganizes(TemperaturesFolder);

	// AnalogItemType node
	AnalogItemNode node1 = nodeFactory.createVariable(
			new NodeId(namespaceIndex, "Temperature_1"),
			new QualifiedName(namespaceIndex, "Temperature_1"),
			LocalizedText.english("Temperature_1"),
			Identifiers.AnalogItemType,
			AnalogItemNode.class
			);
	node1.setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)));
	node1.setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)));
	node1.setDataType(Identifiers.Double);
	node1.setValue(new DataValue(new Variant(3.14d)));

	node1.setEURange(new Range(0.0, 100.0));

	server.getNodeMap().addNode(node1);
	TemperaturesFolder.addOrganizes(node1);

	//Add node which returns datavalue based on your implemented logic in the delegate
	AnalogItemNode node2 = nodeFactory.createVariable(
			new NodeId(namespaceIndex, "Temperature_2"),
			new QualifiedName(namespaceIndex, "Temperature_2"),
			LocalizedText.english("Temperature_2"),
			Identifiers.AnalogItemType,
			AnalogItemNode.class
			);

	node2.setDataType(Identifiers.Double);
	node2.setValue(new DataValue(new Variant(3.15d)));

	AttributeDelegate delegate = AttributeDelegateChain.create(
			new AttributeDelegate() {
				@Override
				public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
					//**** Write your  datavalue logic here and return ****
					//dosomething()
					return new DataValue(new Variant(random.nextDouble()));
				}
			},
			ValueLoggingDelegate::new
			);

	node2.setAttributeDelegate(delegate);

	node2.setEURange(new Range(0.0, 100.0));

	server.getNodeMap().addNode(node2);
	TemperaturesFolder.addOrganizes(node2);

	//Add node which sets the datavalue based on your implemented logic in the delegate2 
	AnalogItemNode node3 = nodeFactory.createVariable(
			new NodeId(namespaceIndex, "Switch"),
			new QualifiedName(namespaceIndex, "Switch"),
			LocalizedText.english("Switch"),
			Identifiers.AnalogItemType,
			AnalogItemNode.class
			);
	node3.setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)));
	node3.setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)));
	node3.setDataType(Identifiers.Double);
	node3.setValue(new DataValue(new Variant(3.14d))); // initial value
	AttributeDelegate delegate2 = AttributeDelegateChain.create(
			new AttributeDelegate() {
				@Override
				public void setValue(AttributeContext context, VariableNode node, DataValue value) throws UaException {
					//set value of the node that has been received from a client
					node.setValue(value);
					//**** Write your  datavalue logic (eg. may be turn on the switch ****
					//dosomething()
				}
			},
			ValueLoggingDelegate::new
			);

	node3.setAttributeDelegate(delegate2);


	server.getNodeMap().addNode(node3);
	TemperaturesFolder.addOrganizes(node3);
}

    @Override
    public CompletableFuture<List<Reference>> browse(AccessContext context, NodeId nodeId) {
        ServerNode node = server.getNodeMap().get(nodeId);

        if (node != null) {
            return CompletableFuture.completedFuture(node.getReferences());
        } else {
            return FutureUtils.failedFuture(new UaException(StatusCodes.Bad_NodeIdUnknown));
        }
    }
    /**
     * methodoverwrite: this method is called when ever there a client try to read a node from this server namespace
     * 
     */
    @Override
    public void read(
        ReadContext context,
        Double maxAge,
        TimestampsToReturn timestamps,
        List<ReadValueId> readValueIds) {

        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId readValueId : readValueIds) {
            ServerNode node = server.getNodeMap().get(readValueId.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(
                    new AttributeContext(context),
                    readValueId.getAttributeId(),
                    timestamps,
                    readValueId.getIndexRange(),
                    readValueId.getDataEncoding()
                );

                results.add(value);
                //for some reason, this logger info is not displaying in console output of eclipse. don't panic if this info is not displayed in console window
                //logger.info("read value={} for node {}", value.getValue(), node.getBrowseName());
            } else {
                results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    /**
     * methodoverwrite: this method is called when ever there a client try to write a node value into this server namespace
     * generally you dont have to touch this method unless you wanna perform some advanced operations.
     */
    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            ServerNode node = server.getNodeMap().get(writeValue.getNodeId());

            if (node != null) {
                try {
                    node.writeAttribute(
                        new AttributeContext(context),
                        writeValue.getAttributeId(),
                        writeValue.getValue(),
                        writeValue.getIndexRange()
                    );

                    results.add(StatusCode.GOOD);
                  //for some reason, this logger info is not displaying in console output of eclipse. don't panic if this info is not displayed in console window
//                    logger.info(
//                        "Wrote value {} to {} attribute of {}",
//                        writeValue.getValue().getValue(),
//                        AttributeId.from(writeValue.getAttributeId()).map(Object::toString).orElse("unknown"),
//                        node.getNodeId());
                } catch (UaException e) {
                    logger.error("Unable to write value={}", writeValue.getValue(), e);
                    results.add(e.getStatusCode());
                }
            } else {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public Optional<MethodInvocationHandler> getInvocationHandler(NodeId methodId) {
        Optional<ServerNode> node = server.getNodeMap().getNode(methodId);

        return node.flatMap(n -> {
            if (n instanceof UaMethodNode) {
                return ((UaMethodNode) n).getInvocationHandler();
            } else {
                return Optional.empty();
            }
        });
}
    @Override
    public UShort getNamespaceIndex() {
        return namespaceIndex;
    }

    @Override
    public String getNamespaceUri() {
        return NAMESPACE_URI;
}

    
}
