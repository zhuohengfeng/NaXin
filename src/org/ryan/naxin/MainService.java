package org.ryan.naxin;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MainService extends Service {
	
	public MyBinder mBinder = new MyBinder();

	private WifiManager mWifiManager = null;
	
	// 定义本地IP地址
	private byte[] mLocalIpByte = null;
	//private InetAddress mLocalInetAddress = null;
	public String mLocalIpStr = null;
	
	
	private CommunicThread mCommunicThread = null;
	// 发送注册的buffer
	private byte[] regBuffer = new byte[Constant.bufferSize]; 
	
	//发送消息的buffer
	private byte[] msgSendBuffer = new byte[Constant.bufferSize];
	//发送文件命令的buffer
	private byte[] fileSendBuffer = new byte[Constant.bufferSize];
	//发送语音命令的buffer
	private byte[] audioCmdBuffer = new byte[Constant.bufferSize];
	//发送视频命令的buffer
	private byte[] videoCmdBuffer = new byte[Constant.bufferSize];
	
	private String mSendfilePath = null;
	private long mSendFileSize = 0;
	private long mRecvFileSize = 0;
	//------------------------------------------------------------
	// 自定义的Binder类，可以用来传递服务类本身实力
	public class MyBinder extends Binder{
		public MainService getServiceInstance(){
			return MainService.this;
		}
		
	}
	
	
	public MainService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		if(Constant.DEBUG) Log.i(Constant.TAG, "Now service onBind ====>");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		return super.onUnbind(intent);
	}
	
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(Constant.DEBUG) Log.i(Constant.TAG, "Now service onDestroy ====>");
		IsStopUpdateMe = true;
	}

	
	
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		if(Constant.DEBUG) Log.i(Constant.TAG, "Now service onStart ====>");
		initCmdBuffer();
		
		// 获取用户信息
		setMyInformation(intent);
		
		
		// 获取到WIFI控制服务
		mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		// 启动线程检测网络连接和本地IP地址
		new CheckNetConnectivity().start();
		
		// 启动通信线程
		// 1.接收消息   2.发送消息
		mCommunicThread = new CommunicThread();
		mCommunicThread.start();
		
		// 通过调用mCommunicThread的接口来发送心跳包
		new UpdateMeThread().start();
		
	}
	
	private void initCmdBuffer(){
		for(int i=0;i<Constant.bufferSize;i++)regBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, regBuffer, 0, 3);
		regBuffer[3] = Constant.CMD80;
		regBuffer[4] = Constant.CMD_TYPE1;
		regBuffer[5] = Constant.OPR_CMD1;
		
		for(int i=0;i<Constant.bufferSize;i++)msgSendBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, msgSendBuffer, 0, 3);
		msgSendBuffer[3] = Constant.CMD81;
		msgSendBuffer[4] = Constant.CMD_TYPE1;
		msgSendBuffer[5] = Constant.OPR_CMD1;
		
		for(int i=0;i<Constant.bufferSize;i++)fileSendBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, fileSendBuffer, 0, 3);
		fileSendBuffer[3] = Constant.CMD82;
		fileSendBuffer[4] = Constant.CMD_TYPE1;
		fileSendBuffer[5] = Constant.OPR_CMD1;
		
		for(int i=0;i<Constant.bufferSize;i++)audioCmdBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, audioCmdBuffer, 0, 3);
		audioCmdBuffer[3] = Constant.CMD83;
		audioCmdBuffer[4] = Constant.CMD_TYPE1;
		audioCmdBuffer[5] = Constant.OPR_CMD1;
		
		for(int i=0;i<Constant.bufferSize;i++)videoCmdBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, videoCmdBuffer, 0, 3);
		videoCmdBuffer[3] = Constant.CMD84;
		videoCmdBuffer[4] = Constant.CMD_TYPE1;
		videoCmdBuffer[5] = Constant.OPR_CMD1;
	}

	
	
	// ---------从主UI线程中设置用户信息------------
	private String userName = "";
	private int userImgId = 0;
	private int userID = 0;
	private boolean IsGetUserInfomation = false;
    public void setMyInformation(Intent intent){
    	this.userName = intent.getExtras().getString(Constant.userNameStr);
    	this.userImgId = intent.getExtras().getInt(Constant.userImgStr);
    	this.userID = intent.getExtras().getInt(Constant.userIDStr);
    	
    	System.arraycopy(ByteAndInt.int2ByteArray(this.userID), 0, regBuffer, 6, 4);
    	System.arraycopy(ByteAndInt.int2ByteArray(this.userImgId), 0, regBuffer, 10, 4);
    	for(int i=14;i<44;i++)
    		regBuffer[i] = 0;
    	byte[] nickeNameBytes = this.userName.getBytes();
    	System.arraycopy(nickeNameBytes, 0, regBuffer, 14, nickeNameBytes.length);
    	
    	this.IsGetUserInfomation = true;
    	if(Constant.DEBUG) Log.i(Constant.TAG, "Service GetInformation : userName="+this.userName+"  ImgId="+this.userImgId +"  userID" +this.userID);
    }
	

	
	// ================Thread 1. 启动线程检测网络连接和本地IP地址===================
    private boolean isGetTheHostIP = false;
	private class CheckNetConnectivity extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				// 如果没有启动WIFI，则打开WIFI
				if (!mWifiManager.isWifiEnabled()) {
					mWifiManager.setWifiEnabled(true);
				}
				// 如果打开网络了，就获取本地IP地址
				for (Enumeration<NetworkInterface> en = NetworkInterface
						.getNetworkInterfaces(); en.hasMoreElements();) {
					NetworkInterface Inet = en.nextElement();
					for (Enumeration<InetAddress> netAdd_en = Inet.getInetAddresses(); netAdd_en.hasMoreElements();) {
						InetAddress netAdd = netAdd_en.nextElement();
						if (!netAdd.isLoopbackAddress()){
							
								/*if(netAdd.isReachable(1000))*/
														
							//mLocalInetAddress = netAdd;
							mLocalIpStr = netAdd.getHostAddress().toString();
							mLocalIpByte = netAdd.getAddress();
							// 把本机地址复制到 注册buffer的第44位开始的数据段
							System.arraycopy(mLocalIpByte,0,regBuffer,44,4);
							// 成功获取IP
							isGetTheHostIP = true;
							
							if(Constant.DEBUG) Log.d(Constant.TAG, "CheckNetConnectivity LocalIp==>" + mLocalIpStr);
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		
	}


	
	// ================Thread 2. 通信线程===================
	private class CommunicThread extends Thread{
		private MulticastSocket multicastSocket = null;
		private byte[] recvBuffer = new byte[Constant.bufferSize]; // 接收的buffer

		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				// 定义一个每个客户端共用的多点传送组
				multicastSocket = new MulticastSocket(Constant.PORT);
				multicastSocket.joinGroup(InetAddress.getByName(Constant.MULTICAST_IP));
				if(Constant.DEBUG) if(Constant.DEBUG) Log.i(Constant.TAG, "Now start the multicast socket....");
				
				// 定义socket从这个多点广播组接收消息
				while(!multicastSocket.isClosed() && multicastSocket != null){
					// 先清空 接收 buffer
					for(int i=0;i<recvBuffer.length;i++){
						recvBuffer[i] = 0;
					}
					DatagramPacket mDatagramPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
					multicastSocket.receive(mDatagramPacket);
					// 获取到组播的消息，开始分析
					parsePackage(recvBuffer);
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				try {
					if(null!=multicastSocket && !multicastSocket.isClosed()){
						multicastSocket.leaveGroup(InetAddress.getByName(Constant.MULTICAST_IP));
						multicastSocket.close();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
			
		}
		
		
		// 发送自己的信息到多点传送组
		private void joinOrganization(){
			try {
				if (null != multicastSocket && !multicastSocket.isClosed()) {
					
					// 注意发送/接收的 DatagramPacket 定义不一样！！！
					DatagramPacket mDatagramPacket = new DatagramPacket(
							regBuffer, regBuffer.length, 
							InetAddress.getByName(Constant.MULTICAST_IP),
							Constant.PORT);
					multicastSocket.send(mDatagramPacket);
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		
		// 发送消息
		public void sendMsg(int personId, String personHost, String msg){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(personId), 0, msgSendBuffer, 6, 4);
				int msgLength = Constant.msgLength+10;
				for(int i=10;i<msgLength;i++){msgSendBuffer[i]=0;}
				
				//把消息复制到MSG buffer的10开始的位置
				byte[] msgBytes = msg.getBytes();
				System.arraycopy(msgBytes, 0, msgSendBuffer, 10, msgBytes.length);
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "SendMsg:"+personId+"@"+personHost+" :"+msg);
				InetAddress host = InetAddress.getByName(personHost);
				DatagramPacket msgDp = new DatagramPacket(msgSendBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// 发送文件命令
		public void sendFile(int personId, String personHost, String filePath){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				fileSendBuffer[4] = Constant.CMD_TYPE1;//发送
				fileSendBuffer[5] = Constant.OPR_CMD1;
				System.arraycopy(ByteAndInt.int2ByteArray(personId), 0, fileSendBuffer, 6, 4);
				int fileNameLength = Constant.fileNameLength+10;
				for(int i=10;i<fileNameLength;i++){fileSendBuffer[i] = 0;}
				
				//把消息复制到MSG buffer的10开始的位置
				byte[] filePathBytes = filePath.getBytes();
				System.arraycopy(filePathBytes, 0, fileSendBuffer, 10, filePathBytes.length);
				
				// 把要发送的文件大小复制到从300开始的地方
				byte[] fileSize = ByteAndInt.longToByteArray(mSendFileSize);
				System.arraycopy(fileSize, 0, fileSendBuffer, 300, fileSize.length);
				
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "SendFile:"+personId+"@"+personHost+" :"+filePath+" size:"+mSendFileSize);
				InetAddress host = InetAddress.getByName(personHost);
				DatagramPacket msgDp = new DatagramPacket(fileSendBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// 同意开始接收文件
		public void startRecvFile(final Person recvPerson, final String fileName){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				fileSendBuffer[4] = Constant.CMD_TYPE2;//接收
				fileSendBuffer[5] = Constant.OPR_CMD1; // 同意接收
				System.arraycopy(ByteAndInt.int2ByteArray(recvPerson.getUID()), 0, fileSendBuffer, 6, 4);
				int fileNameLength = Constant.fileNameLength+10;
				for(int i=10;i<fileNameLength;i++){fileSendBuffer[i] = 0;}
				
				//把消息复制到MSG buffer的10开始的位置
				byte[] fileNameBytes = fileName.getBytes();
				System.arraycopy(fileNameBytes, 0, fileSendBuffer, 10, fileNameBytes.length);
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "Agree :"+recvPerson.getUID()+"@"+recvPerson.getPersonHost()+" :"+fileName);
				InetAddress host = InetAddress.getByName(recvPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(fileSendBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// 拒绝接收文件
		public void rejectRecvFile(final Person recvPerson, final String fileName){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				fileSendBuffer[4] = Constant.CMD_TYPE2;//接收
				fileSendBuffer[5] = Constant.OPR_CMD2; // 拒绝接收
				System.arraycopy(ByteAndInt.int2ByteArray(recvPerson.getUID()), 0, fileSendBuffer, 6, 4);
				int fileNameLength = Constant.fileNameLength+10;
				for(int i=10;i<fileNameLength;i++){fileSendBuffer[i] = 0;}
				
				//把消息复制到MSG buffer的10开始的位置
				byte[] fileNameBytes = fileName.getBytes();
				System.arraycopy(fileNameBytes, 0, fileSendBuffer, 10, fileNameBytes.length);
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "Agree :"+recvPerson.getUID()+"@"+recvPerson.getPersonHost()+" :"+fileName);
				InetAddress host = InetAddress.getByName(recvPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(fileSendBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		///////////////////////////////////////////////////////////////////
		// 发送语音请求命令
		public void sendAudioRequest(int personId, String personHost){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(personId), 0, audioCmdBuffer, 6, 4);
				int audioCmdLength = Constant.msgLength+10;
				for(int i=10;i<audioCmdLength;i++){audioCmdBuffer[i]=0;}
				
				audioCmdBuffer[3] = Constant.CMD83;
				audioCmdBuffer[4] = Constant.CMD_TYPE1;
				audioCmdBuffer[5] = Constant.OPR_CMD1;
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "SendAudioCmd:"+personId+"@"+personHost);
				InetAddress host = InetAddress.getByName(personHost);
				DatagramPacket msgDp = new DatagramPacket(audioCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// 拒绝接收语音命令
		public void rejectAudioConnect(final Person chatPerson){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(chatPerson.getUID()), 0, audioCmdBuffer, 6, 4);
				int audioCmdLength = Constant.msgLength+10;
				for(int i=10;i<audioCmdLength;i++){audioCmdBuffer[i]=0;}
				
				audioCmdBuffer[3] = Constant.CMD83;
				audioCmdBuffer[4] = Constant.CMD_TYPE1;
				audioCmdBuffer[5] = Constant.OPR_CMD2;
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "Reject the Audio CMD:"+chatPerson.getUID()+"@"+chatPerson.getPersonHost());
				InetAddress host = InetAddress.getByName(chatPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(audioCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// 同意接收语音命令
		public void agreeAudioConnect(final Person chatPerson){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(chatPerson.getUID()), 0, audioCmdBuffer, 6, 4);
				int audioCmdLength = Constant.msgLength+10;
				for(int i=10;i<audioCmdLength;i++){audioCmdBuffer[i]=0;}
				
				audioCmdBuffer[3] = Constant.CMD83;
				audioCmdBuffer[4] = Constant.CMD_TYPE1;
				audioCmdBuffer[5] = Constant.OPR_CMD3;
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "Reject the Audio CMD:"+chatPerson.getUID()+"@"+chatPerson.getPersonHost());
				InetAddress host = InetAddress.getByName(chatPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(audioCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// 断开语音命令
		public void disconnectAudio(final Person chatPerson){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(chatPerson.getUID()), 0, audioCmdBuffer, 6, 4);
				int AudioCmdLength = Constant.msgLength+10;
				for(int i=10;i<AudioCmdLength;i++){audioCmdBuffer[i]=0;}
				
				audioCmdBuffer[3] = Constant.CMD83;
				audioCmdBuffer[4] = Constant.CMD_TYPE1;
				audioCmdBuffer[5] = Constant.OPR_CMD4;
				
				// 发送数据
				InetAddress host = InetAddress.getByName(chatPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(audioCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		
		
		///////////////////////////////////////////////////////////////////
		// 发送视频请求命令
		public void sendVideoRequest(int personId, String personHost){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(personId), 0, videoCmdBuffer, 6, 4);
				int videoCmdLength = Constant.msgLength+10;
				for(int i=10;i<videoCmdLength;i++){videoCmdBuffer[i]=0;}
				
				videoCmdBuffer[3] = Constant.CMD84;
				videoCmdBuffer[4] = Constant.CMD_TYPE1;
				videoCmdBuffer[5] = Constant.OPR_CMD1;
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "SendAudioCmd:"+personId+"@"+personHost);
				InetAddress host = InetAddress.getByName(personHost);
				DatagramPacket msgDp = new DatagramPacket(videoCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// 拒绝接收视频命令
		public void rejectVideoConnect(final Person chatPerson){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(chatPerson.getUID()), 0, videoCmdBuffer, 6, 4);
				int videoCmdLength = Constant.msgLength+10;
				for(int i=10;i<videoCmdLength;i++){videoCmdBuffer[i]=0;}
				
				videoCmdBuffer[3] = Constant.CMD84;
				videoCmdBuffer[4] = Constant.CMD_TYPE1;
				videoCmdBuffer[5] = Constant.OPR_CMD2;
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "Reject the Audio CMD:"+chatPerson.getUID()+"@"+chatPerson.getPersonHost());
				InetAddress host = InetAddress.getByName(chatPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(videoCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// 同意接收视频命令
		public void agreeVideoConnect(final Person chatPerson){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(chatPerson.getUID()), 0, videoCmdBuffer, 6, 4);
				int videoCmdLength = Constant.msgLength+10;
				for(int i=10;i<videoCmdLength;i++){videoCmdBuffer[i]=0;}
				
				videoCmdBuffer[3] = Constant.CMD84;
				videoCmdBuffer[4] = Constant.CMD_TYPE1;
				videoCmdBuffer[5] = Constant.OPR_CMD3;
				
				// 发送数据
				//if(Constant.DEBUG) Log.e(Constant.TAG, "Reject the Audio CMD:"+chatPerson.getUID()+"@"+chatPerson.getPersonHost());
				InetAddress host = InetAddress.getByName(chatPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(videoCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// 断开视频命令
		public void disconnectVideo(final Person chatPerson){
			try {
				// 把ID转换成数组拷贝的buffer 6~9byte
				System.arraycopy(ByteAndInt.int2ByteArray(chatPerson.getUID()), 0, videoCmdBuffer, 6, 4);
				int videoCmdLength = Constant.msgLength+10;
				for(int i=10;i<videoCmdLength;i++){videoCmdBuffer[i]=0;}
				
				videoCmdBuffer[3] = Constant.CMD84;
				videoCmdBuffer[4] = Constant.CMD_TYPE1;
				videoCmdBuffer[5] = Constant.OPR_CMD4;
				
				// 发送数据
				InetAddress host = InetAddress.getByName(chatPerson.getPersonHost());
				DatagramPacket msgDp = new DatagramPacket(videoCmdBuffer, Constant.bufferSize, 
						host, Constant.PORT);
				
				multicastSocket.send(msgDp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/////////////////////////////////////////////////////////////
		//               解析收到的多点传送数据
		////////////////////////////////////////////////////////////
		private void parsePackage(byte[] recvBuff){
			int CMD = recvBuff[3];//命令
			int cmdType = recvBuff[4];//命令类型
			int oprCmd = recvBuff[5];//操作码

			int recvUserID = 0;
			byte[] ArrUserID = new byte[4];
			System.arraycopy(recvBuff, 6, ArrUserID, 0 , 4);
			recvUserID = ByteAndInt.byteArray2Int(ArrUserID);

			switch(CMD){
				// 收到注册信息
				case Constant.CMD80:
				{
					
					// 先判断是否是自己发送的心跳包
					if(recvUserID != MainService.this.userID){
						int recvUserImgID = 0;
						String recvUserName = null;
						String recvUserHost = null;
						
						byte[] ArrUserImgID = new byte[4];
						System.arraycopy(recvBuff, 10, ArrUserImgID, 0 , 4);
						recvUserImgID = ByteAndInt.byteArray2Int(ArrUserImgID);
						
						byte[] ArrUserName = new byte[30];
						System.arraycopy(recvBuff, 14, ArrUserName, 0 , 30);
						recvUserName = (new String(ArrUserName)).trim();
						
						byte[] ArrUserIPbyte = new byte[4];
						System.arraycopy(recvBuff, 44, ArrUserIPbyte, 0, 4);
						try {
							InetAddress targetIp = InetAddress.getByAddress(ArrUserIPbyte);
							recvUserHost = targetIp.getHostAddress().toString();
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						
						// 发送新用户广播，更新UI
						Intent mNewPesonBroadcast = new Intent(Constant.BR_NewUser_Update);
						mNewPesonBroadcast.putExtra(Constant.userIDStr, recvUserID);
						mNewPesonBroadcast.putExtra(Constant.userImgStr, recvUserImgID);
						mNewPesonBroadcast.putExtra(Constant.userNameStr, recvUserName);
						mNewPesonBroadcast.putExtra(Constant.userHostStr, recvUserHost);
						sendBroadcast(mNewPesonBroadcast);
						
						//if(Constant.DEBUG) if(Constant.DEBUG) Log.i(Constant.TAG, "Service GetInfo: userName="+recvUserName+"@"+recvUserHost+" ImgId="+recvUserImgID +" userID=" +recvUserID);
					}
					
				}
				break;
			
				// 收到Text消息
				case Constant.CMD81:
				{
					// 获取消息
					byte[] ArrMessageBytes = new byte[Constant.msgLength+10];
					System.arraycopy(recvBuff, 10, ArrMessageBytes, 0, Constant.msgLength);
					String message = (new String(ArrMessageBytes)).trim();
		
					//if(Constant.DEBUG) if(Constant.DEBUG) Log.e(Constant.TAG,"recve the "+recvUserID+" said:"+message);
					
					// 发送新消息广播，更新UI
					Intent mNewMsgBroadcast = new Intent(Constant.BR_NewMessage);
					mNewMsgBroadcast.putExtra(Constant.userIDStr, recvUserID);
					mNewMsgBroadcast.putExtra(Constant.userNewMsgStr, message);
					sendBroadcast(mNewMsgBroadcast);
				}
				break;
				
				// 收到文件命令消息
				case Constant.CMD82:
				{
					//Type1表示是收到一个要给自己发送文件命令
					if(cmdType == Constant.CMD_TYPE1){
						// 获取消息
						byte[] ArrFileNameBytes = new byte[Constant.msgLength+10];
						System.arraycopy(recvBuff, 10, ArrFileNameBytes, 0, Constant.msgLength);
						String fileName = (new String(ArrFileNameBytes)).trim();
			
						// 从300开始的地方读取要接收的文件大小
						byte[] fileSize = new byte[30];
						System.arraycopy(recvBuff, 300, fileSize, 0, fileSize.length);
						mRecvFileSize = ByteAndInt.byteArrayToLong(fileSize);
						
						//if(Constant.DEBUG) Log.e(Constant.TAG,"recve the "+recvUserID+" filename:"+fileName+" size:"+mRecvFileSize);
						
						// 发送新消息广播，更新UI
						Intent mRecvNewFilBroadcast = new Intent(Constant.BR_RecvNewFile);
						mRecvNewFilBroadcast.putExtra(Constant.userIDStr, recvUserID);
						mRecvNewFilBroadcast.putExtra(Constant.RecvFileName, fileName);
						sendBroadcast(mRecvNewFilBroadcast);
						
					}
					// Type2表示给对方发送后，对方返回是否接收
					else if(cmdType == Constant.CMD_TYPE2){
						boolean isRecvFile = false;
						if(oprCmd == Constant.OPR_CMD1){
							//if(Constant.DEBUG) Log.e(Constant.TAG, "对方同意接收文件了");
							isRecvFile = true;
						}
						else if(oprCmd == Constant.OPR_CMD2){
							//if(Constant.DEBUG) Log.e(Constant.TAG, "对方拒绝接收文件了");
							isRecvFile = false;
						}
						// 发送新消息广播，更新UI
						Intent mRecvNewFilBroadcast = new Intent(Constant.BR_HandleNewFileResult);
						mRecvNewFilBroadcast.putExtra(Constant.userIDStr, recvUserID);
						mRecvNewFilBroadcast.putExtra(Constant.HandleFileName, isRecvFile);
						sendBroadcast(mRecvNewFilBroadcast);
					}
					//-----------
				}
				break;
				
				
				// 语音命令消息
				case Constant.CMD83:
				{
					//Type1表示是收到一个要给自己发送/拒接语音命令
					if(cmdType == Constant.CMD_TYPE1){
						if(oprCmd == Constant.OPR_CMD1){
							//if(Constant.DEBUG) Log.e(Constant.TAG,"RECV the "+recvUserID+" filename:"+fileName+" size:"+mRecvFileSize);
							// 发送新消息广播，更新UI
							Intent mAudioRequestBroadcast = new Intent(Constant.BR_AudioRequest);
							mAudioRequestBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mAudioRequestBroadcast);
						}
						else if(oprCmd == Constant.OPR_CMD2){
							// 发送新消息广播，更新UI
							Intent mAudiorejectBroadcast = new Intent(Constant.BR_RejectAudioConnect);
							mAudiorejectBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mAudiorejectBroadcast);
						}
						else if(oprCmd == Constant.OPR_CMD3){
							// 发送新消息广播，更新UI
							Intent mAudiorejectBroadcast = new Intent(Constant.BR_AgreetAudioConnect);
							mAudiorejectBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mAudiorejectBroadcast);
						}
						else if(oprCmd == Constant.OPR_CMD4){
							// 断开语音连接---- 发送新消息广播，更新UI
							Intent mAudiorejectBroadcast = new Intent(Constant.BR_DisconnectAudio);
							mAudiorejectBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mAudiorejectBroadcast);
						}
						
					}
					
					
				}
				break;
				
				// 视频命令消息
				case Constant.CMD84:
				{
					//Type1表示是收到一个要给自己发送/拒接视频命令
					if(cmdType == Constant.CMD_TYPE1){
						if(oprCmd == Constant.OPR_CMD1){
							// 发送新消息广播，更新UI
							Intent mVideoRequestBroadcast = new Intent(Constant.BR_VideoRequest);
							mVideoRequestBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mVideoRequestBroadcast);
						}
						else if(oprCmd == Constant.OPR_CMD2){
							// 发送新消息广播，更新UI
							Intent mVideoRejectBroadcast = new Intent(Constant.BR_RejectVideoConnect);
							mVideoRejectBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mVideoRejectBroadcast);
						}
						else if(oprCmd == Constant.OPR_CMD3){
							// 发送新消息广播，更新UI
							Intent mVideoAgreeBroadcast = new Intent(Constant.BR_AgreeVideoConnect);
							mVideoAgreeBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mVideoAgreeBroadcast);
						}
						else if(oprCmd == Constant.OPR_CMD4){
							// 断开Video连接
							Intent mVideoDisconnetBroadcast = new Intent(Constant.BR_DisconnectVideo);
							mVideoDisconnetBroadcast.putExtra(Constant.userIDStr, recvUserID);
							sendBroadcast(mVideoDisconnetBroadcast);
						}
					}
				}
				break;
				
				
				default:
					break;
			}
			
		}
		
	}
	

	
	// ================Thread 2. 发送心跳包线程===================
	private boolean IsStopUpdateMe = false;
	private class UpdateMeThread extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while(!IsStopUpdateMe){
				try {
					
					if(IsGetUserInfomation && isGetTheHostIP){
						//if(Constant.DEBUG) Log.e(Constant.TAG, "发送本身信息包------>");
						mCommunicThread.joinOrganization();
					}

					sleep(5000); // 10s
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}
	}
	
	// ==================================
	// Server暴露的发送消息接口
	public void sendMsg(final int personId,final String personHost, final String msg){
		
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.sendMsg(personId, personHost ,msg);
			}
		}.start();
	}
	
	// ==================================
	// Server暴露的发送消息接口发送文件
	public void sendFile(final int personId, final String personHost, final String filePath){
		mSendfilePath = filePath;
		mSendFileSize = (new File(filePath)).length();//要发送的文件大小
		String[] fileNames = filePath.split("/");
		final String fileName = fileNames[fileNames.length - 1];
		
		//if(Constant.DEBUG) Log.e(Constant.TAG, "SendFile the File name is:"+fileName+" size is:"+mSendFileSize);
				
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.sendFile(personId, personHost ,fileName);
			}
		}.start();
	}
	

	// ==========发送/接收文件采用TCP协议========================
	private class SendFileThread extends Thread{
		private Person chatPerson = null;
		
		public SendFileThread(Person mPerson) {
			super();
			// TODO Auto-generated constructor stub
			this.chatPerson = mPerson;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			
			byte[] buff = new byte[Constant.bufferSize];
			// 创建客户端连接服务器的client
			InetAddress sendAddress = null;
			Socket sendSocket = null;
			OutputStream socketStream = null;
			FileInputStream mFileInputStream = null;
			try {
				File inFile = new File(mSendfilePath);
				mFileInputStream = new FileInputStream(inFile);
				
				// 读取文件
				sendAddress = InetAddress.getByName(chatPerson.getPersonHost());
				sendSocket = new Socket(sendAddress, Constant.PORT);
				socketStream = sendSocket.getOutputStream();
				
				int hasRead = 0;
				long hasSendSize = 0;
				while((hasRead = mFileInputStream.read(buff))>0){
					//如果读取的字节流大于0，则循环读取
					hasSendSize += hasRead; 
					//if(Constant.DEBUG) Log.e(Constant.TAG, "Read the buff bytes:"+hasSendSize+" totalsize:"+mSendFileSize);
					socketStream.write(buff, 0, hasRead);
					socketStream.flush();
					
					// 发送新消息广播，更新UI
					Intent mSendFileSizeBroadcast = new Intent(Constant.BR_SendFileSize);
					mSendFileSizeBroadcast.putExtra(Constant.hasSendSize, hasSendSize);
					mSendFileSizeBroadcast.putExtra(Constant.totalSizes, mSendFileSize);
					sendBroadcast(mSendFileSizeBroadcast);
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				// 无论结果如何都要关闭socket
	
				try {
					if(null!=socketStream)socketStream.close();
					if(null!=mFileInputStream)mFileInputStream.close();
					if(!sendSocket.isClosed())sendSocket.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
			}
		}
		
	}
	
	// 接收文件的线程
	private class RecvFileThread extends Thread{
		private String fileName = null;
		private Person recvPerson = null;
		
		public RecvFileThread(Person recvPerson, String fileName) {
			super();
			// TODO Auto-generated constructor stub
			this.fileName = fileName;
			this.recvPerson = recvPerson;
		}

		@Override
		public void run() {
				// TODO Auto-generated method stub
				super.run();
				byte[] recvBuff = new byte[Constant.bufferSize];
				ServerSocket recvSocketServer = null;
				Socket recvSocket = null;
				InputStream mSocketInputStream = null;
				FileOutputStream fileOutputStream = null;
				
				if(Constant.DEBUG) Log.e(Constant.TAG,"File RecvFileThread ...");
				int hasReadSize = 0;
				
				try {
					File recvFile = new File("/mnt/sdcard/"+this.fileName);
					fileOutputStream = new FileOutputStream(recvFile);
					recvSocketServer = new ServerSocket(Constant.PORT);
					recvSocket = recvSocketServer.accept();
					recvSocket.setSoTimeout(5000);
					//开始接收文件
					mSocketInputStream = recvSocket.getInputStream();
					
					long hasRecvSize = 0;
					while((hasReadSize = mSocketInputStream.read(recvBuff))>0){
						fileOutputStream.write(recvBuff, 0, hasReadSize);
						
						hasRecvSize += hasReadSize;
						// 发送新消息广播，更新UI
						Intent mRecvFileSizeBroadcast = new Intent(Constant.BR_RecvFileSize);
						mRecvFileSizeBroadcast.putExtra(Constant.hasRecvSize, hasRecvSize);
						mRecvFileSizeBroadcast.putExtra(Constant.totalSizes, mRecvFileSize);
						sendBroadcast(mRecvFileSizeBroadcast);
					}
				
					// 接收完毕，通知主UI
					// 发送新消息广播，更新UI
					Intent mRecvNewFilBroadcast = new Intent(Constant.BR_ReceveFileDone);
					mRecvNewFilBroadcast.putExtra(Constant.userIDStr, recvPerson.getUID());
					mRecvNewFilBroadcast.putExtra(Constant.HandleFileName, this.fileName);
					sendBroadcast(mRecvNewFilBroadcast);
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					// 无论结果如何都要关闭socket
					try {
						if(null!=fileOutputStream)fileOutputStream.close();
						if(null!=mSocketInputStream)mSocketInputStream.close();
						if(!recvSocket.isClosed())recvSocket.close();
						if(!recvSocketServer.isClosed())recvSocketServer.close();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
		}
		
	}
	
	
	// 开始发送文件
	public void startSendFile(final Person chatPerson){
		// 启动一个线程开始发送文件
		new SendFileThread(chatPerson).start();
	}
	
	// 选择开始接收文件
	public void startRecvFile(final Person recvPerson, final String fileName){
		
		// 启动一个线程开始发送文件
		new RecvFileThread(recvPerson, fileName).start();
		
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.startRecvFile(recvPerson,fileName);
			}
		}.start();
	}
	
	// 拒绝接收文件
	public void rejectRecvFile(final Person recvPerson, final String fileName){
		
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.rejectRecvFile(recvPerson,fileName);
			}
		}.start();
	}
	
	
	
	//////////////////////////////////////////////////////////////////////////
	//
	//                    以下是接收/发送语音命令
	//
	//////////////////////////////////////////////////////////////////////////
	//private Person chatPerson = null;
	
	// 1. 发送方：发送请求语音通话命令
	public void sendAudioRequest(final int personId,final String personHost){
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.sendAudioRequest(personId, personHost);
			}
		}.start();
	}
	
	// 2.  接收方：拒绝接收语音请求
	public void rejectAudioConnect(final Person chatPerson){
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.rejectAudioConnect(chatPerson);
			}
		}.start();
	}
	
	
	//3.  接收方：同意接收语音
	public void agreeRecvAudio(final Person chatPerson){
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.agreeAudioConnect(chatPerson);
			}
		}.start();
	}
	

	//4.  断开语音连接
	public void disconnectAudio(final Person chatPerson){

		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.disconnectAudio(chatPerson);
			}
		}.start();
	}
	
	
	//////////////////////////////////////////////////////////////////////////
	//
	//                    以下是接收/发送视频命令
	//
	//////////////////////////////////////////////////////////////////////////
	
	// 1. 发送请求视频通话命令
	public void sendVideoRequest(final int personId,final String personHost){
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.sendVideoRequest(personId, personHost);
			}
		}.start();
	}
	
	// 2.  拒绝接收视频请求
	public void rejectVideoConnect(final Person chatPerson){
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.rejectVideoConnect(chatPerson);
			}
		}.start();
	}
	
	
	// 3.  同意接收视频请求
	public void agreeVideoConnect(final Person chatPerson){
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.agreeVideoConnect(chatPerson);
			}
		}.start();
	}
	
	
	//4.  断开视频连接
	public void disconnectVideo(final Person chatPerson){

		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicThread.disconnectVideo(chatPerson);
			}
		}.start();
	}
	
	
	
	
	
	
	
	
}
