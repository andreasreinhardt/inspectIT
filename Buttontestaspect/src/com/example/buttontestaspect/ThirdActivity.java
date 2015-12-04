package com.example.buttontestaspect;



import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class ThirdActivity extends Activity {
	ImageView image;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main2);
        image = (ImageView) findViewById(R.id.imageView1);
     	image.setImageResource(R.drawable.hi);
	}

	public void hello(){
		SecondActivity sec = new SecondActivity();
		sec.hi();
	}
}
