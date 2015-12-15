package com.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.MethodSensorData;
import info.novatec.inspectit.communication.SystemSensorData;
import com.spring.KryoNetConnection;


public class CoreData  {
	
	
		
		private Map<String, DefaultData> sensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU
		private Map<String, DefaultData> sensorDataObjects2 = new ConcurrentHashMap<String, DefaultData>();//CPU
		KryoNetConnection kry1;
		
	
		
	public CoreData(KryoNetConnection kry1){
		this.kry1=kry1;
    }
	

	
	public SystemSensorData getPlatformSensorData(long sensorTypeIdent) {
		return (SystemSensorData) sensorDataObjects.get(Long.toString(sensorTypeIdent));
	}
	


	public MethodSensorData getMethodSensorData(long sensorTypeIdent, long methodIdent, String prefix) {
		StringBuffer buffer = new StringBuffer();
		if (null != prefix) {
			buffer.append(prefix);
			buffer.append('.');
		}
		buffer.append(methodIdent);
		buffer.append('.');
		buffer.append(sensorTypeIdent);
		return (MethodSensorData) sensorDataObjects2.get(buffer.toString());
	}
	
}
	


