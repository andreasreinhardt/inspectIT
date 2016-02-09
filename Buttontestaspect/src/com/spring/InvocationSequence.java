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

public class InvocationSequence {
	
	long invocationsensorID;
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
	private Map<String, DefaultData> sensorDataObjectsinvo = new ConcurrentHashMap<String, DefaultData>();
	private final ThreadLocal<InvocationSequenceData> threadLocalInvocationData = new ThreadLocal<InvocationSequenceData>();
	Timestamp timestamp;
	List<ParameterContentData> parameterContentData = null;
    private final ThreadLocal<Long> invocationStartId = new ThreadLocal<Long>();
    public Timer2 timer;
    private final ThreadLocal<Long> invocationStartIdCount = new ThreadLocal<Long>();
	InvocationSequenceData invocationSequenceData ;
	private Map<Long, Double> minDurationMap = new HashMap<Long, Double>();
	String filename = "InvoSensor.txt";
  	String path;
  	File file;
  	 ObjectOutputStream oos ;
  	FileOutputStream fileStream;
  	List<DefaultData> invos;
		
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public InvocationSequence(long invocationsensorID,long pltid,CoreData cd,KryoNetConnection kry1,RegisteredSensorConfig rsc, IPropertyAccessor propertyAccessor){
		this.invocationsensorID = invocationsensorID;
		this.pltid = pltid;
		this.cd = cd;
		this.kry1 = kry1;
		this.rsc = rsc;
		this.propertyAccessor = propertyAccessor;
		Timer2 timer = new Timer2(); 
		this.timer = timer;
		invos = new ArrayList<DefaultData>();
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
	
	
	public void addMethodSensorData(long sensorTypeIdentinvo, long methodIdentinvo, String prefix, MethodSensorData methodSensorDatainvo) {
		sensorDataObjectsinvo.clear();
		StringBuffer buffer = new StringBuffer();
		
		if (null != prefix) {
			buffer.append(prefix);
			buffer.append('.');
		}
		buffer.append(methodIdentinvo);
		buffer.append('.');
		buffer.append(sensorTypeIdentinvo);
		sensorDataObjectsinvo.put(buffer.toString(), methodSensorDatainvo);
		Log.d("hi", "methodtimerinvo" + sensorDataObjectsinvo);
	    List<DefaultData> tempListinvo = new ArrayList<DefaultData>(sensorDataObjectsinvo.values());
		Log.d("hi", "tempListinvo" + tempListinvo);
		//Serialize
		//if(cpuobj.size() != cpulistsize){
				try {
				//FileOutputStream fileStream = new FileOutputStream(filename);
					Log.d("hi", "Writing Going to write");
					fileStream = new FileOutputStream(file);
				    oos = new ObjectOutputStream(fileStream);
				    invos.addAll(tempListinvo);
					Log.d("","Writing invos = " + invos); 	
					oos.writeObject(invos);
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
