package com.industry.printer.ui.Test;

import android.widget.FrameLayout;
import android.widget.TextView;

public interface ITestOperation {
    // 显示测试页面
    public void show(FrameLayout f);

    // 显示题目
    public void setTitle(TextView tv);

    // 结束测试
    // true: 实际退出；false: 未退出
    public boolean quit();
}
