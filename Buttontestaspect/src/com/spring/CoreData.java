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

public class CoreData  {
	
	
		
		private Map<String, DefaultData> sensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU
		private Map<String, DefaultData> sensorDataObjects1 = new ConcurrentHashMap<String, DefaultData>();//Memory
		private Map<String, DefaultData> sensorDataObjects3 = new ConcurrentHashMap<String, DefaultData>();//Methods
		KryoNetConnection kry1;
		
	
		
	public CoreData(KryoNetConnection kry1){
		this.kry1=kry1;
    }
	

	
	public SystemSensorData getPlatformSensorData(long sensorTypeIdent) {
		return (SystemSensorData) sensorDataObjects.get(Long.toString(sensorTypeIdent));
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
	
}
	


