package com.spring;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.spring.AndroidAgent;


public  aspect Tester {
	
	//AndroidAgent agent = AndroidAgent.INSTANCE;
	//AndroidAgent agent = new AndroidAgent("");
	AndroidAgent agent = new AndroidAgent();
	Methods method = new Methods();
	 
	long start,end,onclickstart,onclickend,onClickduration,t;
	String n,m,n1,m1;
    String clsname,pckgname;
   
    
	pointcut methodCalls():
		  execution(* com.example.buttontestaspect..*(..)) && !within(com.spring.Tester);
      
    pointcut OnClickListener_onClick(View v) :
        execution(void OnClickListener.onClick(View)) && args(v);
     
    before(View v) : OnClickListener_onClick(v) {
    	n = thisJoinPointStaticPart.getSignature().toString();
        onclickstart = System.nanoTime();
    	//display(null,0,0,0,0,0,0,0);
    }
    
    after(View v) : OnClickListener_onClick(v) {
    	onclickend = System.nanoTime();
    	m = thisJoinPointStaticPart.getSignature().toString();
    	clsname = thisJoinPoint.getTarget().getClass().getName();
 
    	onClickduration = (onclickend - onclickstart);
    	Log.d("hi", "onClickduration = " + onClickduration);
    	agent.methodhandler(onclickstart,onclickend,onClickduration,m,clsname);
    }
     
     before(): methodCalls(){
    	 n1 = thisJoinPointStaticPart.getSignature().toString();
         start = System.nanoTime();//Start of execution time of method
	     //display(ts0,null,0,0,0,0,0,0,0);
    }

     after(): methodCalls(){
    	 m1 = thisJoinPointStaticPart.getSignature().toString();
    	 clsname = thisJoinPoint.getTarget().getClass().getName();
    	 end = System.nanoTime();
         t = (end - start);
         agent.methodhandler(start,end,t,m1,clsname);
    }

 }

