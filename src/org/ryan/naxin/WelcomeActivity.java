package org.ryan.naxin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class WelcomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.welcome);
		
		new Handler().postDelayed(new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				Intent it = new Intent(WelcomeActivity.this, LoginActivity.class);
				WelcomeActivity.this.startActivity(it);
				
				WelcomeActivity.this.finish();
			}
		}, 1000);
		
	}
}
