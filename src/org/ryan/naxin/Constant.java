package org.ryan.naxin;



public class Constant {
	public static final String TAG = "NaXin";
	public static final boolean DEBUG = false;
	
	
	public Constant() {
		// TODO Auto-generated constructor stub
	}

	// 产生一个随机数作为用户ID
	public static int getMyId(){
		int id = (int)(Math.random()*1000000);
		return id;
	}
	
	
	public static int[] mThumbIds={//显示的图片数组
			  R.drawable.head1,R.drawable.head2,
			  R.drawable.head3,R.drawable.head4,
			  R.drawable.head5,R.drawable.head6,
			  R.drawable.head7,R.drawable.head8,
			  R.drawable.head9,R.drawable.head10,
			  R.drawable.head11,R.drawable.head12,
			  R.drawable.head13,R.drawable.head14,
			  R.drawable.head15,R.drawable.head16,
			  R.drawable.head17,R.drawable.head18,
			  R.drawable.head19,R.drawable.head20,
			  R.drawable.head21,R.drawable.head22,
			  R.drawable.head23,R.drawable.head24,
			  R.drawable.head25
	};
	
	public static final int bufferSize = 1024;	
	public static final int msgLength = 180;
	public static final int fileNameLength = 90;
	public static final int readBufferSize = 4096;
	public static final byte[] pkgHead = "AND".getBytes();
	public static final int CMD80 = 80;
	public static final int CMD81 = 81; // text
	public static final int CMD82 = 82; // file
	public static final int CMD83 = 83; // audio
	public static final int CMD84 = 84; // video
	public static final int CMD_TYPE1 = 1;
	public static final int CMD_TYPE2 = 2;
	public static final int CMD_TYPE3 = 3;
	public static final int OPR_CMD1 = 1;
	public static final int OPR_CMD2 = 2;
	public static final int OPR_CMD3 = 3;
	public static final int OPR_CMD4 = 4;
	public static final int OPR_CMD5 = 5;
	public static final int OPR_CMD6 = 6;
	public static final int OPR_CMD10 = 10;
	
	public static final String myInfoStr = "myPersonInfo";
	public static final String userNameStr = "userName";
	public static final String userImgStr = "userImg";
	public static final String userIDStr = "userID";
	public static final String userHostStr = "userHostStr";
	public static final String userNewMsgStr = "userNewMessage";		
	public static final String SelectFilesStr= "selectFiles";
	public static final String RecvFileName = "RecvTheFiles";
	public static final String HandleFileName = "HandleFileName";
	public static final String hasSendSize = "hasSendSize";
	public static final String hasRecvSize = "hasRecvSize";
	public static final String totalSizes = "totalSizes";
	public static final String audioCmdStr = "audioCmdStr";
	public static final String videoCmdStr = "videoCmdStr";
	
	public static final String MULTICAST_IP = "239.9.9.1";
	public static final int PORT = 5760;
	public static final int AudioPORT = 5761;

	// 定义广播消息
	public static final String BR_NewUser_Update = "org.ryan.service.newuser.update";
	public static final String BR_NewMessage = "org.ryan.service.newmessage.recev";
	public static final String BR_RecvNewFile = "org.ryan.service.newfile.recev";
	public static final String BR_HandleNewFileResult = "org.ryan.service.handle.newfile";
	public static final String BR_ReceveFileDone = "org.ryan.service.receFile.done";
	public static final String BR_SendFileSize = "org.ryan.service.sendfile.size";
	public static final String BR_RecvFileSize = "org.ryan.service.recvfile.size";
	public static final String BR_AudioRequest = "org.ryan.service.audio.request";
	public static final String BR_RejectAudioConnect = "org.ryan.service.audio.reject";
	public static final String BR_AgreetAudioConnect = "org.ryan.service.audio.agree";
	public static final String BR_DisconnectAudio = "org.ryan.service.Audio.disconnect";
	public static final String BR_VideoRequest = "org.ryan.service.video.request";
	public static final String BR_RejectVideoConnect = "org.ryan.service.video.reject";
	public static final String BR_AgreeVideoConnect = "org.ryan.service.video.agree";
	public static final String BR_DisconnectVideo = "org.ryan.service.video.disconnect";
	
	
	// Handler消息
	public static final int MSG_removeOne = 0x1001;	
	
	public static final int SelectFiles_RequestCode = 0x2001;
	public static final int SelectFiles_ResultCode_OK = 0x2002;
	public static final int SendAudio_RequestCode = 0x2003;
	public static final int SendAudio_ResultCode_OK = 0x2004;
	public static final int SendVideo_RequestCode = 0x2005;
	public static final int SendVideo_ResultCode_OK = 0x2006;
}


