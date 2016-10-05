package com.example.cmrdatasender;



import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import com.example.cmrdatasender.ServerUnavailableException;
//import org.slf4j.Logger;


import com.esotericsoftware.kryonet.rmi.RemoteObject;

import android.os.StrictMode;
import android.util.Log;
import info.novatec.inspectit.cmr.service.IAgentStorageService;
import info.novatec.inspectit.cmr.service.IRegistrationService;
import info.novatec.inspectit.cmr.service.ServiceInterface;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.exception.BusinessException;
import info.novatec.inspectit.kryonet.Client;
import info.novatec.inspectit.kryonet.ExtendedSerializationImpl;
import info.novatec.inspectit.kryonet.IExtendedSerialization;
import info.novatec.inspectit.kryonet.rmi.ObjectSpace;

import info.novatec.inspectit.util.PrototypeProvider;



public class KryoNetConnection {

	/**
	 * The logger of the class.
	 */
	
	//Logger log;

	/**
	 * {@link PrototypesProvider}.
	 */

	private PrototypeProvider prototypesProvider;

	/**
	 * The kryonet client to connect to the CMR.
	 */
	private Client client;

	/**
	 * The agent storage remote object which will be used to send the measurements to.
	 */
	private IAgentStorageService agentStorageService;

	/**
	 * The registration remote object which will be used for the registration of the sensors.
	 */
	private IRegistrationService registrationService;

	/**
	 * Attribute to check if we are connected.
	 */
	private boolean connected = false;

	/**
	 * Defines if there was a connection exception before. Used for throttling the info log
	 * messages.
	 */
	private boolean connectionException = false;

	/**
	 * The list of all network interfaces.
	 */
	private List<String> networkInterfaces;

	/**
	 * {@inheritDoc}
	 */
	public void connect(String host, int port) throws ConnectException {
	      if (null == client) {
			try {
				
				if (!connectionException) {
					
					
				}
				
				initClient(host, port);
				
				int agentStorageServiceId = IAgentStorageService.class.getAnnotation(ServiceInterface.class).serviceId();
				agentStorageService = ObjectSpace.getRemoteObject(client, agentStorageServiceId, IAgentStorageService.class);
				((RemoteObject) agentStorageService).setNonBlocking(true);
				((RemoteObject) agentStorageService).setTransmitReturnValue(false);

				int registrationServiceServiceId = IRegistrationService.class.getAnnotation(ServiceInterface.class).serviceId();
				registrationService = ObjectSpace.getRemoteObject(client, registrationServiceServiceId, IRegistrationService.class);
				((RemoteObject) registrationService).setNonBlocking(false);
				((RemoteObject) registrationService).setTransmitReturnValue(true);
                
				//log.info("KryoNet: Connection established!");
				connected = true;
				connectionException = false;
			} catch (Exception exception) {
				if (!connectionException) {
					//log.info("KryoNet: Connection to the server failed.");
				}
				connectionException = true;
				disconnect();
				//if (log.isTraceEnabled()) {
					//log.trace("connect()", exception);
				//}
				ConnectException e = new ConnectException(exception.getMessage());
				
		     	        e.printStackTrace();
				
				e.initCause(exception);
				throw e; // NOPMD root cause exception is set
			}
		}
	}

	/**
	 * Creates new client and tries to connect to host.
	 * 
	 * @param host
	 *            Host IP address.
	 * @param port
	 *            Port to connect to.
	 * @throws Exception
	 *             If {@link Exception} occurs during communication.
	 */
	private void initClient(String host, int port) throws Exception {
		
		prototypesProvider = new PrototypeProvider();
		
		prototypesProvider.init();
		
		IExtendedSerialization serialization = new ExtendedSerializationImpl(prototypesProvider);
		
		client = new Client(serialization, prototypesProvider);
		
		client.start();
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        
		client.connect(5000, host, port);
		
	}

	/**
	 * {@inheritDoc}
	 */
	public void disconnect() {
		if (null != client) {
			client.stop();
			client = null; // NOPMD
		}
		agentStorageService = null; // NOPMD
		registrationService = null; // NOPMD
		connected = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public long registerPlatform(String agentName, String version) throws ServerUnavailableException, RegistrationException {
		if (!connected) {
			throw new ServerUnavailableException();
		}

		try {
			if (null == networkInterfaces) {
				networkInterfaces = getNetworkInterfaces();
			}
			return registrationService.registerPlatformIdent(networkInterfaces, agentName, version);
		} catch (SocketException socketException) {
			//log.error("Could not obtain network interfaces from this machine!");
			//if (log.isTraceEnabled()) {
				//log.trace("Constructor", socketException);
			//}
			throw new RegistrationException("Could not register the platform", socketException);
		} catch (BusinessException businessException) {
			//if (log.isTraceEnabled()) {
				//log.trace("registerPlatform(String)", businessException);
			//}
			throw new RegistrationException("Could not register the platform", businessException);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void unregisterPlatform(String agentName) {
		if (!connected) {
			return;
		}

		try {
			if (null == networkInterfaces) {
				networkInterfaces = getNetworkInterfaces();
			}

			registrationService.unregisterPlatformIdent(networkInterfaces, agentName);
		} catch (SocketException socketException) {
		
			//throw new RegistrationException("Could not un-register the platform", socketException);
		} catch (BusinessException businessException) {
		
			//throw new RegistrationException("Could not un-register the platform", businessException);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	
	public void sendDataObjects(List<? extends DefaultData> measurements) {
		
		if (!connected) {
			//throw new ServerUnavailableException();
		}
		
		if (null != measurements && !measurements.isEmpty()) {
			try {
			//	AbstractRemoteMethodCall remote = new AddDataObjects(agentStorageService, measurements);
				//remote.makeCall();
				
				agentStorageService.addDataObjects(measurements);
			} catch (Exception e) {
				
			}
		}
	}

	
	
	public long registerMethod(long platformId, String methodname,String classname,List<String> parameterTypes)  throws ServerUnavailableException, RegistrationException  {
		if (!connected) {
			throw new ServerUnavailableException();
		}

		try {
			return registrationService.registerMethodIdent(platformId, methodname, classname, methodname, parameterTypes, methodname, 0);
		} catch (Exception serverUnavailableException) {
			throw new RegistrationException("Could not register the method sensor type", serverUnavailableException);
		}

	}

	
	public long registerMethodSensorType(long platformId, String methodSensorTypeConfig) throws ServerUnavailableException, RegistrationException {
		if (!connected) {
			throw new ServerUnavailableException();
		}

		//RegisterMethodSensorType register = new RegisterMethodSensorType(registrationService, methodSensorTypeConfig, platformId);
		try {
			//Long id = (Long) register.makeCall();
			//return id.longValue();
			return registrationService.registerMethodSensorTypeIdent(platformId, methodSensorTypeConfig, null);
		} catch (Exception serverUnavailableException) {
			//if (log.isTraceEnabled()) {
				//log.trace("registerMethod(RegisteredSensorConfig)", serverUnavailableException);
			//}
			throw new RegistrationException("Could not register the method sensor type", serverUnavailableException);
		}
	}



	/**
	 * {@inheritDoc}
	 */
	public long registerPlatformSensorType(long platformId, String platformSensorTypeConfig) throws ServerUnavailableException, RegistrationException {
		
		if (!connected) {
			
			throw new ServerUnavailableException();
		}

		//RegisterPlatformSensorType register = new RegisterPlatformSensorType(registrationService, platformSensorTypeConfig, platformId);
		
		try {
			
			return registrationService.registerPlatformSensorTypeIdent(platformId, platformSensorTypeConfig);
		} catch (Exception serverUnavailableException) {
			
		throw new RegistrationException("Could not register the platform sensor type", serverUnavailableException);
		}
	}
	
	

	
	public void addSensorTypeToMethod(long sensorTypeId, long methodId) {
		if (!connected) {
		//	throw new ServerUnavailableException();
		}

		//AddSensorTypeToMethod addTypeToSensor = new AddSensorTypeToMethod(registrationService, sensorTypeId, methodId);
		try {
			//registrationService.addSensorTypeToMethod(arg0, arg1);
			//addTypeToSensor.makeCall();
		} catch (Exception e) {
		
		}
	}

	/**
	 * Loads all the network interfaces and transforms the enumeration to the list of strings
	 * containing all addresses.
	 * 
	 * @return List of all network interfaces.
	 * @throws SocketException
	 *             If {@link SocketException} occurs.
	 */
	private List<String> getNetworkInterfaces() throws SocketException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		List<String> networkInterfaces = new ArrayList<String>();

		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();
			Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress address = (InetAddress) addresses.nextElement();
				networkInterfaces.add(address.getHostAddress());
			}
		}

		return networkInterfaces;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isConnected() {
		return connected;
	}
}
