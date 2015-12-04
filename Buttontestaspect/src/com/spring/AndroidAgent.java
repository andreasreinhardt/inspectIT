package com.spring;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.spring.PropertyAccessor.PropertyPathStart;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.SystemSensorData;
import info.novatec.inspectit.communication.data.CpuInformationData;
import info.novatec.inspectit.communication.data.InvocationSequenceData;
import info.novatec.inspectit.communication.data.MemoryInformationData;
import info.novatec.inspectit.communication.data.ParameterContentData;
import info.novatec.inspectit.communication.data.TimerData;
import info.novatec.inspectit.storage.serializer.util.KryoSerializationPreferences;
import com.spring.Timer2;

public class AndroidAgent {

   
	//public static final AndroidAgent INSTANCE = new AndroidAgent("");
	public Timer timer;
	KryoNetConnection kryo;
	TimerTask mTimerTask;
	Thread myThread;
	Timestamp timestamp;
    CoreData coredata;
	long pltid;
	MemoryInformationData memoryData;
	CpuInformationData cpuData;
	String Memory = "info.novatec.inspectit.agent.sensor.platform.Memorynformation";
	String CPU = "info.novatec.inspectit.agent.sensor.platform.CpuInformation";
	String TimerSensor = "info.novatec.inspectit.agent.sensor.method.timer.TimerSensor";
	String isequence = "info.novatec.inspectit.agent.sensor.method.invocationsequence.InvocationSequenceSensor";
	long sensorIDmem;
	long sensorIDcpu;
	long methodtimerID;
	long methodinvoID;
	float cpuUsage;
	long processCpuTime;
	long usedHeapMemorySize;
	String agent = "AndroidAgent";
	String version = "1.0";
	private  IPropertyAccessor propertyAccessor;   
	private List<PropertyPathStart> propertyAccessorList = new CopyOnWriteArrayList<PropertyPathStart>();
	private StringConstraint strConstraint;
    long methodID;
    private final ThreadLocal<InvocationSequenceData> threadLocalInvocationData = new ThreadLocal<InvocationSequenceData>();
    private final ThreadLocal<Long> invocationStartId = new ThreadLocal<Long>();
    private Map<Long, Double> minDurationMap = new HashMap<Long, Double>();
	/**
	 * Stores the count of the of the starting method being called in the same invocation sequence
	 * so that closing is done on the right end.
	 */
	private final ThreadLocal<Long> invocationStartIdCount = new ThreadLocal<Long>();
	private  Timer2 timer2;
	RegisteredSensorConfig rsc;
	Memory mem;
	CPU cpu;
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
		methodinvoID = kryo.registerMethodSensorType(pltid, isequence);//Get Sensor ID of Method
		Log.d("hi", "methodinvoID" + methodinvoID);
		//Register Invocation Sensor and get Sensor ID................................................................
		
		coredata = new CoreData(kryo);
		propertyAccessor = new PropertyAccessor();
		rsc = new RegisteredSensorConfig();
		mem = new Memory(); 
		cpu = new CPU();
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
                    	
                    	cpuUsage = retrieveCpuUsage();
                    	processCpuTime = getProcessCpuTime();
                    	usedHeapMemorySize = getUsedHeapMemorySize();
                    	
                    	//CPU
                    	CpuInformationData cpuData = (CpuInformationData) coredata.getPlatformSensorData(sensorIDcpu);
                    	MemoryInformationData memoryData = (MemoryInformationData) coredata.getPlatformSensorDataformem(sensorIDmem);
                    	if (cpuData == null) {
                			try {
                				 Log.d("hi", "italy0");
                				pltid = kryo.registerPlatform(agent, version);
                				sensorIDcpu = kryo.registerPlatformSensorType(pltid, CPU);//Get Sensor ID
     							Timestamp timestamp = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());
                				cpuData = new CpuInformationData(timestamp, pltid, sensorIDcpu);
                				cpuData.incrementCount();
                				cpuData.updateProcessCpuTime(processCpuTime);
                                cpuData.addCpuUsage(cpuUsage);
                                cpuData.setMinCpuUsage(cpuUsage);
                                cpuData.setMaxCpuUsage(cpuUsage);
                                coredata.addPlatformSensorData(sensorIDcpu, cpuData);
                			} catch (Exception e) {
                				e.printStackTrace();
                				 Log.d("hi", "italy1");
                			}
                		} else{
                			 Log.d("hi", "italy2");
                			cpuData.incrementCount();
                			cpuData.updateProcessCpuTime(processCpuTime);
                			cpuData.addCpuUsage(cpuUsage);

                			if (cpuUsage < cpuData.getMinCpuUsage()) {
                				cpuData.setMinCpuUsage(cpuUsage);
                			} else if (cpuUsage > cpuData.getMaxCpuUsage()) {
                				cpuData.setMaxCpuUsage(cpuUsage);
                			}
                			 Log.d("hi", "cpudata2 = " + cpuData + "platformid2 = " + pltid + "sensorid2 = " + sensorIDcpu);   
                			coredata.addPlatformSensorData(sensorIDcpu, cpuData);
                		}
                		//CPU
                    	  
                    	//MEMORY
                    	if (memoryData == null) {
                			try {
                				pltid = kryo.registerPlatform(agent, version);
                				sensorIDmem = kryo.registerPlatformSensorType(pltid, Memory);//Get Sensor ID
                				Timestamp timestamp = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());

                				memoryData = new MemoryInformationData(timestamp, pltid, sensorIDmem);
                				memoryData.incrementCount();

                			

                				memoryData.addUsedHeapMemorySize(usedHeapMemorySize);
                				memoryData.setMinUsedHeapMemorySize(usedHeapMemorySize);
                				memoryData.setMaxUsedHeapMemorySize(usedHeapMemorySize);

                			
                				coredata.addPlatformSensorDataformem(sensorIDmem, memoryData);
                			} catch (Exception e) {
                				
                			}
                		} else {
                			memoryData.incrementCount();
                			
                			memoryData.addUsedHeapMemorySize((long) usedHeapMemorySize);
                			

                			
                			if (usedHeapMemorySize < memoryData.getMinUsedHeapMemorySize()) {
                				memoryData.setMinUsedHeapMemorySize(usedHeapMemorySize);
                			} else if (usedHeapMemorySize > memoryData.getMaxUsedHeapMemorySize()) {
                				memoryData.setMaxUsedHeapMemorySize(usedHeapMemorySize);
                			}
                			coredata.addPlatformSensorDataformem(sensorIDmem, memoryData);
                		
                		}
                    	//MEMORY
                		
                		
                    	
                          Thread myThread = new Thread();
                          myThread.start();
                   }

					},  
               12000, 10000
               );
     }
	

   
	public long getUsedHeapMemorySize() {
		 Runtime rt = Runtime.getRuntime();
		 long maxMemory = rt.maxMemory();//Returns the maximum number of bytes the heap can expand to
		 long totalMemory = rt.totalMemory();//Returns the number of bytes taken by the heap at its current size.
		 long freeMemory = rt.freeMemory();//Returns the number of bytes currently available on the heap without expanding the heap
		 long usedMemory = (maxMemory - (freeMemory + (maxMemory - totalMemory)))/(1024 * 1024 * 1024);
			Log.d("hi", "heapmem" + usedMemory);
			return usedMemory;
	}
	

    private float retrieveCpuUsage() {
		// TODO Auto-generated method stub
		float usage = 30;
		float end = Debug.threadCpuTimeNanos();
		Log.d("hi", "torre = " + end);//nanos
		float start = 10000;//millis
		float end1 = (end/1000000);
		Log.d("hi", "end1 = " + end1);
		Log.d("hi", "start = " + start);
		 //usage = end1/start * 100;
		
		return usage;
	}
    
	public long getProcessCpuTime() {
		long z =  Debug.threadCpuTimeNanos();
		
		return z;
	}
	
	//TIMER METHODS
	public void methodhandler(long start,long end,long duration,String func,String classname){
		List<String> parameterTypes = null;
		try {
			methodID = kryo.registerMethod(pltid, func,classname,parameterTypes);
		} catch (ServerUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RegistrationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<ParameterContentData> parameterContentData = null;
		Object object = null;
		 Object[] parameters = null;
		 Object result = null;
		 String prefix = null;
		 
		parameterContentData = propertyAccessor.getParameterContentData(getPropertyAccessorList(), object, parameters, result);
		prefix = parameterContentData.toString();

		// crop the content strings of all ParameterContentData but leave the prefix as it is
		for (ParameterContentData contentData : parameterContentData) {
			contentData.setContent(strConstraint.crop(contentData.getContent()));
		}
		
		TimerData timerData = (TimerData) coredata.getMethodSensorData(methodtimerID, methodID, prefix);
		InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();
		
		if (null == timerData) {
			try {
				

				Timestamp timestamp = new Timestamp(System.currentTimeMillis() - Math.round(duration/1000000));

				timerData = new TimerData(timestamp, pltid, methodtimerID, methodID, parameterContentData);
				timerData.increaseCount();
				timerData.addDuration(duration/1000000);
				timerData.calculateMin(duration/1000000);
				timerData.calculateMax(duration/1000000);

				coredata.addMethodSensorData(methodtimerID, methodID, prefix, timerData);
			} catch (Exception e) {
			
			}
		} else {
			timerData.increaseCount();
			timerData.addDuration(duration/1000000);

			timerData.calculateMin(duration/1000000);
			timerData.calculateMax(duration/1000000);
			coredata.addMethodSensorData(methodtimerID, methodID, prefix, timerData);
		}
		
		


	}
	//TIMER METHODS
	

   
	public List<PropertyPathStart> getPropertyAccessorList() {
		return propertyAccessorList;
	}
	
	private void checkForSavingOrNot(CoreData coredata, long methodId, long sensorTypeId, RegisteredSensorConfig rsc, InvocationSequenceData invocationSequenceData, double startTime, // NOCHK
			double endTime, double duration) {
		double minduration = minDurationMap.get(invocationStartId.get()).doubleValue();
		if (duration >= minduration) {
			//if (LOG.isDebugEnabled()) {
				//LOG.debug("Saving invocation. " + duration + " > " + minduration + " ID(local): " + rsc.getId());
			//}
			invocationSequenceData.setDuration(duration);
			invocationSequenceData.setStart(startTime);
			invocationSequenceData.setEnd(endTime);
			coredata.addMethodSensorData(sensorTypeId, methodId, String.valueOf(System.currentTimeMillis()), invocationSequenceData);
		} else {
			//if (LOG.isDebugEnabled()) {
				//LOG.debug("Not saving invocation. " + duration + " < " + minduration + " ID(local): " + rsc.getId());
			//}
		}
	}
	

	
	
	
	
	
	
}



