package com.example.cmrdatasender;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;
import info.novatec.inspectit.communication.DefaultData;
import info.novatec.inspectit.communication.SystemSensorData;

public class CPUClass extends SystemSensorData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	FileInputStream is;
	BufferedReader reader;
	InputStream instream;
	String line;
	public Map<String, DefaultData> cpusensorDataObjects = new ConcurrentHashMap<String, DefaultData>();//CPU  
	
	public String line12;
	
	public void setCpuValue(String line12) {
		// TODO Auto-generated method stub
		this.line12 = line12;
	}
	

	
	
	

	}


