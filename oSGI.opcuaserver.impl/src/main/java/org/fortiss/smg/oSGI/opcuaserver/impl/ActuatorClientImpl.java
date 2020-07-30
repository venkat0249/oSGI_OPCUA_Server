/** Authors: Venkatesh Pampana (fortiss GmbH) and Daniel Lavin
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 * **/

package org.fortiss.smg.oSGI.opcuaserver.impl;

import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ActuatorClientImpl {
	//message logging slf4j
	private static org.slf4j.Logger logger = LoggerFactory
			.getLogger(ActuatorClientImpl.class);
	
	private ScheduledExecutorService executor;
	private int port;
	private String feedback = null;
	private startOPCUAServer opcuaServer;
	private String endpointUlr;
	private String filepath = null;

	
	
	public ActuatorClientImpl(String serverEndpointName, int port, String jsonFile) {
		this.endpointUlr = serverEndpointName;
		this.port = port;
		this.feedback = jsonFile; //path to json build file
		//activate();
	}
	

	public synchronized void activate() {
		logger.debug("OPCUASERVER: Activating...");
		try {
			if(!feedback.isEmpty()) {
				int delimiter = feedback.indexOf(":///");
				if(delimiter == -1) {
					logger.error("OPCUASERVER: error parsing json file");
				} else {
					String infotype = feedback.substring(0,delimiter);
					if(infotype.contains("file")) {
						this.filepath = feedback.substring(delimiter + ":///".length());
						logger.debug("OPCUASERVER: parsed " + infotype + ":" + this.filepath);
					} 
				}
			}
			
			startServer(); //will decide how to construct the namespace. If no JSON file is passed,server start with default nodes
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** This method decides how to construct the OPCUA Server['s namespace]. 
	 * If a filepath is provided, it will attempt to build it from a json-formatted file which contains the node display names and initial values.
	 * Otherwise, it will default to sample nodes
	 * 
	 * NOTE: in the filepath method, there are is no exception handling, so if an incorrect/invalid/nonexistent filepath
	 * is provided, the whole thing will crash (filenotfoundexception). Careful!
	 * **/
	
	public void startServer() throws Exception {
		executor = Executors.newSingleThreadScheduledExecutor();
		if(filepath != null) {
			logger.info("OCUASERVER: Building from " + filepath);
			opcuaServer = new startOPCUAServer(this, filepath);
			executor.execute(opcuaServer); //start (build) OPCUA from file if filepath is provided
			logger.debug("OPCUASERVER: execute ");
		}
		else
		{
			logger.info("OCUASERVER: Building from default server");
			opcuaServer = new startOPCUAServer(this);
			executor.execute(opcuaServer); 
			logger.debug("OPCUASERVER: execute ");
		}
	}
	

	/** Deactivate Bundle
	 * **/
	
	public synchronized void deactivate() {
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public int getPort() {
		return this.port;
	}
	
	public String getEndpointUrl() {
		return this.endpointUlr;
	}

    
}
