package com.spring;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.spring.AndroidAgent;

public  aspect Tester {
 
 	double start,end,onclickstart,onclickend,onClickduration,t;
 	double activitystart,activityend,activityduration;
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
     
       	end = System.nanoTime();
         t = (end - start);
         
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
     	
     	store(onclickstart,onclickend,onClickduration,m,clsname);
     	agent.afterinvocation(onclickend,onClickduration,m);
     }
    
     public void store(double s,double e1,double d,String met,String cls){

        agent.methodhandler(s, e1, d, met, cls);
      }
  }
 
