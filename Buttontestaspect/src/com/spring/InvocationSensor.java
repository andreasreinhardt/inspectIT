package com.spring;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.MethodSensorData;
import info.novatec.inspectit.communication.data.InvocationSequenceData;
import info.novatec.inspectit.communication.data.ParameterContentData;
import info.novatec.inspectit.communication.data.TimerData;

public class InvocationSensor {
	
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
	 private Map<String, DefaultData> sensorDataObjects3 = new ConcurrentHashMap<String, DefaultData>();
	 private final ThreadLocal<InvocationSequenceData> threadLocalInvocationData = new ThreadLocal<InvocationSequenceData>();
	 Timestamp timestamp;
	 List<ParameterContentData> parameterContentData = null;
		private final ThreadLocal<Long> invocationStartId = new ThreadLocal<Long>();
        public Timer2 timer;
		private final ThreadLocal<Long> invocationStartIdCount = new ThreadLocal<Long>();
		InvocationSequenceData invocationSequenceData ;
		private Map<Long, Double> minDurationMap = new HashMap<Long, Double>();

		
	
	public InvocationSensor(long invocationsensorID,long pltid,CoreData cd,KryoNetConnection kry1,RegisteredSensorConfig rsc, IPropertyAccessor propertyAccessor){
		this.invocationsensorID = invocationsensorID;
		this.pltid = pltid;
		this.cd = cd;
		this.kry1 = kry1;
		this.rsc = rsc;
		this.propertyAccessor = propertyAccessor;
		Timer2 timer = new Timer2(); 
		this.timer = timer;
	}
	
	public void update(long methodID,long metstarttime,long metendtime,long metduration){
		
		String prefix = null;
		Object object = null;
		Object[] parameters = null;
		Object result = null;
	
}
		
		

	public void addMethodSensorData(long sensorTypeIdent, long methodIdent1, String prefix, MethodSensorData methodSensorData) {
		sensorDataObjects3.clear();
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
		kry1.sendDataObjects(tempList2);
	}

}
