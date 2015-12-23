package com.spring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections.CollectionUtils;

import com.spring.PropertyAccessor.PropertyPathStart;
import android.annotation.SuppressLint;
import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.data.InvocationSequenceData;
import info.novatec.inspectit.communication.data.ParameterContentData;

@SuppressLint("UseValueOf")
public class AndroidAgent  {

   
	//public static final AndroidAgent INSTANCE = new AndroidAgent("");
	public Timer timer;
	KryoNetConnection kryo;
	TimerTask mTimerTask;
	Thread myThread;
	Timestamp timestamp;
    CoreData coredata;
	long pltid;
	//String Memory = "info.novatec.inspectit.agent.sensor.platform.MemoryInformation";
	String Memory;
	//String CPU = "info.novatec.inspectit.agent.sensor.platform.CpuInformation";
	String CPU;
	//String TimerSensor = "info.novatec.inspectit.agent.sensor.method.timer.TimerSensor";
	String TimerSensor;
	//String Isequence = "info.novatec.inspectit.agent.sensor.method.invocationsequence.InvocationSequenceSensor";
	String Isequence;
	long sensorIDmem;
	long sensorIDcpu;
	long methodtimerID;
	long methodinvoID;
    public  IPropertyAccessor propertyAccessor;   
	private List<PropertyPathStart> propertyAccessorList = new CopyOnWriteArrayList<PropertyPathStart>();
	
    long methodID;
    RegisteredSensorConfig rsc;
	CPU cpuclass;
	Memory memclass;
	Methods met;
	InvocationSensor invos;
	long start1,end1,duration1;

	String pkg;
	String hostip;
	String port;
	int portaddress;
	String agentname;
	String agentversion = "1.0";
	AndroidAgent agent;
	
	//INVO
	 StringConstraint strConstraint;
	 private Map<String, DefaultData> sensorDataObjects3 = new ConcurrentHashMap<String, DefaultData>();
	 private final ThreadLocal<InvocationSequenceData> threadLocalInvocationData = new ThreadLocal<InvocationSequenceData>();
	 private final ThreadLocalStack<Double> timeStack = new ThreadLocalStack<Double>();
	 List<ParameterContentData> parameterContentData = null;
	 private final ThreadLocal<Long> invocationStartId = new ThreadLocal<Long>();
     public Timer2 timer2;
	 private final ThreadLocal<Long> invocationStartIdCount = new ThreadLocal<Long>();
	 InvocationSequenceData invocationSequenceData ;
	 private Map<Long, Double> minDurationMap = new HashMap<Long, Double>();
	 //INVO
	 
	 private static final String CONFIG_COMMENT = "#";
	 private static final String CONFIG_REPOSITORY = "repository";
	 private static final String CONFIG_SEND_STRATEGY = "send-strategy";
	 private static final String CONFIG_BUFFER_STRATEGY = "buffer-strategy";
	 private static final String CONFIG_METHOD_SENSOR_TYPE = "method-sensor-type";
	 private static final String CONFIG_PLATFORM_SENSOR_TYPE = "platform-sensor-type";
		
	public AndroidAgent() {
		Log.d("hi", "Inside Android Agent");
	
	try {
 		loadconfig();//Load configuration file
 		  
 		//KRYO CONNECTION.........................................................................................
 	     kryo = new KryoNetConnection();
 		 kryo.connect(hostip, portaddress);//connect to CMR 
 		//KRYO CONNECTION.........................................................................................
 		
 		//Register the Agent and get Platform ID .................................................................
 		pltid = kryo.registerPlatform(agentname,agentversion);
    	Log.d("hi", "pltid" + pltid);
    	//Register the Agent and get Platform ID .................................................................
    	
    	//Register Memory Sensor and get Sensor ID................................................................
		sensorIDmem = kryo.registerPlatformSensorType(pltid, Memory);//Get Sensor ID of Memory
		Log.d("hi", "sensormem" + sensorIDmem);
		//Register Memory Sensor and get Sensor ID................................................................
		
		//Register CPU Sensor and get Sensor ID................................................................
		sensorIDcpu = kryo.registerPlatformSensorType(pltid, CPU);//Get Sensor ID of CPU
		Log.d("hi", "sensorcpu" + sensorIDcpu);
		//Register CPU Sensor and get Sensor ID................................................................
		
		//Register Timer Sensor and get Sensor ID................................................................
		methodtimerID = kryo.registerMethodSensorType(pltid, TimerSensor);//Get Timer ID of Method
		Log.d("hi", "methodtimerID" + methodtimerID);
		//Register Timer Sensor and get Sensor ID................................................................
		
		//Register Invocation Sensor and get Sensor ID................................................................
		methodinvoID = kryo.registerMethodSensorType(pltid, Isequence);//Get Sensor ID of Method
		Log.d("hi", "methodinvoID" + methodinvoID);
		//Register Invocation Sensor and get Sensor ID................................................................
		
		coredata = new CoreData(kryo);
		propertyAccessor = new PropertyAccessor();
		rsc = new RegisteredSensorConfig();
		cpuclass = new CPU(sensorIDcpu,pltid,coredata,kryo);
		memclass = new Memory(sensorIDmem,pltid,coredata,kryo);
	    met = new Methods(methodtimerID,pltid,coredata,kryo,rsc,propertyAccessor);
		invos = new InvocationSensor(methodinvoID,pltid,coredata,kryo,rsc,propertyAccessor);
	} catch (Exception e) {
		Log.d("hi", "Exceptionviv is " + e);
		
		e.printStackTrace(System.out);
	}
        
 	    timer = new Timer();
        timer.scheduleAtFixedRate( 
                   new java.util.TimerTask() {
                    @Override
                       public void run() {
                    	
                    	
                    	cpuclass.update();//CPU Data
                    	
                    	
                    	memclass.update();//Memory Data
                    	
                    	
                        Thread myThread = new Thread();
                        myThread.start();
                   }
                  },  
               12000, 10000
               );
     }
	
//Reading Config File...................................................................................................................................................................	
		 public void loadconfig() throws ParserException {
		    	
				try{
				BufferedReader Br=new BufferedReader(new FileReader("/data/local/inspectit-agent.cfg"));
				File configFile = new File("/data/local/inspectit-agent.cfg");
				InputStream is = new FileInputStream(configFile);
				InputStreamReader reader = new InputStreamReader(is);
				this.parse(reader,"/data/local/inspectit-agent.cfg");
				}
			    catch (FileNotFoundException e) {
					Log.d("hi","Agent Configuration file not found at ");
					throw new ParserException("Agent Configuration file not found at " +  e);
				}
			}
			
		public void parse(Reader reader, String pathToConfig) throws ParserException {
				// check for a valid Reader object
				if (null == reader) {
					throw new ParserException("Input is null! Aborting parsing.");
				}

				BufferedReader br = new BufferedReader(reader);

				String line = null;
				try {
					while ((line = br.readLine()) != null) { // NOPMD
						// Skip empty and comment lines
						if (line.trim().equals("") || line.startsWith(CONFIG_COMMENT)) {
							continue;
						}

						// Split the line into tokens
						StringTokenizer tokenizer = new StringTokenizer(line, " ");
						String discriminator = tokenizer.nextToken();

						// check for the repository
						if (discriminator.equalsIgnoreCase(CONFIG_REPOSITORY)) {
							processRepositoryLine(tokenizer);
							continue;
						}

						// check for a sending strategy
						if (discriminator.equalsIgnoreCase(CONFIG_SEND_STRATEGY)) {
						//	processSendStrategyLine(tokenizer);
							continue;
						}

						// check for a buffer strategy
						if (discriminator.equalsIgnoreCase(CONFIG_BUFFER_STRATEGY)) {
						//	processBufferStrategyLine(tokenizer);
							continue;
						}

						// Check for the method sensor type
						if (discriminator.equalsIgnoreCase(CONFIG_METHOD_SENSOR_TYPE)) {
						  processMethodSensorTypeLine(tokenizer);
							continue;
						}

						// Check for the platform sensor type
						if (discriminator.equalsIgnoreCase(CONFIG_PLATFORM_SENSOR_TYPE)) {
							processPlatformSensorTypeLine(tokenizer);
							continue;
						}
		           }
				} catch (Throwable throwable) {
					
					throw new ParserException("Error reading config on line : " + line, throwable);
				}
			}
			
			/*Read the config file and get-
			AgentName
			Hostip
			Port Address
			*/
	       private void processRepositoryLine(StringTokenizer tokenizer) throws ParserException {
		        Log.d("hi", "Inside parse");
				hostip = tokenizer.nextToken();
				portaddress = Integer.parseInt(tokenizer.nextToken()); 
		        agentname = tokenizer.nextToken();
		   }
	       
	       /*Process method sensor type and get - 
	       Timer Sensor
	       Invocation Sensor
	       */
	       private void processMethodSensorTypeLine(StringTokenizer tokenizer) throws ParserException {
	   		String sensorTypeName = tokenizer.nextToken();
	   		String sensorTypeClass = tokenizer.nextToken();
	   		String priorityString = tokenizer.nextToken();
	   		PriorityEnum priority = PriorityEnum.valueOf(priorityString);

	        if(sensorTypeName.equalsIgnoreCase("timer")){
	        	TimerSensor = sensorTypeClass;
	        	Log.d("hi", "TimerSensor" + TimerSensor);
	        }
	        else if(sensorTypeName.equalsIgnoreCase("isequence")){
	        	Isequence = sensorTypeClass;
	        	Log.d("hi", "Isequence" + Isequence);
	        }
	   		
	   	}
	       
	       /*Process Platform Sensors like
	         CPU and Memory */
	       private void processPlatformSensorTypeLine(StringTokenizer tokenizer) throws ParserException {
	   		String sensorTypePlatform = tokenizer.nextToken();
	        
	   		Log.d("hi", "sensorTypePlatform" + sensorTypePlatform);
	   		
	   		if(sensorTypePlatform.equalsIgnoreCase("info.novatec.inspectit.agent.sensor.platform.MemoryInformation")){
	   			Memory = sensorTypePlatform;
	   			Log.d("hi", "Memory" + Memory);
	   		}
	   		if(sensorTypePlatform.equalsIgnoreCase("info.novatec.inspectit.agent.sensor.platform.CpuInformation")){
	   			CPU = sensorTypePlatform;
	   			Log.d("hi", "CPU" + CPU);
	   		}
	   	}
			
//Reading Config File...................................................................................................................................................................	
			
	

//TIMER METHODS.....................................................................................................................................................................
	  public void methodhandler(long start,long end,long duration,String func,String classname){
		List<String> parameterTypes = null;
		try {
			methodID = kryo.registerMethod(pltid, func,func,parameterTypes);
			Log.d("hi", "methodID" + methodID + func);
		} catch (ServerUnavailableException e1) {
			
			e1.printStackTrace();
		} catch (RegistrationException e1) {
			
			e1.printStackTrace();
		}
		
        long startms = start/1000000;
        long endms = end/1000000;
        long durationms = duration/1000000;
        if(TimerSensor!=null){
        met.update(methodID,startms,endms,durationms);
        }else{}
       // invos.update(methodID,startms,endms,durationms);
   }
//TIMER METHODS......................................................................................................................................................................

//INVOCATION SENSOR....................................................................................................................................
	
	public void beforeinvocation(long stime1,String function){
		List<String> parameterTypes = null;
		try {
			methodID = kryo.registerMethod(pltid, function,function,parameterTypes);
			//Log.d("hi", "methodID" + methodID);
		} catch (ServerUnavailableException e1) {
			
			e1.printStackTrace();
		} catch (RegistrationException e1) {
			
			e1.printStackTrace();
		}
		long time2 = stime1/1000000;
		
		try {
			
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			

			if (null == threadLocalInvocationData.get()) {
				// save the start time
				timeStack.push(new Double(time2));

				// no invocation tracer is currently started, so we do that now.
				InvocationSequenceData invocationSequenceData = new InvocationSequenceData(timestamp, pltid, methodinvoID, methodID);
				threadLocalInvocationData.set(invocationSequenceData);

				invocationStartId.set(Long.valueOf(methodID));
				invocationStartIdCount.set(Long.valueOf(1));
			} else {
				  Log.d("hi", "inside else");
				if (methodID == invocationStartId.get().longValue()) {
					long count = invocationStartIdCount.get().longValue();
					invocationStartIdCount.set(Long.valueOf(count + 1));
				}
				// A subsequent call to the before body method where an
				// invocation tracer is already started.
				InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();
				invocationSequenceData.setChildCount(invocationSequenceData.getChildCount() + 1L);
                Log.d("hi", "invocationSequenceData"+ invocationSequenceData);
				InvocationSequenceData nestedInvocationSequenceData = new InvocationSequenceData(timestamp, pltid, invocationSequenceData.getSensorTypeIdent(), methodID);
				nestedInvocationSequenceData.setStart(time2);
				nestedInvocationSequenceData.setParentSequence(invocationSequenceData);

				invocationSequenceData.getNestedSequences().add(nestedInvocationSequenceData);

				threadLocalInvocationData.set(nestedInvocationSequenceData);
			}
		} catch (Exception idNotAvailableException) {
			
		}
	}
	
	public void afterinvocation(long etime1,long dur1,String function1){
		
		long time3 = etime1/1000000;
		String prefix = null;
		Object object = null;
		Object[] parameters = null;
		Object result = null;
		InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();

		if (null != invocationSequenceData) {
			if (methodID == invocationStartId.get().longValue()) {
				long count = invocationStartIdCount.get().longValue();
				invocationStartIdCount.set(Long.valueOf(count - 1));

				if (0 == count - 1) {
					timeStack.push(new Double(time3));
				}
			}
			
			//Second After Body
			
						if (rsc.isPropertyAccess()) {
							List<ParameterContentData> parameterContentData = propertyAccessor.getParameterContentData(rsc.getPropertyAccessorList(), object, parameters, result);

							// crop the content strings of all ParameterContentData
							for (ParameterContentData contentData : parameterContentData) {
								contentData.setContent(strConstraint.crop(contentData.getContent()));
							}
						}

						if (methodID == invocationStartId.get().longValue() && 0 == invocationStartIdCount.get().longValue()) {
							double endTime = timeStack.pop().doubleValue();
							double startTime = timeStack.pop().doubleValue();
							double duration = endTime - startTime;
                             Log.d("hi", "OLA" + methodID);
							// complete the sequence and store the data object in the 'true'
							// core service so that it can be transmitted to the server. we
							// just need an arbitrary prefix so that this sequence will
							// never be overwritten in the core service!
							if (minDurationMap.containsKey(invocationStartId.get())) {
								checkForSavingOrNot( methodID, methodinvoID, rsc, invocationSequenceData, startTime, endTime, duration);
							} else {
								// maybe not saved yet in the map
								if (rsc.getSettings().containsKey("minduration")) {
									minDurationMap.put(invocationStartId.get(), Double.valueOf((String) rsc.getSettings().get("minduration")));
									checkForSavingOrNot(methodID, methodinvoID, rsc, invocationSequenceData, startTime, endTime, duration);
								} else {
									invocationSequenceData.setDuration(duration);
									invocationSequenceData.setStart(startTime);
									invocationSequenceData.setEnd(endTime);
									invos.addMethodSensorData(methodinvoID, methodID, String.valueOf(System.currentTimeMillis()), invocationSequenceData);
								}
							}

							threadLocalInvocationData.set(null);
						} else {
						
							InvocationSequenceData parentSequence = invocationSequenceData.getParentSequence();
						
						//if (removeDueToExceptionDelegation(rsc, invocationSequenceData) || removeDueToWrappedSqls(rsc, invocationSequenceData)) {
								parentSequence.getNestedSequences().remove(invocationSequenceData);
								parentSequence.setChildCount(parentSequence.getChildCount() - 1);
								
								if (CollectionUtils.isNotEmpty(invocationSequenceData.getNestedSequences())) {
									parentSequence.getNestedSequences().addAll(invocationSequenceData.getNestedSequences());
									parentSequence.setChildCount(parentSequence.getChildCount() + invocationSequenceData.getChildCount());
								}
							//} else {
								invocationSequenceData.setEnd(time3);
								invocationSequenceData.setDuration(invocationSequenceData.getEnd() - invocationSequenceData.getStart());
								parentSequence.setChildCount(parentSequence.getChildCount() + invocationSequenceData.getChildCount());
							//}
							threadLocalInvocationData.set(parentSequence);
						}
			//Second After Body
		}
		
	}
	
	private void checkForSavingOrNot( long methodId, long sensorTypeId, RegisteredSensorConfig rsc, InvocationSequenceData invocationSequenceData, double startTime, // NOCHK
			double endTime, double duration) {
		double minduration = minDurationMap.get(invocationStartId.get()).doubleValue();
		if (duration >= minduration) {
			
			invocationSequenceData.setDuration(duration);
			invocationSequenceData.setStart(startTime);
			invocationSequenceData.setEnd(endTime);
			invos.addMethodSensorData(sensorTypeId, methodId, String.valueOf(System.currentTimeMillis()), invocationSequenceData);
		} else {
		
		}
	}
//INVOCATION SENSOR.....................................................................................................................................................................
	
	public List<PropertyPathStart> getPropertyAccessorList() {
		return propertyAccessorList;
	}
	
	
	
}
	


	
	
	




