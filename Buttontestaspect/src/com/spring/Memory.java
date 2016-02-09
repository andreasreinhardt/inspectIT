package com.spring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
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
	int memlistsize;
	List<DefaultData> memobj;
	String filename = "MemoryData.txt";
  	String path;
  	File file;
  	FileOutputStream fileStream;
  	ObjectOutputStream oos ;
  	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public Memory(long sensorIDmem,long pltid,CoreData cd,KryoNetConnection kry1,int memlistsize){
		this.sensorIDmem = sensorIDmem;
		this.pltid = pltid;
		this.cd = cd;
		this.kry1 = kry1;
		this.memlistsize = memlistsize;
		memobj = new ArrayList<DefaultData>(memlistsize);
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
	
	
	public void update() {
	//	long freePhysMemory = getFreePhysMemory();
		//long freeSwapSpace = getFreeSwapSpace();
		//long comittedVirtualMemSize = getComittedVirtualMemSize();
		long usedHeapMemorySize = getUsedHeapMemorySize();
		long comittedHeapMemorySize = getComittedHeapMemorySize();
		long usedNonHeapMemorySize = getUsedNonHeapMemorySize();
		//long comittedNonHeapMemorySize = getComittedNonHeapMemoryUsage();

		MemoryInformationData memoryData = (MemoryInformationData) cd.getPlatformSensorData(sensorIDmem);

		if (memoryData == null) {
			try {
				
				Timestamp timestamp = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());

				memoryData = new MemoryInformationData(timestamp, pltid, sensorIDmem);
				memoryData.incrementCount();

				//memoryData.addComittedVirtualMemSize(comittedVirtualMemSize);
				//memoryData.setMinComittedVirtualMemSize(comittedVirtualMemSize);
				//memoryData.setMaxComittedVirtualMemSize(comittedVirtualMemSize);


				memoryData.addUsedHeapMemorySize(usedHeapMemorySize);
				memoryData.setMinUsedHeapMemorySize(usedHeapMemorySize);
				memoryData.setMaxUsedHeapMemorySize(usedHeapMemorySize);

				memoryData.addComittedHeapMemorySize(comittedHeapMemorySize);
				memoryData.setMinComittedHeapMemorySize(comittedHeapMemorySize);
				memoryData.setMaxComittedHeapMemorySize(comittedHeapMemorySize);

				memoryData.addUsedNonHeapMemorySize(usedNonHeapMemorySize);
				memoryData.setMinUsedNonHeapMemorySize(usedNonHeapMemorySize);
				memoryData.setMaxUsedNonHeapMemorySize(usedNonHeapMemorySize);

	

				addPlatformSensorData(sensorIDmem, memoryData);
			} catch (Exception e) {
				
			}
		} else {
			memoryData.incrementCount();
			//memoryData.addComittedVirtualMemSize(comittedVirtualMemSize);
			memoryData.addUsedHeapMemorySize(usedHeapMemorySize);
			memoryData.addComittedHeapMemorySize(comittedHeapMemorySize);
			memoryData.addUsedNonHeapMemorySize(usedNonHeapMemorySize);
			
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
            
			if (usedNonHeapMemorySize < memoryData.getMinUsedNonHeapMemorySize()) {
				memoryData.setMinUsedNonHeapMemorySize(usedNonHeapMemorySize);
			} else if (usedNonHeapMemorySize > memoryData.getMaxUsedNonHeapMemorySize()) {
				memoryData.setMaxUsedNonHeapMemorySize(usedNonHeapMemorySize);
			}
			
		
			addPlatformSensorData(sensorIDmem, memoryData);
		}
	}

	

	private long getUsedNonHeapMemorySize() {
		// TODO Auto-generated method stub
		return 0;
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
			
			//Serialize
			//if(cpuobj.size() != cpulistsize){
					try {
					//FileOutputStream fileStream = new FileOutputStream(filename);
						Log.d("hi", "Writing Going to write");
						fileStream = new FileOutputStream(file);
					    oos = new ObjectOutputStream(fileStream);
					    memobj.addAll(tempListmem);
						Log.d("","Writing memobj = " + memobj); 	
						oos.writeObject(memobj);
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