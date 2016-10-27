package org.ryan.naxin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MyExListAdapter extends BaseExpandableListAdapter {
	private LayoutInflater mInflater = null;
	
	private String[] mGroupStr = {"当前在线","朋友","同事","家人"};
	
	private Context mContext = null;
	
	public MyExListAdapter(Context context) {
		super();
		// TODO Auto-generated constructor stub
		this.mInflater = LayoutInflater.from(context);
		this.mContext = context;
	}

	public Object getChild(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return childPosition;
	}

	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return childPosition;
	}

	public Object getGroup(int groupPosition) {
		// TODO Auto-generated method stub
		return groupPosition;
	}

	public long getGroupId(int groupPosition) {
		// TODO Auto-generated method stub
		return groupPosition;
	}
	
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return true;
	}
	
	//------------------------------------------------
	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
		// 返回每组的MAP的大小
		int childnum = 0;
		if(groupPosition<((MainActivity)mContext).children.size()){
			childnum = ((MainActivity)mContext).children.get(groupPosition).size();
		}
		
		return childnum;
	}

	public int getGroupCount() {
		// TODO Auto-generated method stub
		return mGroupStr.length;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if(convertView == null){
			convertView = mInflater.inflate(R.layout.child_list, null);
		}
		
       if(groupPosition<((MainActivity)mContext).children.size()){
	   		Person mChild = ((MainActivity)mContext).children.get(groupPosition)
					.get(((MainActivity)mContext).personKeys.get(childPosition));
			int userImgId = mChild.getImgID();
			String userName = mChild.getName();
			((TextView)convertView.findViewById(R.id.child_textView)).setText(userName);
			((ImageView)convertView.findViewById(R.id.child_imageView)).
									setImageResource(Constant.mThumbIds[userImgId]);
        }
		

		return convertView;
	}

	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if(convertView == null){
			convertView = mInflater.inflate(R.layout.group_list, null);
		}
		
		int childnum = 0;
		// 一开始只有children中只有1组数据
        if(groupPosition<((MainActivity)mContext).children.size()){
        	childnum = ((MainActivity)mContext).children.get(groupPosition).size();
        }
		
		((TextView)convertView.findViewById(R.id.group_textView)).
									setText(mGroupStr[groupPosition]+"("+childnum+")");
		
		return convertView;
	}


}
