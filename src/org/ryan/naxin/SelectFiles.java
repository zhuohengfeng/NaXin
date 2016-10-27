package org.ryan.naxin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SelectFiles extends Activity {
	
	// 定义控件成员
	private TextView mTextView;
	private ListView mListView;
	
	// 记录当前的父文件夹
	File mCurrentParent;
	// 记录当前路径下的所有文件的文件数组
	File[] mCurrentFiles;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectfiles);
		
		mTextView = (TextView)findViewById(R.id.sdcard_path);
		mListView = (ListView)findViewById(R.id.listView_files);
		
		mListView.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				if(Constant.DEBUG) Log.i(Constant.TAG, "onItemClick position =" + position );
				//如果选择的是一个文件
				if(mCurrentFiles[position].isFile() == true){

					try {
						// 发送文件
						Intent FilePathIntent = new Intent();
						String SelectFilePath;
						SelectFilePath = mCurrentFiles[position].getCanonicalPath();
						FilePathIntent.putExtra(Constant.SelectFilesStr, SelectFilePath);
						setResult(Constant.SelectFiles_ResultCode_OK, FilePathIntent);
						finish();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					return;
				}
				//如果是一个目录
				else{
					// 获取选择的目录下所有文件
					File[] tempFiles = mCurrentFiles[position].listFiles();
					if((tempFiles == null)||(tempFiles.length == 0)){
						//如果没有文件
						Toast.makeText(SelectFiles.this, "There are's the files!", Toast.LENGTH_SHORT).show();
					}
					else{
						//如果有文件，则列出
						mCurrentParent = mCurrentFiles[position];
						mCurrentFiles = tempFiles;
						inflateListView(mCurrentFiles);
					}
				}
			}
		});
		
		
		// 绑定按钮的监听事件
		Button mBunton = (Button)findViewById(R.id.button_parent);
		mBunton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				try {
					// TODO Auto-generated method stub
					if (!mCurrentParent.getCanonicalPath().equals("/mnt/sdcard")) {
						// 获取上一级目录
						mCurrentParent = mCurrentParent.getParentFile();
						mCurrentFiles = mCurrentParent.listFiles();
						inflateListView(mCurrentFiles);
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		});
		
		// 获取系统的SD卡目录
		File root = new File("/mnt/sdcard");
		// 判断SD卡是否存在
		if(root.exists() == true){
			//记录当前路径和文件数组
			mCurrentParent = root;
			mCurrentFiles = root.listFiles();
			//使用当前目录下的全部文件，文件夹填充LISTVIEW
			inflateListView(mCurrentFiles);
		}
		else{
			mTextView.setText(R.string.no_found_SDcard);
		}
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	private void inflateListView(File[] files){
		// 创建一个List集合，List集合的元素是Map
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < files.length; i++)
		{
			Map<String, Object> listItem = new HashMap<String, Object>();
			//如果当前File是文件夹，使用folder图标；否则使用file图标
			if (files[i].isDirectory())
			{
				listItem.put("icon", R.drawable.folder);
			}
			else
			{
				listItem.put("icon", R.drawable.file);
			}
			listItem.put("fileName", files[i].getName());
			//添加List项
			listItems.add(listItem);
		}
		
		// 最终还是要创建一个Adapter
		// 创建一个SimpleAdapter
		SimpleAdapter simpleAdapter = new SimpleAdapter(this, listItems,
			R.layout.list_files, new String[] { "icon", "fileName" }, new int[] {
				R.id.icon, R.id.file_name });
		
		
		// 为ListView绑定Adapter
		mListView.setAdapter(simpleAdapter);
		// 设置当前路径（相对路径）
		try {
			mTextView.setText(mCurrentParent.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	


}
