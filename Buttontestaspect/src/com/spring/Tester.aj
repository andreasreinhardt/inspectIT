package com.spring;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.spring.AndroidAgent;

public  aspect Tester {
	
	//AndroidAgent agent = AndroidAgent.INSTANCE;
	//AndroidAgent agent = new AndroidAgent("");
	
	long start,end,onclickstart,onclickend,onClickduration,t;
	long activitystart,activityend,activityduration;
	String n,m,n1,m1,act1,act2;
    String clsname,pckgname;
	  
	AndroidAgent agent = new AndroidAgent();
	//String pkg1 = agent.PackageNameGetter();
	
	//pointcut OnCreateCalls() : execution(void *.onCreate(..));
	
	pointcut onCreateandallmethods():
		  execution(* com.example.buttontestaspect..*(..)) && !within(com.spring.Tester);
  
    //pointcut excludeOnCreatebutincludeallmethods() : onCreateandallmethods() && !OnCreateCalls();
	
    pointcut OnClickListener_onClick(View v) :
        execution(void OnClickListener.onClick(View)) && args(v);
    
  /* before() : OnCreateCalls() {
    	act1 = thisJoinPointStaticPart.getSignature().toString();
    	Log.d("hi", "act1 = " + act1);
    	activitystart = System.nanoTime();
    	Log.d("hi", "activitystart = " + activitystart);
       // agent.beforeinvocation(activitystart,act1);
    }
    
    after() : OnCreateCalls() {
    	activityend = System.nanoTime();
    	Log.d("hi", "activityend = " + activityend);
    	act2 = thisJoinPointStaticPart.getSignature().toString();
    	Log.d("hi", "act2 = " + act2);
    	activityduration = (activityend - activitystart);
    	Log.d("hi", "activityduration = " + activityduration);
    	store(activitystart,activityend,activityduration,act2,clsname);
    	//agent.afterinvocation(activityend,activityduration,act2);
    	//agent.secondafterbody();
    }*/
    
   
     before() : onCreateandallmethods(){
    	n1 = thisJoinPointStaticPart.getSignature().toString();
    	start = System.nanoTime();
    	agent.beforeinvocation(start,n1);
    }
    
    after() : onCreateandallmethods(){
    	m1 = thisJoinPointStaticPart.getSignature().toString();
      	end = System.nanoTime();
        t = (end - start);
        store(start,end,t,m1,clsname);
        agent.afterinvocation(end,t,m1);
        //agent.secondafterbody();
    }
     
     
    
   /* before() : excludeOnCreatebutincludeallmethods(){
    	n1 = thisJoinPointStaticPart.getSignature().toString();
    	start = System.nanoTime();
        //agent.beforeinvocation(start,n1);
    }
    
    after() : excludeOnCreatebutincludeallmethods(){
    	m1 = thisJoinPointStaticPart.getSignature().toString();
      	end = System.nanoTime();
        t = (end - start);
        store(start,end,t,m1,clsname);
        //agent.afterinvocation(end,t,m1);
       // agent.secondafterbody();
    }*/
    
    
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
    	//agent.secondafterbody();
    }
   
    public void store(long s,long e1,long d,String met,String cls){
    	 Log.d("hi", "Method name : " + met);
        agent.methodhandler(s, e1, d, met, cls);
     }
 }

