package com.spring;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.collections.CollectionUtils;
import com.spring.PropertyAccessor.PropertyPathStart;
import android.annotation.SuppressLint;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.MethodSensorData;
import info.novatec.inspectit.communication.data.CpuInformationData;
import info.novatec.inspectit.communication.data.InvocationSequenceData;
import info.novatec.inspectit.communication.data.MemoryInformationData;
import info.novatec.inspectit.communication.data.ParameterContentData;
import info.novatec.inspectit.communication.data.TimerData;
import com.spring.Timer2;

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
	String agent = "AndroidAgent";
	String version = "1.0";
	public  IPropertyAccessor propertyAccessor;   
	private List<PropertyPathStart> propertyAccessorList = new CopyOnWriteArrayList<PropertyPathStart>();
	public StringConstraint strConstraint;
    long methodID;
    RegisteredSensorConfig rsc;
	CPU cpuclass;
	Memory memclass;
	Methods met;
	long start1,end1,duration1;
	    
	public AndroidAgent() {
		Log.d("hi", "Inside Android Agent");
	
		
 	try {
 		//KRYO CONNECTION.........................................................................................
 	    kryo = new KryoNetConnection();
 		kryo.connect("10.0.2.2", 9070);//connect to CMR 
 		//KRYO CONNECTION.........................................................................................
 		
 		//Register the Agent and get Platform ID .................................................................
		pltid = kryo.registerPlatform(agent, version);
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
		met = new Methods(methodtimerID,methodinvoID,pltid,coredata,kryo,rsc,propertyAccessor);
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
}
//TIMER METHODS......................................................................................................................................................................
	
	
	public List<PropertyPathStart> getPropertyAccessorList() {
		return propertyAccessorList;
	}
	
	
}
	


	
	
	




