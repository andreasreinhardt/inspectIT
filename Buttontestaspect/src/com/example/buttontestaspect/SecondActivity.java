package com.example.buttontestaspect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SecondActivity extends Activity {
	ThirdActivity thr;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main1);
		
	}
    
	public void hi(){
	  try{
		  Thread.sleep(3000);
		  thr.hello();
	  }catch(Exception e){}
	}
}
