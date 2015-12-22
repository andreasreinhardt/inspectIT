package com.spring;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Debug;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.SystemSensorData;
import info.novatec.inspectit.communication.data.MemoryInformationData;


public class Memory {
	float cpuUsage;
	long processCpuTime;
	long sensorIDmem;
	long pltid;
	CoreData cd;
	public Map<String, DefaultData> sensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU
	KryoNetConnection kry1;
	
	public Memory(long sensorIDmem,long pltid,CoreData cd,KryoNetConnection kry1){
		this.sensorIDmem = sensorIDmem;
		this.pltid = pltid;
		this.cd = cd;
		this.kry1 = kry1;
	}
	
	public void update() {
	//	long freePhysMemory = getFreePhysMemory();
		//long freeSwapSpace = getFreeSwapSpace();
		//long comittedVirtualMemSize = getComittedVirtualMemSize();
		long usedHeapMemorySize = getUsedHeapMemorySize();
		long comittedHeapMemorySize = getComittedHeapMemorySize();
		
		//long comittedNonHeapMemorySize = getComittedNonHeapMemoryUsage();

		MemoryInformationData memoryData = (MemoryInformationData) cd.getPlatformSensorData(sensorIDmem);

		if (memoryData == null) {
			try {
				
				Timestamp timestamp = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());

				memoryData = new MemoryInformationData(timestamp, pltid, sensorIDmem);
				memoryData.incrementCount();

			

				memoryData.addUsedHeapMemorySize(usedHeapMemorySize);
				memoryData.setMinUsedHeapMemorySize(usedHeapMemorySize);
				memoryData.setMaxUsedHeapMemorySize(usedHeapMemorySize);

				memoryData.addComittedHeapMemorySize(comittedHeapMemorySize);
				memoryData.setMinComittedHeapMemorySize(comittedHeapMemorySize);
				memoryData.setMaxComittedHeapMemorySize(comittedHeapMemorySize);

				//memoryData.addUsedNonHeapMemorySize(usedNonHeapMemorySize);
				//memoryData.setMinUsedNonHeapMemorySize(usedNonHeapMemorySize);
				//memoryData.setMaxUsedNonHeapMemorySize(usedNonHeapMemorySize);

	

				addPlatformSensorData(sensorIDmem, memoryData);
			} catch (Exception e) {
				
			}
		} else {
			memoryData.incrementCount();
		
			memoryData.addUsedHeapMemorySize(usedHeapMemorySize);
			memoryData.addComittedHeapMemorySize(comittedHeapMemorySize);
			
			

			if (usedHeapMemorySize < memoryData.getMinUsedHeapMemorySize()) {
				memoryData.setMinUsedHeapMemorySize(usedHeapMemorySize);
			} else if (usedHeapMemorySize > memoryData.getMaxUsedHeapMemorySize()) {
				memoryData.setMaxUsedHeapMemorySize(usedHeapMemorySize);
			}

			if (comittedHeapMemorySize < memoryData.getMinComittedHeapMemorySize()) {
				memoryData.setMinComittedHeapMemorySize(comittedHeapMemorySize);
			} else if (comittedHeapMemorySize > memoryData.getMaxComittedHeapMemorySize()) {
				memoryData.setMaxComittedHeapMemorySize(comittedHeapMemorySize);
			}

			
		
			addPlatformSensorData(sensorIDmem, memoryData);
		}
	}

	public long getUsedHeapMemorySize() {
		Runtime rt = Runtime.getRuntime();
		long maxMemory = rt.maxMemory();//Returns the maximum number of bytes the heap can expand to
		long totalMemory = rt.totalMemory();//Returns the number of bytes taken by the heap at its current size.
		long freeMemory = rt.freeMemory();//Returns the number of bytes currently available on the heap without expanding the heap
		long usedMemory = (maxMemory - (freeMemory + (maxMemory - totalMemory)));
	    Log.d("hi", "heapmem" + usedMemory);
		
			return usedMemory;
	    
	}
	
	private long getComittedHeapMemorySize() {
		// TODO Auto-generated method stub
		Runtime rt = Runtime.getRuntime();
		long totalMemory = rt.totalMemory();//Returns the number of bytes taken by the heap at its current size.
		
	    Log.d("hi", "heapmemmax" + totalMemory);
		
			return totalMemory;
	}


	 	
		
		public void addPlatformSensorData(long sensorTypeIdent, SystemSensorData systemSensorData) {
			Log.d("hi", "deva" + sensorTypeIdent + systemSensorData);
			sensorDataObjects.put(Long.toString(sensorTypeIdent), systemSensorData);
			
			List<DefaultData> tempListmem = new ArrayList<DefaultData>(sensorDataObjects.values());
			Log.d("hi", "tempList" + tempListmem);
			
			kry1.sendDataObjects(tempListmem);
		}

}