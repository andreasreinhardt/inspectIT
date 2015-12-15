package com.spring;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Intent;
import android.os.Debug;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.SystemSensorData;
import info.novatec.inspectit.communication.data.CpuInformationData;


public class CPU {
	float cpuUsage;
	long processCpuTime;
	long sensorIDcpu;
	long pltid;
	CoreData cd;
	public Map<String, DefaultData> sensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU
	KryoNetConnection kry1;
	
	public CPU(long sensorIDcpu,long pltid,CoreData cd,KryoNetConnection kry1){
		this.sensorIDcpu = sensorIDcpu;
		this.pltid = pltid;
		this.cd = cd;
		this.kry1 = kry1;
	}
	
	public void update() {
		 processCpuTime = getProcessCpuTime();
		 cpuUsage = retrieveCpuUsage();

		CpuInformationData osData = (CpuInformationData) cd.getPlatformSensorData(sensorIDcpu);
		
		if (osData == null) {
			try {
				
				
				Timestamp timestamp = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());
                
				osData = new CpuInformationData(timestamp, pltid, sensorIDcpu);
		
				osData.incrementCount();

				osData.updateProcessCpuTime(processCpuTime);

				osData.addCpuUsage(cpuUsage);
				osData.setMinCpuUsage(cpuUsage);
				osData.setMaxCpuUsage(cpuUsage);

				addPlatformSensorData(sensorIDcpu, osData);
			} catch (Exception e) {
			
			}
		} else {
			osData.incrementCount();
			osData.updateProcessCpuTime(processCpuTime);
			osData.addCpuUsage(cpuUsage);

			if (cpuUsage < osData.getMinCpuUsage()) {
				osData.setMinCpuUsage(cpuUsage);
			} else if (cpuUsage > osData.getMaxCpuUsage()) {
				osData.setMaxCpuUsage(cpuUsage);
			}
			addPlatformSensorData(sensorIDcpu, osData);
		}

	}
		//CPU
	
	
	 private float retrieveCpuUsage() {
			// TODO Auto-generated method stub
		float usage;

		     
			float end = Debug.threadCpuTimeNanos();
			Log.d("hi", "torre = " + end);//nanos
			float start = 10000;//millis
			float end1 = (end/1000000);
			Log.d("hi", "end1 = " + end1);
			Log.d("hi", "start = " + start);
			 usage = end1/start * 100;
			Log.d("hi", "cpu = " + usage);
			return usage;
	    	
	    
	    }

	 	public long getProcessCpuTime() {
	 		long z =  Debug.threadCpuTimeNanos();
	 		
	 		return z;
	 	}
	 	
		//CPU
		public void addPlatformSensorData(long sensorTypeIdent, SystemSensorData systemSensorData) {
			Log.d("hi", "deva" + sensorTypeIdent + systemSensorData);
			sensorDataObjects.put(Long.toString(sensorTypeIdent), systemSensorData);
			Log.d("hi", "deva0 = " + sensorDataObjects);
			List<DefaultData> tempList = new ArrayList<DefaultData>(sensorDataObjects.values());
			Log.d("hi", "tempList" + tempList);
	        
			kry1.sendDataObjects(tempList);
		}
}
