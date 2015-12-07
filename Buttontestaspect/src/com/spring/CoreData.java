package com.spring;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.MethodSensorData;
import info.novatec.inspectit.communication.SystemSensorData;
import com.spring.KryoNetConnection;
import java.util.ArrayList;
import java.util.Iterator;

public class CoreData {
	
	
		
		private Map<String, DefaultData> sensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU
		private Map<String, DefaultData> sensorDataObjects1 = new ConcurrentHashMap<String, DefaultData>();//Memory
		private Map<String, DefaultData> sensorDataObjects3 = new ConcurrentHashMap<String, DefaultData>();//Methods
		KryoNetConnection kry1;
		
		
	public CoreData(KryoNetConnection kry1){
		this.kry1=kry1;
    }
	
	//CPU
	public void addPlatformSensorData(long sensorTypeIdent, SystemSensorData systemSensorData) {
		Log.d("hi", "deva" + sensorTypeIdent + systemSensorData);
		sensorDataObjects.put(Long.toString(sensorTypeIdent), systemSensorData);
		Log.d("hi", "deva0 = " + sensorDataObjects);
		//notifyListListeners();
		prepareData();
	}
	
	public SystemSensorData getPlatformSensorData(long sensorTypeIdent) {
		return (SystemSensorData) sensorDataObjects.get(Long.toString(sensorTypeIdent));
	}
	
	//Memory
	public void addPlatformSensorDataformem(long sensorTypeIdent1, SystemSensorData systemSensorData1) {
		Log.d("hi", "deva" + sensorTypeIdent1 + systemSensorData1);
		sensorDataObjects1.put(Long.toString(sensorTypeIdent1), systemSensorData1);
		Log.d("hi", "deva0 = " + sensorDataObjects1);
		prepareData();
	} 
	
	public SystemSensorData getPlatformSensorDataformem(long sensorTypeIdent1) {
		return (SystemSensorData) sensorDataObjects1.get(Long.toString(sensorTypeIdent1));
	}
	
    //Methods
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
		//prepareData();
	}

	/**
	 * {@inheritDoc}
	 */
	public MethodSensorData getMethodSensorData(long sensorTypeIdent3, long methodIdent, String prefix) {
		StringBuffer buffer = new StringBuffer();
		if (null != prefix) {
			buffer.append(prefix);
			buffer.append('.');
		}
		buffer.append(methodIdent);
		buffer.append('.');
		buffer.append(sensorTypeIdent3);
		return (MethodSensorData) sensorDataObjects3.get(buffer.toString());
	}
	
	//Send Data to CMR
	@SuppressWarnings("unchecked")
	private boolean prepareData() {
		// check if measurements are added in the last interval, if not
		// nothing needs to be sent.
		

		//CPU
		List<DefaultData> tempList = new ArrayList<DefaultData>(sensorDataObjects.values());
		Log.d("hi", "tempList" + tempList);
		kry1.sendDataObjects(tempList);
		
		//Memory
		List<DefaultData> tempList1 = new ArrayList<DefaultData>(sensorDataObjects1.values());
		Log.d("hi", "tempList1" + tempList1);
		kry1.sendDataObjects(tempList1);
		
		//Methods
		List<DefaultData> tempList2 = new ArrayList<DefaultData>(sensorDataObjects3.values());
		Log.d("hi", "tempList2" + tempList2);
		kry1.sendDataObjects(tempList2);
		
		return true;
	}
	
	
	}
	


