package com.spring;

import java.sql.Timestamp;
import java.util.GregorianCalendar;

import android.util.Log;
import info.novatec.inspectit.communication.data.MemoryInformationData;

public class Memory {
	
	long usedHeapMemorySize;
	
	public void refresh(){
		
	}
	
	public long getUsedHeapMemorySize() {
		 Runtime rt = Runtime.getRuntime();
			long maxMemory = rt.maxMemory();//Returns the maximum number of bytes the heap can expand to
			long totalMemory = rt.totalMemory();//Returns the number of bytes taken by the heap at its current size.
			long freeMemory = rt.freeMemory();//Returns the number of bytes currently available on the heap without expanding the heap
			long usedMemory = (maxMemory - (freeMemory + (maxMemory - totalMemory)))/(1024000);
			Log.d("hi", "heapmem" + usedMemory);
			return usedMemory;
	}

}
