package com.example.cmrdatasender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.Reader;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.example.cmrdatasender.KryoNetConnection;
import com.example.cmrdatasender.ParserException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.data.CpuInformationData;

public class MainActivity extends Activity {
	
	 private static final String CONFIG_COMMENT = "#";
	 private static final String CONFIG_REPOSITORY = "repository";
	 private static final String CONFIG_SEND_STRATEGY = "send-strategy";
	 private static final String CONFIG_BUFFER_STRATEGY = "buffer-strategy";
	 private static final String CONFIG_METHOD_SENSOR_TYPE = "method-sensor-type";
	 private static final String CONFIG_PLATFORM_SENSOR_TYPE = "platform-sensor-type";
	 private static final String CONFIG_DATA_SIZE_CPU = "cpucmrdata";
	 private static final String CONFIG_DATA_SIZE_MEM = "memcmrdata";
	 String hostip;
	 String port;
	 int portaddress;
	 String agentname;
	 String agentversion;
	 KryoNetConnection kryo;
	 Button button1,button2,button3,button4;
	 TextView textview1,textview2,textview3,textview4,textview5,textview6;
	 String cpudatasize;
	 String memdatasize;
	 int cpusize;
	 int memsize;
	 List<DefaultData> cpuobj;
	 List<DefaultData> memobj;
	 List<DefaultData> timers;
	 List<DefaultData> invos;
	 Context context;
	 String path;
	 String pathcmr;
	 String cmrcpu;
	 String cmrmem;
	 String cmrtimersensor;
	 String cmrinvosensor;

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        //KRYO CONNECTION.........................................................................................
		try {
		loadconfig();//Load configuration file

 		kryo = new KryoNetConnection();
 		kryo.connect(hostip, portaddress);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//connect to CMR 
 		//KRYO CONNECTION.........................................................................................
 		button1 = (Button) findViewById(R.id.button1);
 		button2 = (Button) findViewById(R.id.button2);
 		button3 = (Button) findViewById(R.id.button3);
 		button4 = (Button) findViewById(R.id.button4);
 		textview1 = (TextView) findViewById(R.id.textView1);
 		textview2 = (TextView) findViewById(R.id.textView2);
 		textview3 = (TextView) findViewById(R.id.textView3);
 		textview4 = (TextView) findViewById(R.id.textView4);
 		textview5 = (TextView) findViewById(R.id.textView5);
 		textview6 = (TextView) findViewById(R.id.textView6);
 		
 		//Read path from the Config file
 		String line = null;
		try{
        BufferedReader Br=new BufferedReader(new FileReader("/sdcard/CMRData.txt"));
     
		while ((line = Br.readLine()) != null)  {
		 
			 pathcmr = line.substring(line.lastIndexOf(":")+1);
			 Log.d("hi", "pathcmr  = " + pathcmr);
		
		
	 }
	 } catch (Throwable throwable) { 
					try {
						throw new ParserException("Error reading config on line : " + line, throwable);
					} catch (ParserException e) {
						
						e.printStackTrace();
					}
				}
	    
 		//Read path from the Config file
 		 cmrcpu = pathcmr.concat("/CPUData.txt");
 		 cmrmem = pathcmr.concat("/MemoryData.txt");
 		 cmrtimersensor = pathcmr.concat("/TimerSensor.txt");
 		 cmrinvosensor = pathcmr.concat("/InvoSensor.txt");
 		 
 		//CPU
 		button1.setOnClickListener(new OnClickListener() {
            @SuppressWarnings({ "unchecked", "deprecation" })
			@Override
			public void onClick(View view) {
				 
					 try{
						 Log.d("hi", "pathcmr1  = " + pathcmr);
						// FileInputStream fin = new FileInputStream("/storage/190B-331F/Documents/CMR Data/CPUData.txt");
						 FileInputStream fin = new FileInputStream(cmrcpu);
						 Log.d("hi", "fin  = " + fin);
						   ObjectInputStream ois = new ObjectInputStream(fin);
						   cpuobj = (List<DefaultData>) ois.readObject();
						   ois.close(); 
				           kryo.sendDataObjects(cpuobj);
						  Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
						  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					      String formattedDate = df.format(c.getTime());
						    textview2.setText(formattedDate);
					}catch(Exception e){}
			 }	
			
 		});
 		
 		//Memory
 		button2.setOnClickListener(new OnClickListener() {
            @SuppressWarnings({ "unchecked", "deprecation" })
			@Override
			public void onClick(View view) {
				 
					 try{
					 
						 FileInputStream fin = new FileInputStream(cmrmem);
						   ObjectInputStream ois = new ObjectInputStream(fin);
						   memobj = (List<DefaultData>) ois.readObject();
						   ois.close(); 
				           kryo.sendDataObjects(memobj);
						  Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
						  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					      String formattedDate = df.format(c.getTime());
						    textview3.setText(formattedDate);
					}catch(Exception e){}
			 }	
			
 		});
 		
 		//Timer Sensor
 		button3.setOnClickListener(new OnClickListener() {
            @SuppressWarnings({ "unchecked", "deprecation" })
			@Override
			public void onClick(View view) {
				 
					 try{
					 
						 FileInputStream fin = new FileInputStream(cmrtimersensor);
						   ObjectInputStream ois = new ObjectInputStream(fin);
						   timers = (List<DefaultData>) ois.readObject();
						   ois.close(); 
				           kryo.sendDataObjects(timers);
						  Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
						  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					      String formattedDate = df.format(c.getTime());
						    textview4.setText(formattedDate);
					}catch(Exception e){}
			 }	
			
 		});
 		
 		//Invo Sensor
 		button4.setOnClickListener(new OnClickListener() {
            @SuppressWarnings({ "unchecked", "deprecation" })
			@Override
			public void onClick(View view) {
				 
					 try{
					 
						 FileInputStream fin = new FileInputStream(cmrinvosensor);
						   ObjectInputStream ois = new ObjectInputStream(fin);
						   invos = (List<DefaultData>) ois.readObject();
						   ois.close(); 
				           kryo.sendDataObjects(invos);
						  Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
						  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					      String formattedDate = df.format(c.getTime());
						    textview5.setText(formattedDate);
					}catch(Exception e){}
			 }	
			
 		});
 		
			
}
		
	//Reading Config File...................................................................................................................................................................	
	 public void loadconfig() throws ParserException {
	    	
			try{
			BufferedReader Br=new BufferedReader(new FileReader("/sdcard/inspectit-agent.cfg"));
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

					// check for a buffer strategy
					if (discriminator.equalsIgnoreCase(CONFIG_BUFFER_STRATEGY)) {
					//	processBufferStrategyLine(tokenizer);
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

					// Check for the method sensor type
					if (discriminator.equalsIgnoreCase(CONFIG_METHOD_SENSOR_TYPE)) {
					 // processMethodSensorTypeLine(tokenizer);
						continue;
					}

					// Check for the platform sensor type
					if (discriminator.equalsIgnoreCase(CONFIG_PLATFORM_SENSOR_TYPE)) {
						//processPlatformSensorTypeLine(tokenizer);
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
      
   
		
//Reading Config File...................................................................................................................................................................	
}
