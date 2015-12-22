package com.spring;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.spring.AndroidAgent;
import android.app.Activity;

public  aspect Tester {
	
	//AndroidAgent agent = AndroidAgent.INSTANCE;
	//AndroidAgent agent = new AndroidAgent("");
	
	long start,end,onclickstart,onclickend,onClickduration,t;
	long activitystart,activityend,activityduration;
	String n,m,n1,m1,a,b;
    String clsname,pckgname;
	  
	AndroidAgent agent = new AndroidAgent();
	//String pkg1 = agent.PackageNameGetter();
	
	
	
	pointcut methodCalls():
		  execution(* com.example.buttontestaspect..*(..)) && !within(com.spring.Tester);
	
    pointcut OnClickListener_onClick(View v) :
        execution(void OnClickListener.onClick(View)) && args(v);
    
  
    
    before(View v) : OnClickListener_onClick(v) {
    	n = thisJoinPointStaticPart.getSignature().toString();
        onclickstart = System.nanoTime();
        agent.beforeinvocation(onclickstart,n);
    }
    
    after(View v) : OnClickListener_onClick(v) {
    	onclickend = System.nanoTime();
    	m = thisJoinPointStaticPart.getSignature().toString();
    	onClickduration = (onclickend - onclickstart);
    	Log.d("hi", "onClickduration = " + onClickduration);
    	store(onclickstart,onclickend,onClickduration,m,clsname);
    	agent.afterinvocation(onclickend,onClickduration,m);
    }
     
     before(): methodCalls(){
    	 n1 = thisJoinPointStaticPart.getSignature().toString();
         start = System.nanoTime();
         agent.beforeinvocation(start,n1);
     }
         
     after(): methodCalls(){
    	 m1 = thisJoinPointStaticPart.getSignature().toString();
    	 end = System.nanoTime();
         t = (end - start);
         store(start,end,t,m1,clsname);
         agent.afterinvocation(end,t,m1);
    }
     
     public void store(long s,long e1,long d,String met,String cls){
    	 Log.d("hi", "Method name : " + met);
        agent.methodhandler(s, e1, d, met, cls);
     }
     
     

 }

