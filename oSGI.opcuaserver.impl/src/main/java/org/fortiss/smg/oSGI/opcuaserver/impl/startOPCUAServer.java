package org.fortiss.smg.oSGI.opcuaserver.impl;
import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;

//import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DirectoryCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.fortiss.smg.oSGI.opcuaserver.impl.ActuatorClientImpl;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;



public class startOPCUAServer implements Runnable{
	private static org.slf4j.Logger logger = LoggerFactory
			.getLogger(startOPCUAServer.class);

	private ActuatorClientImpl impl;
	private String filepath = null;
	@Override
	public void run() {
		
		try {

			server.startup().get();
			final CompletableFuture<Void> future = new CompletableFuture<>();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

			future.get();

		}  catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getStackTrace().toString());
			e.printStackTrace();
		}
		

	}
	

private final OpcUaServer server;

/**
 * First constructor: start bare minimum opc server with sample nodes
 * **/

public startOPCUAServer(ActuatorClientImpl impl) throws Exception {
    File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");
    if (!securityTempDir.exists() && !securityTempDir.mkdirs()) {
        throw new Exception("unable to create security temp dir: " + securityTempDir);
    }
    LoggerFactory.getLogger(getClass()).info("security temp dir: {}", securityTempDir.getAbsolutePath());
//    KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

    DefaultCertificateManager certificateManager = new DefaultCertificateManager();
//    DefaultCertificateManager certificateManager = new DefaultCertificateManager(
//            loader.getServerKeyPair(),
//            loader.getServerCertificateChain()
//);

    File pkiDir = securityTempDir.toPath().resolve("pki").toFile();
    DirectoryCertificateValidator certificateValidator = new DirectoryCertificateValidator(pkiDir);
    LoggerFactory.getLogger(getClass()).info("pki dir: {}", pkiDir.getAbsolutePath());

    UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
        true,
        authChallenge -> {
            String username = authChallenge.getUsername();
            String password = authChallenge.getPassword();
            //it's configured with only 2 users at the moment.  user and admin.
            boolean userOk = "user".equals(username) && "password1".equals(password);
            boolean adminOk = "admin".equals(username) && "password2".equals(password);

            return userOk || adminOk;
        }
    );

    X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);
    //if you would like to assign custom host, here is this place
    List<String> bindAddresses = newArrayList();
    bindAddresses.add("0.0.0.0");

    List<String> endpointAddresses = newArrayList();
    endpointAddresses.add(HostnameUtil.getHostname());
    endpointAddresses.addAll(HostnameUtil.getHostnames("0.0.0.0"));

    String applicationUri ="urn:smg:actuatorclient:opcua:server:" + UUID.randomUUID();
    OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
        .setApplicationUri(applicationUri)
        .setApplicationName(LocalizedText.english(" OPC UA Server"))
        .setBindPort(impl.getPort())
        .setBindAddresses(bindAddresses)
        .setEndpointAddresses(endpointAddresses)
        .setBuildInfo(
            new BuildInfo(
                "urn:smg:actuatorclient:opcua:server",
                "fortiss",
                " OPC UA Server 1.0",
                OpcUaServer.SDK_VERSION,
                "", DateTime.now()))
        .setCertificateManager(certificateManager)
        .setCertificateValidator(certificateValidator)
        .setIdentityValidator(identityValidator)
        .setProductUri("urn:smg:actuatorclient:opcua:server")
        .setServerName(impl.getEndpointUrl())
        .setSecurityPolicies(
            EnumSet.of(
                SecurityPolicy.None,
                SecurityPolicy.Basic128Rsa15/*,
                SecurityPolicy.Basic256,
                SecurityPolicy.Basic256Sha256*/))
        .setUserTokenPolicies(
            ImmutableList.of(
                USER_TOKEN_POLICY_ANONYMOUS,
                USER_TOKEN_POLICY_USERNAME))
        .build();

    server = new OpcUaServer(serverConfig);

    server.getNamespaceManager().registerAndAdd(
    		SensorsNamespace.NAMESPACE_URI,
        idx -> new SensorsNamespace(server, idx));
}

/**
 * Second constructor: used if a filepath to a json buildfile is provided
 * @param filepath
 * @throws Exception
 */

public startOPCUAServer(ActuatorClientImpl impl, String filepath) throws Exception {
	this.filepath = filepath;
    File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");
    if (!securityTempDir.exists() && !securityTempDir.mkdirs()) {
        throw new Exception("unable to create security temp dir: " + securityTempDir);
    }
    LoggerFactory.getLogger(getClass()).info("security temp dir: {}", securityTempDir.getAbsolutePath());
//    KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

    DefaultCertificateManager certificateManager = new DefaultCertificateManager();
//    DefaultCertificateManager certificateManager = new DefaultCertificateManager(
//            loader.getServerKeyPair(),
//            loader.getServerCertificateChain()
//);

    File pkiDir = securityTempDir.toPath().resolve("pki").toFile();
    DirectoryCertificateValidator certificateValidator = new DirectoryCertificateValidator(pkiDir);
    LoggerFactory.getLogger(getClass()).info("pki dir: {}", pkiDir.getAbsolutePath());

    UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
        true,
        authChallenge -> {
            String username = authChallenge.getUsername();
            String password = authChallenge.getPassword();
            //it's configured with only 2 users at the moment.  user and admin.
            boolean userOk = "user".equals(username) && "password1".equals(password);
            boolean adminOk = "admin".equals(username) && "password2".equals(password);

            return userOk || adminOk;
        }
    );

    X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);

    List<String> bindAddresses = newArrayList();
    bindAddresses.add("0.0.0.0");

    List<String> endpointAddresses = newArrayList();
    endpointAddresses.add(HostnameUtil.getHostname());
    endpointAddresses.addAll(HostnameUtil.getHostnames("0.0.0.0"));

    String applicationUri ="urn:smg:actuatorclient:opcua:server:" + UUID.randomUUID();
    OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
        .setApplicationUri(applicationUri)
        .setApplicationName(LocalizedText.english(" OPC UA Server"))
        .setBindPort(impl.getPort())
        .setBindAddresses(bindAddresses)
        .setEndpointAddresses(endpointAddresses)
        .setBuildInfo(
            new BuildInfo(
                "urn:smg:actuatorclient:opcua:server",
                "fortiss",
                " OPC UA Server 1.0",
                OpcUaServer.SDK_VERSION,
                "", DateTime.now()))
        .setCertificateManager(certificateManager)
        .setCertificateValidator(certificateValidator)
        .setIdentityValidator(identityValidator)
        .setProductUri("urn:smg:actuatorclient:opcua:server")
        .setServerName(impl.getEndpointUrl())
        .setSecurityPolicies(
            EnumSet.of(
                SecurityPolicy.None,
                SecurityPolicy.Basic128Rsa15/*,
                SecurityPolicy.Basic256,
                SecurityPolicy.Basic256Sha256*/))
        .setUserTokenPolicies(
            ImmutableList.of(
                USER_TOKEN_POLICY_ANONYMOUS,
                USER_TOKEN_POLICY_USERNAME))
        .build();

    server = new OpcUaServer(serverConfig);

    server.getNamespaceManager().registerAndAdd(
    		SensorsNamespace.NAMESPACE_URI,
        idx -> new SensorsNamespace(server, idx, filepath));
}


public OpcUaServer getServer() {
    return server;
}

public CompletableFuture<OpcUaServer> startup() {
    return server.startup();
}

public CompletableFuture<OpcUaServer> shutdown() {
    return server.shutdown();
}
}


