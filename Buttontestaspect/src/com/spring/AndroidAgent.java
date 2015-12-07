package com.spring;

import java.io.BufferedReader;
import java.io.FileReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.os.HandlerThread;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.MethodSensorData;
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
	String Memory = "info.novatec.inspectit.agent.sensor.platform.MemoryInformation";
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
	long usedNonHeapMemorySize;
	long comittedHeapMemorySize;
	String agent = "AndroidAgent";
	String version = "1.0";
	private  IPropertyAccessor propertyAccessor;   
	private List<PropertyPathStart> propertyAccessorList = new CopyOnWriteArrayList<PropertyPathStart>();
	private StringConstraint strConstraint;
    long methodID;
    private final ThreadLocal<InvocationSequenceData> threadLocalInvocationData = new ThreadLocal<InvocationSequenceData>();
    private final ThreadLocal<Long> invocationStartId = new ThreadLocal<Long>();
    private Map<Long, Double> minDurationMap = new HashMap<Long, Double>();
    long usedmem;
    private final boolean enhancedExceptionSensor = false;
    private Map<String, DefaultData> sensorDataObjects3 = new ConcurrentHashMap<String, DefaultData>();//Methods
	private final ThreadLocal<Long> invocationStartIdCount = new ThreadLocal<Long>();
	private  Timer2 timer2;
	RegisteredSensorConfig rsc;
	CPU cpuclass;
	Memory memclass;
	    
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
		cpuclass = new CPU(sensorIDcpu,pltid,coredata,kryo);
		memclass = new Memory(sensorIDmem,pltid,coredata,kryo);
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
                    	
                    	cpuclass.update();
                    	memclass.update();
                		
                        Thread myThread = new Thread();
                        myThread.start();
                   }
                  },  
               12000, 10000
               );
     }
	


	//TIMER METHODS
	public void methodhandler(long start,long end,long duration,String func,String classname){
		List<String> parameterTypes = null;
		long duration1 = duration/1000000;
		long start1 = start/1000000;
		long end1 = end/1000000;
		
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
		String prefix = null;
		Object object = null;
		Object[] parameters = null;
		Object result = null;
		
		// check if some properties need to be accessed and saved
		if (rsc.isPropertyAccess()) {
			parameterContentData = propertyAccessor.getParameterContentData(rsc.getPropertyAccessorList(), object, parameters, result);
			prefix = parameterContentData.toString();

			// crop the content strings of all ParameterContentData but leave the prefix as it is
			for (ParameterContentData contentData : parameterContentData) {
				contentData.setContent(strConstraint.crop(contentData.getContent()));
			}
		}
		
		TimerData timerData = (TimerData) coredata.getMethodSensorData(methodtimerID, methodID, prefix);
		InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();
		
		if (null == timerData) {
			try {
				

				Timestamp timestamp = new Timestamp(System.currentTimeMillis() - Math.round(duration/1000000));

				timerData = new TimerData(timestamp, pltid, methodtimerID, methodID, parameterContentData);
				timerData.increaseCount();
				timerData.addDuration(duration1);
				timerData.calculateMin(duration1);
				timerData.calculateMax(duration1);

				addMethodSensorData(methodtimerID, methodID, prefix, timerData);
				
			} catch (Exception e) {
			
			}
		} else {
			timerData.increaseCount();
			timerData.addDuration(duration1);

			timerData.calculateMin(duration1);
			timerData.calculateMax(duration1);
			addMethodSensorData(methodtimerID, methodID, prefix, timerData);
		}
		
		//Invocation
	/*	if (null != invocationSequenceData) {
			// check if some properties need to be accessed and saved
			if (rsc.isPropertyAccess()) {
				List<ParameterContentData> parameterContentData1 = propertyAccessor.getParameterContentData(rsc.getPropertyAccessorList(), object, parameters, result);

				// crop the content strings of all ParameterContentData
				for (ParameterContentData contentData : parameterContentData1) {
					contentData.setContent(strConstraint.crop(contentData.getContent()));
				}
			}

			if (methodID == invocationStartId.get().longValue() && 0 == invocationStartIdCount.get().longValue()) {
				

				// complete the sequence and store the data object in the 'true'
				// core service so that it can be transmitted to the server. we
				// just need an arbitrary prefix so that this sequence will
				// never be overwritten in the core service!
				if (minDurationMap.containsKey(invocationStartId.get())) {
					checkForSavingOrNot(coredata, methodID, methodinvoID, rsc, invocationSequenceData, start1, end1, duration1);
				} else {
					// maybe not saved yet in the map
					if (rsc.getSettings().containsKey("minduration")) {
						minDurationMap.put(invocationStartId.get(), Double.valueOf((String) rsc.getSettings().get("minduration")));
						checkForSavingOrNot(coredata, methodID, methodinvoID, rsc, invocationSequenceData, start1, end1, duration1);
					} else {
						invocationSequenceData.setDuration(duration1);
						invocationSequenceData.setStart(start1);
						invocationSequenceData.setEnd(end1);
						//coredata.addMethodSensorData(methodinvoID, methodID, String.valueOf(System.currentTimeMillis()), invocationSequenceData);
					}
				}

				threadLocalInvocationData.set(null);
			} else {
				// just close the nested sequence and set the correct child count
				InvocationSequenceData parentSequence = invocationSequenceData.getParentSequence();
				// check if we should not include this invocation because of exception delegation or
				// SQL wrapping
				if (removeDueToExceptionDelegation(rsc, invocationSequenceData) || removeDueToWrappedSqls(rsc, invocationSequenceData)) {
					parentSequence.getNestedSequences().remove(invocationSequenceData);
					parentSequence.setChildCount(parentSequence.getChildCount() - 1);
					// but connect all possible children to the parent then
					// we are eliminating one level here
					if (CollectionUtils.isNotEmpty(invocationSequenceData.getNestedSequences())) {
						parentSequence.getNestedSequences().addAll(invocationSequenceData.getNestedSequences());
						parentSequence.setChildCount(parentSequence.getChildCount() + invocationSequenceData.getChildCount());
					}
				} else {
					invocationSequenceData.setEnd(timer2.getCurrentTime());
					invocationSequenceData.setDuration(invocationSequenceData.getEnd() - invocationSequenceData.getStart());
					parentSequence.setChildCount(parentSequence.getChildCount() + invocationSequenceData.getChildCount());
				}
				threadLocalInvocationData.set(parentSequence);
				//coredata.addMethodSensorData(methodinvoID, methodID, String.valueOf(System.currentTimeMillis()), invocationSequenceData);
			}
		}*/
		//Invocation


	}
	//TIMER METHODS
	

	

   
	public List<PropertyPathStart> getPropertyAccessorList() {
		return propertyAccessorList;
	}
	
	
	
	public void addMethodSensorData(long sensorTypeIdent3, long methodIdent, String prefix, MethodSensorData methodSensorData) {
		StringBuffer buffer = new StringBuffer();
		if (null != prefix) {
			buffer.append(prefix);
			buffer.append('.');
		}
		buffer.append(methodIdent);
		buffer.append('.');
		buffer.append(sensorTypeIdent3);
		sensorDataObjects3.put(buffer.toString(), methodSensorData);
		Log.d("hi", "methodinvoc" + sensorDataObjects3);
		//Methods
		List<DefaultData> tempList2 = new ArrayList<DefaultData>(sensorDataObjects3.values());
		Log.d("hi", "tempList2" + tempList2);
		kryo.sendDataObjects(tempList2);
	}
	
	
	}
	


	
	
	




