package com.spring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.MethodSensorData;
import info.novatec.inspectit.communication.data.InvocationSequenceData;
import info.novatec.inspectit.communication.data.ParameterContentData;
import info.novatec.inspectit.communication.data.TimerData;
import org.apache.commons.collections.CollectionUtils;

public class Methods {
	
	long timersensorID;
	long pltid;
	KryoNetConnection kry1;
	CoreData cd;
	long metID;
	long metstarttime;
	long metendtime;
	long metduration;
	RegisteredSensorConfig rsc;
	IPropertyAccessor propertyAccessor;
	StringConstraint strConstraint;
	 private Map<String, DefaultData> sensorDataObjects3 = new ConcurrentHashMap<String, DefaultData>();
	 private final ThreadLocal<InvocationSequenceData> threadLocalInvocationData = new ThreadLocal<InvocationSequenceData>();
	 Timestamp timestamp;
	 List<ParameterContentData> parameterContentData = null;
		private final ThreadLocal<Long> invocationStartId = new ThreadLocal<Long>();
        public Timer2 timer;
		private final ThreadLocal<Long> invocationStartIdCount = new ThreadLocal<Long>();
		InvocationSequenceData invocationSequenceData ;
		private Map<Long, Double> minDurationMap = new HashMap<Long, Double>();
        AndroidAgent agent;
    	String filename = "TimerSensor.txt";
      	String path;
      	File file;
      	 ObjectOutputStream oos ;
      	FileOutputStream fileStream;
      	List<DefaultData> timers;
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public Methods(long timersensorID,long pltid,CoreData cd,KryoNetConnection kry1,RegisteredSensorConfig rsc, IPropertyAccessor propertyAccessor,AndroidAgent agent){
		this.timersensorID = timersensorID;
		this.pltid = pltid;
		this.cd = cd;
		this.kry1 = kry1;
		this.rsc = rsc;
		this.propertyAccessor = propertyAccessor;
		Timer2 timer = new Timer2(); 
		this.timer = timer;
		this.agent = agent;
		timers = new ArrayList<DefaultData>();
		 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				path = Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
				Log.d("hi", "path: " + path);
			} else {
				path = Environment.getExternalStorageDirectory()
						+ File.separator + "Documents";
			}
			
			
			path += File.separatorChar + "CMR Data";
			Log.d("hi", "path = " + path);
			 file = new File(path, filename);

			new File(path).mkdirs();
			try {
				file.createNewFile();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
     }
	
	public void update(long methodID,double metstarttime,double metendtime,double metduration){
	
		String prefix = null;
		Object object = null;
		Object[] parameters = null;
		Object result = null;
		
		
		//TimerSensor
		if (rsc.isPropertyAccess()) {
			
			parameterContentData = propertyAccessor.getParameterContentData(rsc.getPropertyAccessorList(), object, parameters, result);
			prefix = parameterContentData.toString();
		
			// crop the content strings of all ParameterContentData but leave the prefix as it is
			for (ParameterContentData contentData : parameterContentData) {
				contentData.setContent(strConstraint.crop(contentData.getContent()));
			}
		}
		TimerData timerData = (TimerData) cd.getMethodSensorData(timersensorID, methodID, prefix);
		if (null == timerData) {
			try {
				Timestamp timestamp = new Timestamp(System.currentTimeMillis() - Math.round(metduration));
				timerData = new TimerData(timestamp, pltid, timersensorID, methodID, parameterContentData);
				timerData.increaseCount();
				timerData.addDuration(metduration);
				timerData.calculateMin(metduration);
				timerData.calculateMax(metduration);
				addMethodSensorData(timersensorID, methodID, prefix, timerData);
				} catch (Exception e) {
			}
		} else {
			timerData.increaseCount();
			timerData.addDuration(metduration);
			timerData.calculateMin(metduration);
			timerData.calculateMax(metduration);
			addMethodSensorData(timersensorID, methodID, prefix, timerData);
		}
		//TimerSensor
}
		
		

	public void addMethodSensorData(long sensorTypeIdent, long methodIdent1, String prefix, MethodSensorData methodSensorData) {
		sensorDataObjects3.clear();
		//agent.saveDataObject(methodSensorData.finalizeData());
		StringBuffer buffer = new StringBuffer();
		
		if (null != prefix) {
			buffer.append(prefix);
			buffer.append('.');
		}
		buffer.append(methodIdent1);
		buffer.append('.');
		buffer.append(sensorTypeIdent);
		sensorDataObjects3.put(buffer.toString(), methodSensorData);
		Log.d("hi", "methodtimer" + sensorDataObjects3);
		//Methods
		List<DefaultData> tempList2 = new ArrayList<DefaultData>(sensorDataObjects3.values());
		Log.d("hi", "tempList2" + tempList2);
		//Serialize
				//if(cpuobj.size() != cpulistsize){
						try {
						//FileOutputStream fileStream = new FileOutputStream(filename);
							Log.d("hi", "Writing Going to write");
							fileStream = new FileOutputStream(file);
						    oos = new ObjectOutputStream(fileStream);
						    timers.addAll(tempList2);
							Log.d("","Writing timers = " + timers); 	
							oos.writeObject(timers);
							Log.d("hi", "Writing 0");
							//oos.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					//}else{
						try{
							
							Log.d("hi", "Writing 1");
							//cpuobj.clear();
							oos.close();
							
						}catch(Exception e){}
					//}
					//Serialize
		
	}
}
