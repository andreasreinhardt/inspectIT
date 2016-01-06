package com.example.cmrdatasender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import com.example.cmrdatasender.KryoNetConnection;
import com.example.cmrdatasender.ParserException;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.data.CpuInformationData;

public class MainActivity extends Activity {
	
	 private static final String CONFIG_COMMENT = "#";
	 private static final String CONFIG_REPOSITORY = "repository";
	 private static final String CONFIG_SEND_STRATEGY = "send-strategy";
	 private static final String CONFIG_BUFFER_STRATEGY = "buffer-strategy";
	 private static final String CONFIG_METHOD_SENSOR_TYPE = "method-sensor-type";
	 private static final String CONFIG_PLATFORM_SENSOR_TYPE = "platform-sensor-type";
	String hostip;
	String port;
	int portaddress;
	String agentname;
	String agentversion;
	KryoNetConnection kryo;
	Button button1,button2,button3,button4;
	FileInputStream is;
	BufferedReader reader;
	InputStream instream;
	public Map<String, DefaultData> cpusensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU
	CPUClass cpudata;
	String line;
	String line1;
	
	
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
 		
 		//CPU
 		button1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				 
				line = readline();
			               //line1 = readline();
						   cpudata = new CPUClass();
						   cpudata.setCpuValue(line1);
						   String line13;
						   
						   
						   List<DefaultData> tempList = new ArrayList<DefaultData>();
						   tempList.add(cpudata);
						   Log.d("hi", "VIVCPU2 = " + tempList);
						   kryo.sendDataObjects(tempList);
			}
 		

	});
}
		//CPU
		
	public String readline() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("/storage/190B-331F/Documents/CPUData/CPU.txt");
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		 
		//Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
	 
		
		try {
			while ((line = br.readLine()) != null) {
				Log.d("hi", "VIVline0 = " +  line);
				if(line!=null){
					Log.d("hi", "VIVline3 = " +  line);
					return line;
				}else{}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	 
		
		Log.d("hi", "VIVline = " +  line);
		return line;
       
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
      
   
		
//Reading Config File...................................................................................................................................................................	
}

