package com.example.buttontestaspect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
Button button1,button2;
SecondActivity sec;
ThirdActivity thr;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tetsmethod();
		 button1 = (Button)findViewById(R.id.button1);
		 button2 = (Button)findViewById(R.id.button2);
		 button1.setOnClickListener(new OnClickListener() {
			            @Override
			 	            public void onClick(View view) {
			            	//SecondActivity sec = new SecondActivity();
			 	             //ThirdActivity thr = new ThirdActivity();
			 	              //sec.hi();
		                      //thr.hello();
			            	
			 	              Intent hi =  new Intent(MainActivity.this,SecondActivity.class);
			 	              startActivity(hi);
			 	              
			 	            }
			});
		 
		 button2.setOnClickListener(new OnClickListener() {
	            @Override
	 	            public void onClick(View view) {
	            	try{
	                	  Thread.sleep(6000);
	                  }catch(Exception e){}
	 	              Intent hi =  new Intent(MainActivity.this,ThirdActivity.class);
	 	              startActivity(hi);
	 
	 	            }
	 
	 	 
	 
	 	        });
	}
	
	public void tetsmethod(){
		
	}


}
