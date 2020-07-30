package org.fortiss.smg.oSGI.opcuaserver.jsonreader;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.fortiss.smg.oSGI.opcuaserver.jsonreader.MyFolderNode;
import  org.fortiss.smg.oSGI.opcuaserver.jsonreader.MyVariableNode;

public class ConfigInterface {

	private List<MyFolderNode> folderNodeList = new ArrayList<>();
	private List<MyVariableNode> variableNodeList = new ArrayList<>();


	public List<MyFolderNode> getFolderNodeList() {
		return folderNodeList;
	}

	public List<MyVariableNode> getVariableNodeList() {
		return variableNodeList;
	}

}
