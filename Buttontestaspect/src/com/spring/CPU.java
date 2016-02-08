package com.spring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.SystemSensorData;
import info.novatec.inspectit.communication.data.CpuInformationData;


public class CPU implements Serializable {
	float cpuUsage;
	long processCpuTime;
	long sensorIDcpu;
	long pltid;
	CoreData cd;
	public Map<String, DefaultData> sensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU
	KryoNetConnection kry1;
	FileOutputStream fout;
    ObjectOutputStream oos ;
  	double utilizedkb;
  	List<DefaultData> cpuobj;
  	List<DefaultData> tempList;
  	int cpulistsize;
  	String filename = "CPUData.txt";
  	String path;
  	File file;
  //	private OutputStreamWriter fileStream;
  	FileOutputStream fileStream;
  
  	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public CPU(long sensorIDcpu,long pltid,CoreData cd,KryoNetConnection kry1,int cpulistsize){
		this.sensorIDcpu = sensorIDcpu;
		this.pltid = pltid;
		this.cd = cd;
		this.kry1 = kry1;
		this.cpulistsize = cpulistsize;
	    cpuobj = new ArrayList<DefaultData>(cpulistsize);
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			path = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
			Log.d("hi", "path: " + path);
		} else {
			path = Environment.getExternalStorageDirectory()
					+ File.separator + "Documents";
		}
		
		
		path += File.separatorChar + "CPU Data";
		Log.d("hi", "path = " + path);
		 file = new File(path, filename);

		new File(path).mkdirs();
		try {
			file.createNewFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			//fileStream = new FileOutputStream(filename);
			//fileStream = new FileOutputStream(file);
			//oos = new ObjectOutputStream(fileStream);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	 	
        @SuppressLint("ShowToast")
		public void addPlatformSensorData(long sensorTypeIdent, SystemSensorData systemSensorData) {
			Log.d("hi", "deva" + sensorTypeIdent + systemSensorData);
			sensorDataObjects.put(Long.toString(sensorTypeIdent), systemSensorData);
			Log.d("hi", "deva0 = " + sensorDataObjects);
			tempList = new ArrayList<DefaultData>(sensorDataObjects.values());
			Log.d("hi", "tempListcpu" + tempList);
			
		//	if(cpuobj.size() != cpulistsize){
	   	//	cpuobj.addAll(tempList);
		//	Log.d("","cpuobj = " + cpuobj);
		//	}else{
		//	kry1.sendDataObjects(cpuobj);
		//	cpuobj.clear();
		//	}
			
			//Serialize
		//if(cpuobj.size() != cpulistsize){
				try {
				//FileOutputStream fileStream = new FileOutputStream(filename);
					Log.d("hi", "Writing Going to write");
					fileStream = new FileOutputStream(file);
				    oos = new ObjectOutputStream(fileStream);
				    cpuobj.addAll(tempList);
					Log.d("","Writing cpuobj = " + cpuobj); 	
					oos.writeObject(cpuobj);
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
