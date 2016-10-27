package org.ryan.naxin;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class ExitDialog extends Activity implements OnClickListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.exit);
		
		findViewById(R.id.exit_yes).setOnClickListener(this);
		findViewById(R.id.exit_no).setOnClickListener(this);
		
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.exit_yes:
				this.finish();
				MainActivity.mInstance.finish();
				break;
		
				
			case R.id.exit_no:
				this.finish();
				break;
				
			default:
				break;
		}
		
	}

	
}
