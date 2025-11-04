package com.industry.printer.ui.CustomerDialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.industry.printer.MessageTask;
import com.industry.printer.R;
import com.industry.printer.ui.CustomerAdapter.ObjectListAdapter;

import java.util.List;

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class MsgListOfGroupDialog extends RelightableDialog implements AdapterView.OnItemClickListener {
//public class ObjectListDialog extends Dialog implements AdapterView.OnItemClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

    private ListView mListview;
    private ObjectListAdapter mAdapter;
    private int selection;

    public MsgListOfGroupDialog(Context context, MessageTask task) {
        super(context);
        this.selection = selection;
    }

    public int getSelection() {
        return selection;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.msglist_of_group);

        mListview = (ListView) findViewById(R.id.list);

        mListview.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 0;
            }

            @Override
            public Object getItem(int i) {
                return null;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                return null;
            }
        });
        mListview.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selection = position;
        mAdapter.setSelect(position);
        dismiss();
    }
}
