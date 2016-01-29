package com.spring;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.spring.AndroidAgent;

public  aspect Tester {

	long start,end,onclickstart,onclickend,onClickduration,t;
	long activitystart,activityend,activityduration;
	String n,m,n1,m1,act1,act2;
    String clsname,pckgname;
	  
	AndroidAgent agent = new AndroidAgent();
	
	pointcut onCreateandallmethods():
		  execution(* com.example.buttontestaspect..*(..)) && !within(com.spring.Tester);
  
    pointcut OnClickListener_onClick(View v) :
        execution(void OnClickListener.onClick(View)) && args(v);
    
     before() : onCreateandallmethods(){
    	n1 = thisJoinPointStaticPart.getSignature().toString();//thisJoinPointStaticPart gives the context of the advice
    	start = System.nanoTime();
    	agent.beforeinvocation(start,n1);
    }
    
    after() : onCreateandallmethods(){
    	m1 = thisJoinPointStaticPart.getSignature().toString();
    	Log.d("hi", "MTVIV0 " + m1);
      	end = System.nanoTime();
        t = (end - start);
        Log.d("hi", "MTVIV1 " + t);
        store(start,end,t,m1,clsname);
        agent.afterinvocation(end,t,m1);
    }
     
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
   
    public void store(long s,long e1,long d,String met,String cls){
    	 Log.d("hi", "Method name : " + met);
        agent.methodhandler(s, e1, d, met, cls);
     }
 }

