package com.example.buttontestaspect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SecondActivity extends Activity {
	//ThirdActivity thr;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main1);
		hi();
	}
	
    
	public void hi(){
	 hello();
	}
	
	public int hello(){
		bye();
		return 5;
	}
	
	public void bye(){
		try{
		Thread.sleep(6000);
		}catch(Exception e){}
	}
}
