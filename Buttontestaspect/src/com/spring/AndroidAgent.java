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
 import com.spring.Timer2;
 import org.apache.commons.collections.CollectionUtils;
 import com.spring.PropertyAccessor.PropertyPathStart;
 import android.annotation.SuppressLint;
 import android.annotation.TargetApi;
 import android.os.Build;
 import android.os.Debug;
 import android.os.Environment;
 import android.util.Log;
 import info.novatec.inspectit.communication.DefaultData;
 import info.novatec.inspectit.communication.data.InvocationSequenceData;
 import info.novatec.inspectit.communication.data.ParameterContentData;
 import info.novatec.inspectit.communication.data.TimerData;
 
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
 	private final TimerStorageFactory timerStorageFactory = TimerStorageFactory.getFactory();
 
     long methodID;
     RegisteredSensorConfig rsc;
 	CPU cpuclass;
 	Memory memclass;
 	Methods met;
 	//InvocationSensor invos;
 	long start1,end1,duration1;
 
 	String pkg;
 	String hostip;
 	String port;
 	int portaddress;
 	String agentname;
 	String agentversion = "1.0";
 	AndroidAgent agent;
 	private final ThreadLocalStack<Long> threadCpuTimeStack = new ThreadLocalStack<Long>();
 	//INVO
 	 StringConstraint strConstraint;
 	 //private Map<String, DefaultData> sensorDataObjects3 = new ConcurrentHashMap<String, DefaultData>();
 	 private final ThreadLocal<InvocationSequenceData> threadLocalInvocationData = new ThreadLocal<InvocationSequenceData>();
 	 private final ThreadLocalStack<Double> timeStack = new ThreadLocalStack<Double>();
 	 List<ParameterContentData> parameterContentData = null;
 	 private final ThreadLocal<Long> invocationStartId = new ThreadLocal<Long>();
      private final Timer2 timer11 = null;
 	 private final ThreadLocal<Long> invocationStartIdCount = new ThreadLocal<Long>();
 	// InvocationSequenceData invocationSequenceData ;
 	 @SuppressLint("UseSparseArrays")
 	public Map<Long, Double> minDurationMap = new HashMap<Long, Double>();
 	 double time3,time2;
 	 long dur2;
 	 double duration;
 	 InvocationSequence invos;
 	 private final boolean enhancedExceptionSensor = false;
 	 String minDuration = "100.0";
 	 
 	 //INVO
 	
 	 private static final String CONFIG_COMMENT = "#";
 	 private static final String CONFIG_REPOSITORY = "repository";
 	 private static final String CONFIG_DATA_SIZE_CPU = "cpucmrdata";
 	 private static final String CONFIG_DATA_SIZE_MEM = "memcmrdata";
 	 private static final String CONFIG_SEND_STRATEGY = "send-strategy";
 	 private static final String CONFIG_BUFFER_STRATEGY = "buffer-strategy";
 	 private static final String CONFIG_METHOD_SENSOR_TYPE = "method-sensor-type";
 	 private static final String CONFIG_PLATFORM_SENSOR_TYPE = "platform-sensor-type";
 	 int cpusize;
 	 int memsize;
 	 String cpudatasize;
 	 String memdatasize;
 	 private TimerData timerData;
 	 long finalduration1;
 	 TimerData td;
 	 String cfgpath;
 		
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
 		cpuclass = new CPU(sensorIDcpu,pltid,coredata,kryo,cpusize);
 		memclass = new Memory(sensorIDmem,pltid,coredata,kryo,memsize);
 		td = new TimerData();
 	    met = new Methods(methodtimerID,pltid,coredata,kryo,rsc,propertyAccessor,agent);
 		invos = new InvocationSequence(methodinvoID,pltid,coredata,kryo,rsc,propertyAccessor);
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
                12000, 10000//delay,period
                );
      }
 	
 //Reading Config File...................................................................................................................................................................	
 		 @TargetApi(Build.VERSION_CODES.KITKAT)
 		public void loadconfig() throws ParserException {
 		    	
 				try{
 				
 					File path = Environment.getDataDirectory();
 					Log.d("hi", "pathhi " + path);
 			
 				File configFile = new File("/sdcard/inspectit-agent.cfg");
 				InputStream is = new FileInputStream(configFile);
 				InputStreamReader reader = new InputStreamReader(is);
 			
 				this.parse(reader,"/sdcard/inspectit-agent.cfg");
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
 						
 						// check for CPU Data to be sent to CMR
 						if (discriminator.equalsIgnoreCase(CONFIG_DATA_SIZE_CPU)) {
 							processcpusizedata(tokenizer);
 							continue;
 						}
 						
 						// check for Memory Data to be sent to CMR
 						if (discriminator.equalsIgnoreCase(CONFIG_DATA_SIZE_MEM)) {
 							processmemsizedata(tokenizer);
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
 			
 			private void processmemsizedata(StringTokenizer tokenizer) {
 			// TODO Auto-generated method stub
 				String memdatasize1 = tokenizer.nextToken();
 			StringTokenizer parameterTokenizer = new StringTokenizer(memdatasize1, "=");
 		
 				String leftSide = parameterTokenizer.nextToken();
 				 memdatasize = parameterTokenizer.nextToken();
 				 Log.d("hi", "leftSide = " + leftSide);
 				 Log.d("hi", "rightSide = " + memdatasize);
 				memsize = Integer.parseInt(memdatasize);
 				 Log.d("hi", "memsize = " + memsize);
 		}
 
 			private void processcpusizedata(StringTokenizer tokenizer) {
 			// TODO Auto-generated method stub
 			String cpudatasize1 = tokenizer.nextToken();
 			StringTokenizer parameterTokenizer = new StringTokenizer(cpudatasize1, "=");
 	
 			String leftSide = parameterTokenizer.nextToken();
 			 cpudatasize = parameterTokenizer.nextToken();
 			 Log.d("hi", "leftSide = " + leftSide);
 			 Log.d("hi", "rightSide = " + cpudatasize);
			 cpusize = Integer.parseInt(cpudatasize);
 			 Log.d("hi", "cpusize = " + cpusize);
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
 	  public void methodhandler(double start,double end,double duration,String func,String classname){
 		List<String> parameterTypes = null;
 		try {
 			methodID = kryo.registerMethod(pltid, func,func,parameterTypes);
 			Log.d("hi", "methodIDtimer" + methodID + func);
 		} catch (ServerUnavailableException e1) {
 			
 			e1.printStackTrace();
 		} catch (RegistrationException e1) {
 			
 			e1.printStackTrace();
 		}
 		
 		double startms = start/1000000;
 		double endms = end/1000000;
 		double durationms = duration/1000000;
         if(TimerSensor!=null){
         met.update(methodID,startms,endms,durationms);
         }else{}
        
    }
 //TIMER METHODS......................................................................................................................................................................
 
 
 
 	//INVOCATION SENSOR....................................................................................................................................
 		
 		  public void beforeinvocation(double stime1,String function){
 				List<String> parameterTypes = null;
 				boolean charting = "true".equals(rsc.getSettings().get("charting"));
 				String prefix = null;
 				
 				try {
 					methodID = kryo.registerMethod(pltid, function,function,parameterTypes);
 					Log.d("hi", "methodIDinvo" + methodID + function);
 				} catch (ServerUnavailableException e1) {
 					
 					e1.printStackTrace();
 				} catch (RegistrationException e1) {
 					
 					e1.printStackTrace();
 				}
 				 time2 = stime1/1000000;
 				
 				try {
 					
 					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
 					
 
 					if (null == threadLocalInvocationData.get()) {
 						// save the start time
 						Log.d("hi", "invo 0" + threadLocalInvocationData.get());
 						timeStack.push(new Double(time2));
 						Log.d("hi", "invo 1");
 						// no invocation tracer is currently started, so we do that now.
 						InvocationSequenceData invocationSequenceData = new InvocationSequenceData(timestamp, pltid, methodinvoID, methodID);
 						Log.d("hi", "invo 2");
 						threadLocalInvocationData.set(invocationSequenceData);
 						Log.d("hi", "invo 3" + invocationSequenceData);
 						invocationStartId.set(Long.valueOf(methodID));
   					    Log.d("hi", "invo 4");
 						invocationStartIdCount.set(Long.valueOf(1));
 						Log.d("hi", "invo 5" + invocationStartIdCount);
 					
 					}else {
 						Log.d("hi", "invo 6");
 						if (methodID == invocationStartId.get().longValue()) {
 						Log.d("hi", "invo 7");
 							long count = invocationStartIdCount.get().longValue();
 							invocationStartIdCount.set(Long.valueOf(count + 1));
 						}
 						// A subsequent call to the before body method where an
 						// invocation tracer is already started.
 						InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();
 						Log.d("hi", "invo 7");
 						invocationSequenceData.setChildCount(invocationSequenceData.getChildCount() + 1L);
 		                Log.d("hi", "invo 8"+ invocationSequenceData);
 		                InvocationSequenceData nestedInvocationSequenceData = new InvocationSequenceData(timestamp, pltid, invocationSequenceData.getSensorTypeIdent(), methodID);
 						Log.d("hi", "invo 9");
 						
 						nestedInvocationSequenceData.setStart(time2);
 						Log.d("hi", "invo 10" + nestedInvocationSequenceData);
 						nestedInvocationSequenceData.setParentSequence(invocationSequenceData);
 						Log.d("hi", "invo 11" + nestedInvocationSequenceData);
 						invocationSequenceData.getNestedSequences().add(nestedInvocationSequenceData);
 	                    
 						threadLocalInvocationData.set(nestedInvocationSequenceData);
 					}
 				} catch (Exception idNotAvailableException) {
 					
 				}
 			}
 			
 			public void afterinvocation(double etime1,double dur1,String function1){
 				
 				 time3 = etime1/1000000;
 
 				InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();
 
 				if (null != invocationSequenceData) {
 					Log.d("hi", "invo 12");
 					if (methodID == invocationStartId.get().longValue()) {
 						Log.d("hi", "invo 13");
 						long count = invocationStartIdCount.get().longValue();
 						invocationStartIdCount.set(Long.valueOf(count - 1));
 
 						if (0 == count - 1) {
 							Log.d("hi", "invo 14");
 							timeStack.push(new Double(time3));
 						}
 					}
 				}
 				secondafterbody(dur1);
 			}
 			
 			public void secondafterbody(double finalduration){
 					//Second After Body
 			    double finalduration1 = finalduration/1000000;
 				String prefix = null;
 				Object object = null;
 				Object[] parameters = null;
 				Object result = null;
 				boolean charting = "true".equals(rsc.getSettings().get("charting"));
 			    double cpuduration = Debug.threadCpuTimeNanos();
 			    double cpuduration1 = cpuduration/1000000;
 				
 				Log.d("hi", "invo 15");
 				InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();
 
 				if (null != invocationSequenceData) {
 					Log.d("hi", "invo 16");
 					// check if some properties need to be accessed and saved
 					if (rsc.isPropertyAccess()) {
 						List<ParameterContentData> parameterContentData = propertyAccessor.getParameterContentData(rsc.getPropertyAccessorList(), object, parameters, result);
 
 						// crop the content strings of all ParameterContentData
 						for (ParameterContentData contentData : parameterContentData) {
 							contentData.setContent(strConstraint.crop(contentData.getContent()));
 						}
 					}
 
 					if (methodID == invocationStartId.get().longValue() && 0 == invocationStartIdCount.get().longValue()) {
 					Log.d("hi", "invo 17");
 						double endTime = timeStack.pop().doubleValue();
 						double startTime = timeStack.pop().doubleValue();
 						double duration = endTime - startTime;
 						Log.d("hi", "invo 18");
 						// complete the sequence and store the data object in the 'true'
 						// core service so that it can be transmitted to the server. we
 						// just need an arbitrary prefix so that this sequence will
 						// never be overwritten in the core service!
 						if (minDurationMap.containsKey(invocationStartId.get())) {
 							Log.d("hi", "invo 19");
 							checkForSavingOrNot(methodID, methodinvoID, rsc, invocationSequenceData, startTime, endTime, finalduration1);
 						} else {
 							Log.d("hi", "invo 20");
 							// maybe not saved yet in the map
 							if (rsc.getSettings().containsKey("minduration")) {
 								Log.d("hi", "invo 20a");
 								minDurationMap.put(invocationStartId.get(), Double.valueOf((String) rsc.getSettings().get("minduration")));
 								checkForSavingOrNot(methodID, methodinvoID, rsc, invocationSequenceData, startTime, endTime, finalduration1);
 							} else {
 								Log.d("hi", "invo 21");
 								invocationSequenceData.setDuration(finalduration1);
 								
 								invocationSequenceData.setStart(time2);
 								invocationSequenceData.setEnd(time3);
 								invos.addMethodSensorData(methodinvoID, methodID, String.valueOf(System.currentTimeMillis()), invocationSequenceData);
 							}
 						}
 
 						threadLocalInvocationData.set(null);
 					}  else {
 						Log.d("hi", "invo 31");
 						// just close the nested sequence and set the correct child count
 						InvocationSequenceData parentSequence = invocationSequenceData.getParentSequence();
 						Log.d("hi", "invo 32");
 						// check if we should not include this invocation because of exception delegation or
 						// SQL wrapping
 						if (removeDueToExceptionDelegation(rsc, invocationSequenceData) || removeDueToWrappedSqls(rsc, invocationSequenceData)) {
 							Log.d("hi", "invo 33");
 							parentSequence.getNestedSequences().remove(invocationSequenceData);
 							parentSequence.setChildCount(parentSequence.getChildCount() - 1);
 							// but connect all possible children to the parent then
 							// we are eliminating one level here
 						if (CollectionUtils.isNotEmpty(invocationSequenceData.getNestedSequences())) {
 								Log.d("hi", "invo 34");
								parentSequence.getNestedSequences().addAll(invocationSequenceData.getNestedSequences());
 								parentSequence.setChildCount(parentSequence.getChildCount() + invocationSequenceData.getChildCount());
 							}
 						} else {
 							Log.d("hi", "invo 35");
 							invocationSequenceData.setEnd(time3);
 							//invocationSequenceData.setDuration(invocationSequenceData.getEnd() - invocationSequenceData.getStart());
 						    invocationSequenceData.setDuration(finalduration1);
 							invocationSequenceData.setTimerData(timerData);
 							//New
 							ITimerStorage storage = (ITimerStorage) coredata.getObjectStorage(methodinvoID, methodID, prefix);
 							storage = timerStorageFactory.newStorage(timestamp, pltid, methodinvoID, methodID, parameterContentData, charting);
 							storage.addData(finalduration1, cpuduration1);
 							addObjectStorage(methodinvoID, methodID, prefix, storage);
 							//New
 							parentSequence.setChildCount(parentSequence.getChildCount() + invocationSequenceData.getChildCount());
 						}
 						threadLocalInvocationData.set(parentSequence);
 					}
 				}
 					//Second After Body
 				}
 			
 			public void addObjectStorage(long sensorTypeId, long methodId, String prefix, IObjectStorage objectStorage) {
 				if (null == threadLocalInvocationData.get()) {
 					
 					return;
 				}
 				DefaultData defaultData = objectStorage.finalizeDataObject();
 				saveDataObject(defaultData.finalizeData());
 			}
 
 			private boolean removeDueToExceptionDelegation(RegisteredSensorConfig rsc, InvocationSequenceData invocationSequenceData) {
 				if (1 == rsc.getSensorTypeConfigs().size()) {
 					MethodSensorTypeConfig methodSensorTypeConfig = rsc.getSensorTypeConfigs().get(0);
 
 					if (ExceptionSensor.class.getCanonicalName().equals(methodSensorTypeConfig.getClassName())) {
 						return CollectionUtils.isEmpty(invocationSequenceData.getExceptionSensorDataObjects());
 					}
 				}
 
 				return false;
 			}
 
 			/**
 			 * Returns if the given {@link InvocationSequenceData} should be removed due to the wrapping of
 			 * the prepared SQL statements.
 			 * 
 			 * @param rsc
 			 *            {@link RegisteredSensorConfig}
 			 * @param invocationSequenceData
 			 *            {@link InvocationSequenceData} to check.
 			 * @return True if the invocation should be removed.
 			 */
 			private boolean removeDueToWrappedSqls(RegisteredSensorConfig rsc, InvocationSequenceData invocationSequenceData) {
 				if (1 == rsc.getSensorTypeConfigs().size() || (2 == rsc.getSensorTypeConfigs().size() && enhancedExceptionSensor)) {
 					for (MethodSensorTypeConfig methodSensorTypeConfig : rsc.getSensorTypeConfigs()) {
 
 						//if (PreparedStatementSensor.class.getCanonicalName().equals(methodSensorTypeConfig.getClassName())) {
 							if (null == invocationSequenceData.getSqlStatementData() || 0 == invocationSequenceData.getSqlStatementData().getCount()) {
 							return true;
 							}
 						//}
 					}
 				}
 
 				return false;
 			}
 		
 			
 		
 		
 		private void checkForSavingOrNot( long methodId, long sensorTypeId, RegisteredSensorConfig rsc, InvocationSequenceData invocationSequenceData, double startTime, // NOCHK
 				double endTime, double duration) {
 			Log.d("hi", "invo 25a");
 			double minduration = minDurationMap.get(invocationStartId.get()).doubleValue();
 			if (duration >= minduration) {
 				Log.d("hi", "invo 25b");
 				invocationSequenceData.setDuration(duration);
 				invocationSequenceData.setStart(startTime);
 				invocationSequenceData.setEnd(endTime);
 				invos.addMethodSensorData(sensorTypeId, methodId, String.valueOf(System.currentTimeMillis()), invocationSequenceData);
 			} else {
 			
 			}
 		}
 
 			
 	//INVOCATION SENSOR................................................................................................................
 		
 	public List<PropertyPathStart> getPropertyAccessorList() {
 		return propertyAccessorList;
 	}
 	
 	
 	
 	public void saveDataObject(DefaultData dataObject) {
 		InvocationSequenceData invocationSequenceData = threadLocalInvocationData.get();
 
 	
 
 		if (dataObject.getClass().equals(TimerData.class)) {
 			// don't overwrite an already existing timerdata or httptimerdata object.
 			if (null == invocationSequenceData.getTimerData()) {
 				invocationSequenceData.setTimerData((TimerData) dataObject);
 			}
 		}
 
 		
 	}
 }
