package com.spring;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.spring.PropertyAccessor.PropertyPathStart;
import android.annotation.SuppressLint;
import android.util.Log;


@SuppressLint("UseValueOf")
public class AndroidAgent {

   
	//public static final AndroidAgent INSTANCE = new AndroidAgent("");
	public Timer timer;
	KryoNetConnection kryo;
	TimerTask mTimerTask;
	Thread myThread;
	Timestamp timestamp;
    CoreData coredata;
	long pltid;
	String Memory = "info.novatec.inspectit.agent.sensor.platform.MemoryInformation";
	String CPU = "info.novatec.inspectit.agent.sensor.platform.CpuInformation";
	String TimerSensor = "info.novatec.inspectit.agent.sensor.method.timer.TimerSensor";
	String Isequence = "info.novatec.inspectit.agent.sensor.method.invocationsequence.InvocationSequenceSensor";
	long sensorIDmem;
	long sensorIDcpu;
	long methodtimerID;
	long methodinvoID;
    public  IPropertyAccessor propertyAccessor;   
	private List<PropertyPathStart> propertyAccessorList = new CopyOnWriteArrayList<PropertyPathStart>();
	public StringConstraint strConstraint;
    long methodID;
    RegisteredSensorConfig rsc;
	CPU cpuclass;
	Memory memclass;
	Methods met;
	InvocationSensor invos;
	long start1,end1,duration1;
	private static final String Package_Name = "packagename:";
	private static final String HOST = "host:";
	private static final String PORT = "port:";
	private static final String Agent_Name = "agentname:";
	private static final String Agent_Version = "version:";
	String pkg;
	String hostip;
	String port;
	int portaddress;
	String agentname;
	String agentversion;
	    
	public AndroidAgent() {
		Log.d("hi", "Inside Android Agent");
	
		
 	try {
 		//KRYO CONNECTION.........................................................................................
 	    
 		hostip = getHostIP();
 		portaddress = getPort();
 		kryo = new KryoNetConnection();
 		kryo.connect(hostip, portaddress);//connect to CMR 
 		//KRYO CONNECTION.........................................................................................
 		
 		//Register the Agent and get Platform ID .................................................................
 		agentname = getAgentName();
 		agentversion = getAgentVersion();
		pltid = kryo.registerPlatform(agentname,agentversion);
    	Log.d("hi", "pltid" + pltid);
    	//Register the Agent and get Platform ID .................................................................
    	
    	//Register Memory Sensor and get Sensor ID................................................................
		sensorIDmem = kryo.registerPlatformSensorType(pltid, Memory);//Get Sensor ID of Memory
		Log.d("hi", "sensormem" + sensorIDmem);
		//Register Memory Sensor and get Sensor ID................................................................
		
		//Register CPU Sensor and get Sensor ID................................................................
		sensorIDcpu = kryo.registerPlatformSensorType(pltid, CPU);//Get Sensor ID of CPU
		Log.d("hi", "sensorcpu" + sensorIDcpu);
		//Register CPU Sensor and get Sensor ID................................................................
		
		//Register Timer Sensor and get Sensor ID................................................................
		methodtimerID = kryo.registerMethodSensorType(pltid, TimerSensor);//Get Sensor ID of Method
		Log.d("hi", "methodtimerID" + methodtimerID);
		//Register Timer Sensor and get Sensor ID................................................................
		
		//Register Invocation Sensor and get Sensor ID................................................................
		methodinvoID = kryo.registerMethodSensorType(pltid, Isequence);//Get Sensor ID of Method
		Log.d("hi", "methodinvoID" + methodinvoID);
		//Register Invocation Sensor and get Sensor ID................................................................
		
		coredata = new CoreData(kryo);
		propertyAccessor = new PropertyAccessor();
		rsc = new RegisteredSensorConfig();
		cpuclass = new CPU(sensorIDcpu,pltid,coredata,kryo);
		memclass = new Memory(sensorIDmem,pltid,coredata,kryo);
		met = new Methods(methodtimerID,pltid,coredata,kryo,rsc,propertyAccessor);
		invos = new InvocationSensor(methodinvoID,pltid,coredata,kryo,rsc,propertyAccessor);
	} catch (Exception e) {
		Log.d("hi", "Exceptionviv is " + e);
		// TODO Auto-generated catch block
		e.printStackTrace(System.out);
	}
        
 	    timer = new Timer();
        timer.scheduleAtFixedRate( 
                   new java.util.TimerTask() {
                    @Override
                       public void run() {
                    	
                    	cpuclass.update();//CPU Data
                    	memclass.update();//Memory Data
                		
                        Thread myThread = new Thread();
                        myThread.start();
                   }
                  },  
               12000, 10000
               );
     }
	
//TIMER METHODS.....................................................................................................................................................................
	public void methodhandler(long start,long end,long duration,String func,String classname){
		List<String> parameterTypes = null;
		try {
			methodID = kryo.registerMethod(pltid, func,func,parameterTypes);
			Log.d("hi", "methodID" + methodID);
		} catch (ServerUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RegistrationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        long startms = start/1000000;
        long endms = end/1000000;
        long durationms = duration/1000000;
        met.update(methodID,startms,endms,durationms);
        invos.update(methodID,startms,endms,durationms);
}
//TIMER METHODS......................................................................................................................................................................
	
	
	public List<PropertyPathStart> getPropertyAccessorList() {
		return propertyAccessorList;
	}
	
	public String PackageNameGetter(){
		String line = null;
		 try{
         BufferedReader Br=new BufferedReader(new FileReader("/data/local/agentconfig.txt"));
		while ((line = Br.readLine()) != null)  {
		 if(line.startsWith(Package_Name)){
		 pkg = line.substring(line.lastIndexOf(":")+1);
		 }
	 }
	 } catch (Throwable throwable) { // NOPMD
					try {
						throw new ParserException("Error reading config on line : " + line, throwable);
					} catch (ParserException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}Log.d("hi", "PackageName = " + pkg);
		return pkg;
	}
	
	public String getHostIP(){
		String line = null;
		try{
        BufferedReader Br=new BufferedReader(new FileReader("/data/local/agentconfig.txt"));
		while ((line = Br.readLine()) != null)  {
		 if(line.startsWith(HOST)){
			 hostip = line.substring(line.lastIndexOf(":")+1);
		 }
	 }
	 } catch (Throwable throwable) { // NOPMD
					try {
						throw new ParserException("Error reading config on line : " + line, throwable);
					} catch (ParserException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}Log.d("hi", "Host Address = " + hostip);
		return hostip;
	}

	public int getPort(){
		String line = null;
		try{
        BufferedReader Br=new BufferedReader(new FileReader("/data/local/agentconfig.txt"));
		while ((line = Br.readLine()) != null)  {
		 if(line.startsWith(PORT)){
			 port = line.substring(line.lastIndexOf(":")+1);
			 Log.d("hi", "Port  = " + port);
			 portaddress = Integer.parseInt(port);
		 }
	 }
	 } catch (Throwable throwable) { // NOPMD
					try {
						throw new ParserException("Error reading config on line : " + line, throwable);
					} catch (ParserException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    Log.d("hi", "Port Address = " + portaddress);
		return portaddress;
	}
	
	public String getAgentName(){
		String line = null;
		try{
        BufferedReader Br=new BufferedReader(new FileReader("/data/local/agentconfig.txt"));
		while ((line = Br.readLine()) != null)  {
		 if(line.startsWith(Agent_Name)){
			 agentname = line.substring(line.lastIndexOf(":")+1);
		 }
	 }
	 } catch (Throwable throwable) { // NOPMD
					try {
						throw new ParserException("Error reading config on line : " + line, throwable);
					} catch (ParserException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}Log.d("hi", "AgentName = " + agentname);
		return agentname;
	}
	
	public String getAgentVersion(){
		String line = null;
		try{
        BufferedReader Br=new BufferedReader(new FileReader("/data/local/agentconfig.txt"));
		while ((line = Br.readLine()) != null)  {
		 if(line.startsWith(Agent_Version)){
			 agentversion = line.substring(line.lastIndexOf(":")+1);
		 }
	 }
	 } catch (Throwable throwable) { // NOPMD
					try {
						throw new ParserException("Error reading config on line : " + line, throwable);
					} catch (ParserException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}Log.d("hi", "AgentVersion = " + agentversion);
		return agentversion;
	}

	
	
}
	


	
	
	




