package com.industry.printer.ui.CustomerDialog;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.industry.printer.MessageTask;
import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.data.DataTask;
import com.industry.printer.object.BaseObject;
import com.industry.printer.ui.CustomerAdapter.ObjectListAdapter;

import java.util.ArrayList;
import java.util.List;

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class MsgListOfGroupDialog extends RelightableDialog {
//public class ObjectListDialog extends Dialog implements AdapterView.OnItemClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
    public static final String TAG = MsgListOfGroupDialog.class.getSimpleName();

    private ListView mListview;
    private BaseAdapter mAdapter;

    private int mSelection;
    private List<MessageTask> mGroupTasks;

    public interface IMsgListDialogListener {
        public void onConfirmed(String path);
        public void onCanceled();
    }
    private IMsgListDialogListener mListener;

    private class Holder {
        public HorizontalScrollView mScrollView;
        public LinearLayout mPreviewImage;
        public RelativeLayout mArea;
        public ImageView mChoiceMark;
        public Bitmap mPrevBmp;
    }

    public MsgListOfGroupDialog(Context context, String groupName) {
        super(context);
        mSelection = 0;
        mListener = null;
        mGroupTasks = new ArrayList<MessageTask>();
        List<String> paths = MessageTask.parseGroup(groupName);
        for (String path : paths) {
            MessageTask task = new MessageTask(mContext, path);
            mGroupTasks.add(task);
        }
    }

    public void setListener(IMsgListDialogListener l) {
        mListener = l;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.msglist_of_group);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(lp);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

        mListview = (ListView) findViewById(R.id.list);

        mAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mGroupTasks.size();
            }

            @Override
            public Object getItem(int i) {
                return (i < mGroupTasks.size() ? mGroupTasks.get(i) : null);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                Holder holder = null;
                if (convertView != null) {
                    holder = (Holder) convertView.getTag();
                    Bitmap bmp = holder.mPrevBmp;
                    if(null != bmp && !bmp.isRecycled()) bmp.recycle();
                } else {
                    convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.msglist_item_of_group, null);
                    holder = new Holder();
                    holder.mScrollView = (HorizontalScrollView) convertView.findViewById(R.id.preview_scroll);
                    holder.mPreviewImage = (LinearLayout) convertView.findViewById(R.id.ll_preview);
                    holder.mArea = (RelativeLayout) convertView.findViewById(R.id.select_area);
                    holder.mChoiceMark = (ImageView) convertView.findViewById(R.id.select_image);
                    convertView.setTag(holder);
                }
                if(mSelection == position) {
                    holder.mArea.setBackgroundColor(Color.parseColor("#44000000"));
                    holder.mChoiceMark.setVisibility(View.VISIBLE);
                } else {
                    holder.mArea.setBackgroundColor(Color.TRANSPARENT);
                    holder.mChoiceMark.setVisibility(View.GONE);
                }

                MessageTask msgTask = mGroupTasks.get(position);
                holder.mPrevBmp = BitmapFactory.decodeFile(msgTask.getPreview());

                int x=0,y=0;
                int cutWidth = 0;
                float scale = 1;
                float height = 0.8f * holder.mPrevBmp.getHeight();
                scale = (height/holder.mPrevBmp.getHeight());
                holder.mPreviewImage.removeAllViews();
                holder.mPreviewImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mSelection = position;
                        mAdapter.notifyDataSetChanged();
                    }
                });

                for (int i = 0;x < holder.mPrevBmp.getWidth(); i++) {
                    if (x + 1200 > holder.mPrevBmp.getWidth()) {
                        cutWidth = holder.mPrevBmp.getWidth() - x;
                    } else {
                        cutWidth =1200;
                    }
                    Bitmap child = Bitmap.createBitmap(holder.mPrevBmp, x, 0, cutWidth, holder.mPrevBmp.getHeight());
                    if (cutWidth * scale < 1 || holder.mPrevBmp.getHeight() * scale < 1) {
                        child.recycle();
                        break;
                    }
//                    Debug.d(TAG, "-->child: [" + child.getWidth() + ", " + child.getHeight() + "]; view h: " + holder.mPreviewImage.getHeight() + "]; orientation: " + mContext.getResources().getConfiguration().orientation);
                    Bitmap scaledChild = Bitmap.createScaledBitmap(child, (int) (cutWidth*scale), (int) (holder.mPrevBmp.getHeight() * scale), true);
                    //child.recycle();
                    //Debug.d(TAG, "--->scaledChild  width = " + child.getWidth() + " scale= " + scale);
                    x += cutWidth;
                    ImageView imgView = new ImageView(mContext);
                    imgView.setScaleType(ImageView.ScaleType.FIT_XY);
                    imgView.setLayoutParams(new ActionBar.LayoutParams(scaledChild.getWidth(),scaledChild.getHeight()));

                    imgView.setBackgroundColor(Color.WHITE);
                    imgView.setImageBitmap(scaledChild);
                    holder.mPreviewImage.addView(imgView);
                }
                holder.mArea.setMinimumHeight((int)height);
                holder.mArea.setMinimumWidth(parent.getWidth());
                holder.mPreviewImage.setMinimumWidth(parent.getWidth());
                return convertView;
            }
        };
        mListview.setAdapter(mAdapter);

        TextView confirmBtn = (TextView) findViewById(R.id.btn_confirm);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Debug.d(TAG, mGroupTasks.get(mSelection).getName());
                if(null != mListener) mListener.onConfirmed(mGroupTasks.get(mSelection).getName());
                dismiss();
            }
        });
        TextView cancelBtn = (TextView) findViewById(R.id.btn_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != mListener) mListener.onCanceled();
                dismiss();
            }
        });
    }
}
