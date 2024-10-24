package com.industry.printer.Socket_Server;

import java.util.List;

import com.industry.printer.Socket_Server.LinearLayoutViews.MNotifyDataSetChangedIF;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public abstract class LinearLayoutBaseAdapter {
    private List<? extends Object> list;
    private Context context;
    private MNotifyDataSetChangedIF changedIF;
    public LinearLayoutBaseAdapter(Context context, List<? extends Object> list) {
        this.context = context;
        this.list = list;
    }

    public LayoutInflater getLayoutInflater() {
        if (context != null) {
            return LayoutInflater.from(context);
        }

        return null;
    }

    public int getCount() {
        if (list != null) {
            return list.size();
        }
        return 0;
    };

    public Object getItem(int position) {
        if (list != null) {
            return list.get(position);
        }
        return null;
    };
    
    /**
     * 绑定adapter中的监听
     * @param changedIF
     */
    public void setNotifyDataSetChangedIF(MNotifyDataSetChangedIF changedIF){
    	this.changedIF = changedIF;
    }
    /**
     * 数据刷新
     */
    public void notifyDataSetChanged(){
    	if (changedIF != null) {
    		changedIF.changed();
		}
    }
    /**
     * 供子类复写
     * 
     * @param position
     * @return
     */
    protected abstract View getView(int position);
}