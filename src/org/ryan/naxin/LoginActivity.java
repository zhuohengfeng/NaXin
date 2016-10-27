package org.ryan.naxin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private String userName = "";
	private int userImgId = 0;
	private int userID = 0;
	
	private GridView mGridView = null;
	private MyGridViewAdapter mMyGridViewAdapter = null;
	
	private SharedPreferences mSharedPreferences = null;
	private SharedPreferences.Editor mEditor = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.login);
		
		
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mEditor = mSharedPreferences.edit();
		
		userName = mSharedPreferences.getString(Constant.userNameStr, "");
		userImgId =mSharedPreferences.getInt(Constant.userImgStr, 0);
		userID =mSharedPreferences.getInt(Constant.userIDStr, 0);
		
		
		//Log.i(Constant.TAG, "userName:"+userName+"+  " + userImgId);
		if(userID != 0){
			// 之前已经有配置
			((EditText)findViewById(R.id.login_tx_username)).setText(userName);
		}
		else{
			// 如果之前没有设置，产生一个随机数
			userID = Constant.getMyId();
		}
		((ImageView)findViewById(R.id.login_img_logo)).setImageResource(Constant.mThumbIds[userImgId]);
		
		
		mGridView = (GridView)findViewById(R.id.login_gridView);
		mMyGridViewAdapter = new MyGridViewAdapter();
		mGridView.setAdapter(mMyGridViewAdapter);
		mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				userImgId = arg2;
				((ImageView)findViewById(R.id.login_img_logo)).setImageResource(Constant.mThumbIds[userImgId]);
				//Toast.makeText(LoginActivity.this, "select :" + arg2, Toast.LENGTH_SHORT).show();
			}
		});
		
		
		// 登录按钮
		findViewById(R.id.login_bt_login).setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String userNameTemp = ((EditText)findViewById(R.id.login_tx_username)).getText().toString();
				if(userNameTemp.equals("")){
					Toast.makeText(LoginActivity.this, R.string.login_name_empty, Toast.LENGTH_SHORT).show();
				}
				else{
					
					if(!userNameTemp.equals(userName)){
						userName = userNameTemp;
						//userID = Constant.getMyId(); //每台机器的ID是固定的
					}

					mEditor.putString(Constant.userNameStr, userName);
					mEditor.putInt(Constant.userImgStr, userImgId);
					mEditor.putInt(Constant.userIDStr, userID);
					mEditor.commit();
					
					Intent intent = new Intent(LoginActivity.this, MainActivity.class);
					LoginActivity.this.startActivity(intent);
					LoginActivity.this.finish();
				}
			}
		});
		
		// 返回按钮
		findViewById(R.id.login_title_return_bn).setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		
	}
	
	
	
	//-----------定义图片显示的adapter----------
	private class MyGridViewAdapter extends BaseAdapter{

		public int getCount() {
			// TODO Auto-generated method stub
			return Constant.mThumbIds.length;
		}

		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ImageView imageview;
			if(convertView==null)
			{
			   imageview=new ImageView(LoginActivity.this);
			   imageview.setLayoutParams(new GridView.LayoutParams(90, 90));
			   imageview.setScaleType(ImageView.ScaleType.CENTER_CROP);
			   imageview.setPadding(6,20,6,20);
			}
			else
			{	
			   imageview=(ImageView) convertView;
			}
			
			imageview.setImageResource(Constant.mThumbIds[position]);
			return imageview;
		}
		
		
	}
	
	
	
	
	
	
}
