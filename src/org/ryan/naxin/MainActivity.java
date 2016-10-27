package org.ryan.naxin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity {
	private static final String TAG = "NaXin";
	private static final String FileSavePath = "/mnt/sdcard";
	
	private boolean isRunning = true;
	public static MainActivity mInstance = null;
	private Person mMySelf = null;
	
	private ViewPager mViewPager = null;
	private RadioButton mBnChat = null;
	private RadioButton mBnAddress = null;
	private RadioButton mBnFriends = null;
	private RadioButton mBnSetting = null;
	
	
	private MyPagerAdapter mPageAdapter = null;
	private final ArrayList<View> mArrayList = new ArrayList<View>();
	
	private PopupWindow mPopupWindow = null;
	private boolean mPopupWindowShow = false;
	
	private ExpandableListView mExpandListView = null;
	private MyExListAdapter mExListAdapter = null;
	

	private SharedPreferences mSharedPreferences = null;
	private String userName = "";
	private int userImgId = 0;
	private int userID = 0;
	
	public ArrayList<Map<Integer,Person>> children = new ArrayList<Map<Integer,Person>>();
	public Map<Integer,Person> childrenMap = new HashMap<Integer,Person>();
	public ArrayList<Integer> personKeys = new ArrayList<Integer>();

	//------------------------------------------------------
	// 在联系人界面显示网络情况
	private TextView mWifiFailedText = null;
	
	//------------------------------------------------------
	// 在“会话”界面显示已有的聊天记录
	private String[] mMessageMapIndexStr = {"UserImg","UserName","Message","LastTime","GroupID"};
	private ListView mMessageListView = null;
	// 数据库操作
	private DatabaseHelper mDatabaseHelper = null;
	private SimpleCursorAdapter mMessageSimpleAdapter = null;
	
	/*******************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
		mInstance = this;
		initView();
	}

	private void initView(){
		mViewPager = (ViewPager)findViewById(R.id.main_viewpager);
		
		// 装载TAB VIEW
		LayoutInflater mInflater = LayoutInflater.from(this);
		mArrayList.add(mInflater.inflate(R.layout.chat_tab, null));
		mArrayList.add(mInflater.inflate(R.layout.address_tab, null));
		mArrayList.add(mInflater.inflate(R.layout.friends_tab, null));
		mArrayList.add(mInflater.inflate(R.layout.setting_tab, null));
		
		
		for(int i=0;i<4;i++){
			(mArrayList.get(i)).setOnTouchListener(new View.OnTouchListener() {
				// 如果Pop windows弹出，则销毁
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if(mPopupWindowShow){
						mPopupWindow.dismiss();
						mPopupWindowShow = false;
					}
					return false;
				}
			});
		}
		
		//--------------设置界面响应的onTouch事件--------------------
		ScrollView mScrollView = (ScrollView) (mArrayList.get(3)
				.findViewById(R.id.setting_tab_scrollView));
		
		mScrollView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(mPopupWindowShow){
					mPopupWindow.dismiss();
					mPopupWindowShow = false;
				}
				return false;
			}
		});
		
		
		// -------------获取ExpandableListView实例-----------------
		mExListAdapter = new MyExListAdapter(this);
		mExpandListView = (ExpandableListView) (mArrayList.get(1)
								.findViewById(R.id.expandableListView_address));
		mExpandListView.setAdapter(mExListAdapter);
		mExpandListView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(mPopupWindowShow){
					mPopupWindow.dismiss();
					mPopupWindowShow = false;
				}
				return false;
			}
		});
		
		mExpandListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				// TODO Auto-generated method stub
				Intent mIntent = new Intent(MainActivity.this, ChatActivity.class);
				Person chatPerson = childrenMap.get(personKeys.get(childPosition));
				Bundle bd = new Bundle();
				bd.putSerializable(Constant.userNameStr, chatPerson);
				bd.putSerializable(Constant.myInfoStr, mMySelf);
				bd.putString(Constant.userNewMsgStr, "");
				mIntent.putExtras(bd);
				MainActivity.this.startActivity(mIntent);
				return false;
			}
		});
		
		
		// 设置适配器
		mPageAdapter = new MyPagerAdapter();
		mViewPager.setAdapter(mPageAdapter);
		mViewPager.setCurrentItem(1); //默认启动联系人页面
		
		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub
				switch(arg0){
					case 0:
						mBnChat.setChecked(true);
						updateTheMessageList();
						break;
						
					case 1:
						mBnAddress.setChecked(true);
						break;
					
					case 2:
						mBnFriends.setChecked(true);
						break;
					
					case 3:
						mBnSetting.setChecked(true);
						break;
				
				}
			}
			
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
			
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
		// 绑定Radio监听事件
		mBnChat = (RadioButton)findViewById(R.id.radioButton1);
		mBnAddress = (RadioButton)findViewById(R.id.radioButton2);
		mBnFriends = (RadioButton)findViewById(R.id.radioButton3);
		mBnSetting = (RadioButton)findViewById(R.id.radioButton4);
		mBnChat.setOnClickListener(new MyRaidoClickListener(0));
		mBnAddress.setOnClickListener(new MyRaidoClickListener(1));
		mBnFriends.setOnClickListener(new MyRaidoClickListener(2));
		mBnSetting.setOnClickListener(new MyRaidoClickListener(3));
		mBnAddress.setChecked(true);
		
		/*----------------获得个人信息----------------------------*/
		getMyInformation();
		// 在设置页面设置自己的信息
		((TextView)((mArrayList.get(3).findViewById(R.id.setting_username)))).setText(userName);
		((ImageView)((mArrayList.get(3).findViewById(R.id.setting_userimg))))
										.setImageResource(Constant.mThumbIds[userImgId]);
		
		
		
		/*----------------显示已有的对话列表----------------------------*/
		mMessageListView = (ListView) (mArrayList.get(0).findViewById(R.id.message_listView));
		// 初始化数据库
		mDatabaseHelper = new DatabaseHelper(this);
		
		// 开始读取数据库获取聊天记录
		mMessageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				final String GroupID = ((TextView)(arg1.findViewById(R.id.db_id))).getText().toString();
				if(Constant.DEBUG) Log.e("database", "the short click!!!!!"+arg2+"  "+arg3+" the ID is="+GroupID);
				
				// 启动聊天窗口
				Intent mChatIntent = new Intent(MainActivity.this, ChatActivity.class);
				Person chatPerson = childrenMap.get(Integer.valueOf(GroupID));
				
				if(chatPerson != null){
					Bundle bd = new Bundle();
					bd.putSerializable(Constant.userNameStr, chatPerson);
					bd.putSerializable(Constant.myInfoStr, mMySelf);
					bd.putString(Constant.userNewMsgStr, "");
					mChatIntent.putExtras(bd);
					MainActivity.this.startActivity(mChatIntent);
				}
				else{
					Toast.makeText(MainActivity.this, "此用户当前不在线！！！", Toast.LENGTH_SHORT).show();
				}

			}
        	
		});
        
		mMessageListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				final String GroupID = ((TextView)(arg1.findViewById(R.id.db_id))).getText().toString();
				if(Constant.DEBUG) Log.e("database", "the long click!!!!!"+arg2+"  "+arg3+" the ID is="+GroupID);
				
				AlertDialog.Builder mbuild = new AlertDialog.Builder(MainActivity.this);
				mbuild.setMessage("是否删除记录")
					  .setPositiveButton("是", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							mDatabaseHelper.delGroup(Integer.valueOf(GroupID));
							
							// 更新消息列表
							updateTheMessageList();
						}
					})
					.setNegativeButton("否", null)
					.setTitle(null);
				
				mbuild.create().show();
				
				return false;
			}
		});
        
        // 关闭数据库
        mDatabaseHelper.close();
		
		
		/*----------------获得网络信息----------------------------*/
		mWifiFailedText = ((TextView)((mArrayList.get(1).findViewById(R.id.wifi_failed))));
		mWifiFailedText.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(android.os.Build.VERSION.SDK_INT > 10 ){
				     //3.0以上打开设置界面，也可以直接用ACTION_WIRELESS_SETTINGS打开到wifi界面
				    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
				} else {
				    startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
				}
				
			}
		});
		// 判断当前网络是否可以连接
		ConnectivityManager conManager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = conManager.getActiveNetworkInfo();
        boolean bisConnFlag = false;
        if(network!=null){
            bisConnFlag=conManager.getActiveNetworkInfo().isAvailable();
        }
		if(bisConnFlag){
			//如果已经连接上了，就不显示网络连接异常
			mWifiFailedText.setVisibility(View.GONE);
			isServerStart = false;
		}
		else{
			//如果网络连接上了就开始打开后台服务
			/*----------------启动通信服务----------------------------*/
			StartMyService();
			isServerStart = true;
		}
		
		
		/*----------------注册广播事件----------------------------*/
		RegBroadcastReceiver();
		
		//开始我的纳信.....
		new CheckPersonOnlineThread().start();
		
		isRunning = true;
		if(Constant.DEBUG) Log.i(TAG, "Finish the initView....");
	}
	
	
	
	private void updateTheMessageList(){
		// 初始化List的数据
		//public Map<Integer,Person> childrenMap = new HashMap<Integer,Person>();
		//public ArrayList<Integer> personKeys = new ArrayList<Integer>();
		
		
		Cursor myCursor = mDatabaseHelper.queryGroupTop();
		mMessageSimpleAdapter = new SimpleCursorAdapter(this, R.layout.message_list, 
				myCursor, 
				mMessageMapIndexStr, //{"UserImg","UserName","Message","TheLastTime","_id"};
				new int[]{R.id.message_img, R.id.message_name, R.id.message_msg, R.id.message_time, R.id.db_id});
		
		mMessageListView.setAdapter(mMessageSimpleAdapter);
		mMessageSimpleAdapter.notifyDataSetChanged();
		
        // 关闭数据库
        mDatabaseHelper.close();
	}
	
	
	
	
	
	
	
	
	private class MyRaidoClickListener implements View.OnClickListener{
		private int mIndex = 0;
		
		public MyRaidoClickListener(int index) {
			if(index < 4){
				this.mIndex = index;
			}
		}

		public void onClick(View v) {
			mViewPager.setCurrentItem(this.mIndex);
		}
	}
	
	
	
	
	/**
	 * define the ViewPager Adapter
	 * @author Owner
	 * 
	 */
	private class MyPagerAdapter extends PagerAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mArrayList.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == (arg1);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// TODO Auto-generated method stub
			((ViewPager)container).addView(mArrayList.get(position));
			return mArrayList.get(position);
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			// TODO Auto-generated method stub
			((ViewPager)container).removeView(mArrayList.get(position));
		}
		
	}



	/**
	 * Handle the BACK and MENU key event
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
			if(mPopupWindowShow){
				mPopupWindow.dismiss();
				mPopupWindowShow = false;
			}
			else{
				Intent intent = new Intent(this, ExitDialog.class);
				this.startActivity(intent);
			}
			
		}
		else if(keyCode == KeyEvent.KEYCODE_MENU){
			
			if(mPopupWindowShow){
				mPopupWindow.dismiss();
				mPopupWindowShow = false;
			}
			else{
				LayoutInflater inflater = LayoutInflater.from(this);
				View popupView = inflater.inflate(R.layout.popupmenu, null);
				
				mPopupWindow = new PopupWindow(popupView, LayoutParams.MATCH_PARENT, 
												LayoutParams.WRAP_CONTENT);
				
				mPopupWindow.showAtLocation(this.findViewById(R.id.main_layout),
											Gravity.BOTTOM|Gravity.CENTER, 0, 0);
				
				popupView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						// TODO Auto-generated method stub
						mPopupWindow.dismiss();
						mPopupWindowShow = false;
						
						Intent mIntent = new Intent(MainActivity.this, ExitDialog.class);
						MainActivity.this.startActivity(mIntent);
						
					}
				});
				
				// 为了让父控件获取到焦点，相应OnTouch事件
				//mPopupWindow.setFocusable(false);
				//mPopupWindow.setOutsideTouchable(true);

				mPopupWindowShow = true;
			}
		}
		
		return true;
	}
		
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		// 注意退出APP的时候要停止服务,取消广播注册
    	unregisterReceiver(mBroadcastReceiver);
    	stopService(mServiceIntent);
		unbindService(mServiceConn);
		mMainService = null;
		isRunning = false;
	}

	
	//---------------获取用户信息-----------------------
	private void getMyInformation(){
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		userName = mSharedPreferences.getString(Constant.userNameStr, "");
		userImgId =mSharedPreferences.getInt(Constant.userImgStr, 0);
		userID = mSharedPreferences.getInt(Constant.userIDStr, 0);
		
		mMySelf = new Person(userID,userName,userImgId, "192.168.0.1");
		
		if(Constant.DEBUG) Log.i(Constant.TAG, "Activity GetInformation : userName="+userName+"  ImgId="+userImgId +"  userID" +userID);
	}

	//----------------启动一个线程检查是否有用户离线------------------------
	private final class CheckPersonOnlineThread extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			
			while(isRunning){
				
				// Map里面包含了所以用户信息，用keySet返回所有的key
				Set<Integer> myKeys = childrenMap.keySet();
				for(Integer key : myKeys){
					Person checkPerson = childrenMap.get(key);
					long diffTime = System.currentTimeMillis() - checkPerson.getTimeStamp();
					//if(Constant.DEBUG) Log.d(TAG,"diffTime:"+diffTime+"  TimeStamp:"+checkPerson.getTimeStamp());
					if(diffTime > 15000){//15s
						
						// 删除用户ID，这个ID是识别用户的唯一ID
						personKeys.remove(key);
						
						// 删除Map中的指定Person
						childrenMap.remove(key);
						
						
						// 发送消息
						Message removeOneMsg = new Message();
						removeOneMsg.what = Constant.MSG_removeOne;
						mainHandler.sendMessage(removeOneMsg);
					}
				}
				
				// Delay 1s
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	// -----------------启动服务-------------------------
	// 定义service的Intent
	private Intent mServiceIntent = null;
	private MainService mMainService = null;
	private boolean isServerStart = false;
	private ServiceConnection mServiceConn = new ServiceConnection(){
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			// 这里的IBinder是本地的，所以可以直接使用
			mMainService = ((MainService.MyBinder)service).getServiceInstance();
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			Toast.makeText(MainActivity.this, "the Service is Disconnedted!", Toast.LENGTH_LONG).show();
			mMainService = null;
		}
		
	};
	
	private void StartMyService(){
		mServiceIntent = new Intent(MainActivity.this, MainService.class);
		mServiceIntent.putExtra(Constant.userIDStr, userID);
		mServiceIntent.putExtra(Constant.userImgStr, userImgId);
		mServiceIntent.putExtra(Constant.userNameStr, userName);
		
		this.bindService(mServiceIntent, mServiceConn, BIND_AUTO_CREATE);
		this.startService(mServiceIntent);
	}
	
	
	// ----------------注册广播事件----------------------
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		private ProgressDialog mProgressDialog = null;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String mainAction = intent.getAction();
			//if(Constant.DEBUG) Log.i(TAG, "BroadcastReceiver receiver ==>"+ mainAction);
			
			if(mainAction.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
				// 获取当前WIFI状态
				Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);    
				if (null != parcelableExtra) {    
					NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;    
					State state = networkInfo.getState();  
					if(state==State.CONNECTED){  
						//Toast.makeText(MainActivity.this, "WIFI网络已经连接上了!!!", Toast.LENGTH_SHORT).show();
						mWifiFailedText.setVisibility(View.GONE);
						
						if(!isServerStart){
							StartMyService();
							isServerStart = true;
						}
					}  
					else{
						//Toast.makeText(MainActivity.this, "WIFI断开了!!!", Toast.LENGTH_SHORT).show();
						mWifiFailedText.setVisibility(View.VISIBLE);
					}
				}
			}
			else if(mainAction.equals(Constant.BR_NewUser_Update)){
				String recvUserName = intent.getExtras().getString(Constant.userNameStr);
				int recvUserImgID = intent.getExtras().getInt(Constant.userImgStr);
				int recvUserID = intent.getExtras().getInt(Constant.userIDStr);
				String recvUserHost = intent.getExtras().getString(Constant.userHostStr);
				
				if(!personKeys.contains(Integer.valueOf(recvUserID))){
					// 创建一个Person对象，覆盖旧对象
					Person mPerson = new Person(recvUserID,recvUserName,recvUserImgID,recvUserHost);
					
					// 存入用户ID，这个ID是识别用户的唯一ID
					personKeys.add(Integer.valueOf(recvUserID));
					// 用户ID和一个具体的用户person组成MAP结构，可以通过ID来找到mPerson
					childrenMap.put(Integer.valueOf(recvUserID), mPerson);
					
					if(children.size() != 0)
						children.set(0, childrenMap);
					else{
						children.add(0, childrenMap);
					}
					
					//if(Constant.DEBUG) Log.i(TAG,"add a new person, timeStamp:"+mPerson.getTimeStamp());
					
				}
				else{
					// 已经包含了这个person更新
					// 创建一个Person对象，覆盖旧对象
					Person mPerson = new Person(recvUserID,recvUserName,recvUserImgID,recvUserHost);
					// 用户ID和一个具体的用户person组成MAP结构，可以通过ID来找到mPerson
					childrenMap.put(Integer.valueOf(recvUserID), mPerson);
					
				}
				mExListAdapter.notifyDataSetChanged();
				if(Constant.DEBUG) Log.i(TAG, "BR_NewUser_Update recvUserID="+recvUserID+"  recvUserImgID="+recvUserImgID+"  recvUserName="+recvUserName);
			}
			//-------------
			else if(mainAction.equals(Constant.BR_NewMessage)){
				
				if(!isActivityOnTop("ChatActivity")){
					String message = intent.getExtras().getString(Constant.userNewMsgStr);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					//if(Constant.DEBUG) Log.e(Constant.TAG,"UID="+recvUID+"  message="+message);
					Person recvPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					// 发送Notification
					NotificationManager notifyManger = (NotificationManager)(MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE)); 
					Notification notify = new Notification();
					notify.icon = R.drawable.ic_launcher;// 设置图标
					notify.tickerText = "您有新的消息"; 
					notify.flags = Notification.FLAG_AUTO_CANCEL;
					notify.defaults = Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE; // 设置铃声
					
					long[]vibrate = new long[]{1000,1000,1000,1000,1000};
					notify.vibrate  = vibrate; // 设置震动
					
					Intent mIntent = new Intent(MainActivity.this, ChatActivity.class);  
					Bundle bd = new Bundle();
					bd.putSerializable(Constant.userNameStr, recvPerson);
					bd.putSerializable(Constant.myInfoStr, mMySelf);
					bd.putString(Constant.userNewMsgStr, message);
					mIntent.putExtras(bd);
					
					
					PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 
													0, mIntent, PendingIntent.FLAG_ONE_SHOT);
					
					notify.setLatestEventInfo(MainActivity.this, 
											recvPerson.getName()+"说：", 
											message, 
											contentIntent );
					
					notifyManger.notify(0, notify);
					
				}
				
			}
			//-------------
			else if(mainAction.equals(Constant.BR_RecvNewFile)){
				if(!isActivityOnTop("ChatActivity")){
					final String fileName = intent.getExtras().getString(Constant.RecvFileName);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					final Person chatPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					AlertDialog.Builder alert= new AlertDialog.Builder(MainActivity.this);
					
					alert.setMessage(chatPerson.getName()+"给您发送了一个文件："+fileName+",是否接收?");
					alert.setPositiveButton("接收", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// 先把对话框销毁
							if(dialog != null)
								dialog.cancel();
							
							// 弹出进度条对话框
							// 接收收到发送的字节数，弹出对话框显示
							mProgressDialog = new ProgressDialog(MainActivity.this);
							// 设置mProgressDialog风格为长形
							mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
							// 设置mProgressDialog标题
							mProgressDialog.setTitle("接收文件中...");
							// 设置mProgressDialog的进度条是否不明确
							mProgressDialog.setIndeterminate(false);
							// 是否可以按回退键取消
							mProgressDialog.setCancelable(true);
		                    // 设置mProgressDialog的一个Button
							mProgressDialog.setButton("确定", new DialogInterface.OnClickListener()
						    {
						         public void onClick(DialogInterface dialog, int which)
						         {
						             dialog.cancel();
						         }
						    });
							// 显示mProgressDialog
							mProgressDialog.show();
							
							
							
							// 通过后台服务发送消息
							mMainService.startRecvFile(chatPerson, fileName);
						}
					});
					alert.setNegativeButton("取消",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// 拒绝接收文件
							mMainService.rejectRecvFile(chatPerson, fileName);
						}
					});
					
					AlertDialog dialog = alert.create();
					dialog.show();
				}
				
			}
			//-------------
			else if(mainAction.equals(Constant.BR_ReceveFileDone)){
				if(!isActivityOnTop("ChatActivity")){
					String mRecvDone = intent.getExtras().getString(Constant.HandleFileName);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					Person recvPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					// 发送Notification
					NotificationManager notifyManger = (NotificationManager)(MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE)); 
					Notification notify = new Notification();
					notify.icon = R.drawable.ic_launcher;// 设置图标
					notify.tickerText = "接收文件成功"; 
					notify.flags = Notification.FLAG_AUTO_CANCEL;
					notify.defaults = Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE; // 设置铃声
					
					long[]vibrate = new long[]{1000,1000,1000,1000,1000};
					notify.vibrate  = vibrate; // 设置震动
					
					Intent mIntent = new Intent(MainActivity.this, ChatActivity.class);  
					Bundle bd = new Bundle();
					bd.putSerializable(Constant.userNameStr, recvPerson);
					bd.putSerializable(Constant.myInfoStr, mMySelf);
					bd.putString(Constant.userNewMsgStr,"文件接收成功，存放在："+FileSavePath+mRecvDone);
					mIntent.putExtras(bd);
					PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 
													0, mIntent, PendingIntent.FLAG_ONE_SHOT);
					
					notify.setLatestEventInfo(MainActivity.this, 
											recvPerson.getName()+"的文件接收成功", 
											"文件存放在："+FileSavePath+mRecvDone, 
											contentIntent );
					
					notifyManger.notify(0, notify);
				}
			}
			//-------------
			else if(mainAction.equals(Constant.BR_RecvFileSize)){
				if(!isActivityOnTop("ChatActivity")){
					long totalSize = intent.getExtras().getLong(Constant.totalSizes);
					long hasRecvSize = intent.getExtras().getLong(Constant.hasRecvSize);
					 //if(Constant.DEBUG) Log.e(Constant.TAG, "Send the file bytes:"+hasRecvSize+" totalSize:"+totalSize);
					
					if(mProgressDialog != null){
						// 发送文件的进度
						int rate = (int)(hasRecvSize*100/totalSize);
						mProgressDialog.setProgress(rate);
						// 发送完毕
						if(rate >= 100){
							mProgressDialog = null;
						}
					}
				}
				
			}
			//--------------------------------------
			else if(mainAction.equals(Constant.BR_AudioRequest)){
				// 收到请求语音聊天的命令
				if(!isActivityOnTop("ChatActivity")){
					AlertDialog.Builder alert= new AlertDialog.Builder(MainActivity.this);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					final Person chatPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					alert.setMessage(chatPerson.getName()+"请求进行语音通话,是否接受?");
					alert.setPositiveButton("接收", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// 通过后台服务发送消息
							Intent mSendAudioIntent = new Intent(MainActivity.this, SendAudio.class);
							Bundle bd = new Bundle();
							bd.putSerializable(Constant.userNameStr, chatPerson);
							bd.putSerializable(Constant.myInfoStr, mMySelf);
							bd.putSerializable(Constant.audioCmdStr, 0x02);
							mSendAudioIntent.putExtras(bd);
							MainActivity.this.startActivityForResult(mSendAudioIntent, Constant.SendAudio_RequestCode);
							
							// 后台开始建立语音连接,开始接收语音
							mMainService.agreeRecvAudio(chatPerson);
						}
					});
					alert.setNegativeButton("取消",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// 拒绝接收语音通话
							mMainService.rejectAudioConnect(chatPerson);
						}
					});
					
					AlertDialog dialog = alert.create();
					dialog.show();
					
				}
			}
			//--------------------------------------
			else if(mainAction.equals(Constant.BR_VideoRequest)){
				// 收到请求语音聊天的命令
				if(!isActivityOnTop("ChatActivity")){
					AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					final Person chatPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					alert.setMessage(chatPerson.getName()+"请求进行视频通话,是否接受?");
					alert.setPositiveButton("接收", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// 通过后台服务发送消息
							Intent mSendVideoIntent = new Intent(MainActivity.this, SendVideo.class);
							Bundle bd = new Bundle();
							bd.putSerializable(Constant.userNameStr, chatPerson);
							bd.putSerializable(Constant.myInfoStr, mMySelf);
							bd.putSerializable(Constant.audioCmdStr, 0x02);
							mSendVideoIntent.putExtras(bd);
							MainActivity.this.startActivityForResult(mSendVideoIntent, Constant.SendVideo_RequestCode);
							
							// 后台开始建立视频连接,开始接收视频
							mMainService.agreeVideoConnect(chatPerson);
						}
					});
					alert.setNegativeButton("取消",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// 拒绝接收视频通话
							mMainService.rejectVideoConnect(chatPerson);
						}
					});
					
					AlertDialog dialog = alert.create();
					dialog.show();
					
				}
			}
			
			//--------------------------------------
		}
	};
	
	private IntentFilter mBroadcastFilter = new IntentFilter();
	
	private void RegBroadcastReceiver(){
		
		mBroadcastFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		
		mBroadcastFilter.addAction(Constant.BR_NewUser_Update);
		mBroadcastFilter.addAction(Constant.BR_NewMessage);
		
		mBroadcastFilter.addAction(Constant.BR_RecvNewFile);
		mBroadcastFilter.addAction(Constant.BR_RecvFileSize);
		mBroadcastFilter.addAction(Constant.BR_ReceveFileDone);
		
		mBroadcastFilter.addAction(Constant.BR_AudioRequest);
		mBroadcastFilter.addAction(Constant.BR_VideoRequest);
		this.registerReceiver(mBroadcastReceiver, mBroadcastFilter);
	}
	
	
	// ---------主线程相应消息处理的handler---------
	private final Handler mainHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if(msg.what == Constant.MSG_removeOne){
				if(Constant.DEBUG) Log.i(TAG,"one Person is offline!");
				mExListAdapter.notifyDataSetChanged();
			}
			
		}
	};
	
	private boolean isActivityOnTop(String ActivityName){
		ActivityManager mActivityManager = (ActivityManager)(MainActivity.this.getSystemService(Context.ACTIVITY_SERVICE));
		List<RunningTaskInfo> runningTaskInfos = mActivityManager.getRunningTasks(1);
		if(runningTaskInfos != null){
			String cmpNameTemp = (runningTaskInfos.get(0).topActivity).toString();
			//if(Constant.DEBUG) Log.e(Constant.TAG, "The top Activity is:"+cmpNameTemp);
			if(cmpNameTemp.contains(ActivityName)){
				return true;
			}
		}
		return false;
	}

	
	//-----------------以下是setting界面的处理函数-----------------------------
	public void onSetFileSavePath(View v){
		AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(MainActivity.this);
		
		LayoutInflater mInfalter = LayoutInflater.from(MainActivity.this);
		View pathView = mInfalter.inflate(R.layout.filepath, null);
		TextView editFilePath = (TextView)pathView.findViewById(R.id.editFilePath);
		editFilePath.setText(FileSavePath);
		
		mAlertBuilder.setTitle("接收的文件保存在：")
					.setView(pathView)
					.setPositiveButton("知道了", null);
					//.setNegativeButton("取消", null);
		
		mAlertBuilder.create().show();
	}
	
	
	public void onSettingAbout(View v){
		AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(MainActivity.this);
		
		mAlertBuilder.setTitle("关于纳信")
					.setIcon(R.drawable.ic_launcher)
					.setMessage("    基于局域网的通信工具，可以文字聊天/收发文件/语音对讲/实时视频。" +
								"其中部分UI模仿微信风格，并采用 @小矛 的歪脖子头像。\n\n" +
								"作者：Ryan_XM \n" +
								"邮箱：RyanXm1122@qq.com \n" +
								"版本： V1.0 \n")
					.setPositiveButton("确定", null);
					
		mAlertBuilder.create().show();
	}
	
	public void onSetShortCut(View v){
		AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(MainActivity.this);
		
		mAlertBuilder.setIcon(R.drawable.ic_launcher)
					.setMessage("是否创建快捷方式?")
					.setPositiveButton("是", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Intent addIntent = new Intent(
									"com.android.launcher.action.INSTALL_SHORTCUT");
							String title = getResources().getString(R.string.app_name);
							Parcelable icon = Intent.ShortcutIconResource.fromContext(MainActivity.this, R.drawable.ic_launcher);
							
							Intent myIntent = new Intent(MainActivity.this, WelcomeActivity.class);
							
							addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
							addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
							addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, myIntent);
							
							sendBroadcast(addIntent);
						}
					})
					.setNegativeButton("否", null);
		mAlertBuilder.create().show();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
