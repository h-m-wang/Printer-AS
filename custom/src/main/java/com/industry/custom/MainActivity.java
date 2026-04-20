package com.industry.custom;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Socket mClientSocket;
    private TextView mResultTV;
    private TextView mConTV;
    private EditText mCommandET;
    private TextView mSendTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mConTV = (TextView)findViewById(R.id.Connect);
        mConTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Socket socket = new Socket();
                            socket.setSoTimeout(2000);
                            socket.connect(new InetSocketAddress("127.0.0.1", 3660),3000);
                            mClientSocket = socket;
                            BufferedReader br = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream(), Charset.forName("UTF-8")));
                            String line = br.readLine();
                            final String l = line;
                            mResultTV.post(new Runnable() {
                                @Override
                                public void run() {
                                    mConTV.setBackgroundColor(Color.GREEN);
                                    mSendTV.setBackgroundColor(Color.BLUE);
                                    mSendTV.setEnabled(true);
                                    mResultTV.append("<- " + l + "\r\n");
                                }
                            });
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });
        mConTV.setBackgroundColor(null == mClientSocket ? Color.BLUE : Color.GREEN);

        mCommandET = (EditText) findViewById(R.id.CommandET);

        mSendTV = (TextView)findViewById(R.id.SendData);
        mSendTV.setBackgroundColor(null == mClientSocket ? Color.GRAY : Color.BLUE);
        mSendTV.setEnabled(null != mClientSocket);
        mSendTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(null != mClientSocket && mClientSocket.isConnected()) {
                            try {
                                mResultTV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mResultTV.append("-> " + mCommandET.getText().toString() + "\r\n");
                                    }
                                });
                                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream(), Charset.forName("UTF-8")));
                                bw.write(mCommandET.getText().toString() + "\r\n");
                                bw.flush();
                                BufferedReader br = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream(), Charset.forName("UTF-8")));
                                String line = br.readLine();
                                final String l = line;
                                mResultTV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mResultTV.append("<- " + l + "\r\n");
                                    }
                                });
                            } catch(SocketTimeoutException e) {
                                mResultTV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        SpannableString message = new SpannableString("Timeout!");
                                        message.setSpan(new ForegroundColorSpan(Color.RED), 0, "Timeout!".length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                        mResultTV.append(message);
                                        mResultTV.append("\r\n");
                                    }
                                });
                            } catch (Exception e) {
                                mResultTV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        SpannableString message = new SpannableString("Connection broken!");
                                        message.setSpan(new ForegroundColorSpan(Color.RED), 0, "Connection broken!".length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                        mResultTV.append(message);
                                        mResultTV.append("\r\n");
                                    }
                                });
                                e.printStackTrace();
                            }
                        } else {
                            mResultTV.post(new Runnable() {
                                @Override
                                public void run() {
                                    SpannableString message = new SpannableString("Not connected!\r\n");
                                    message.setSpan(new ForegroundColorSpan(Color.RED), 0, "Not connected!\r\n".length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                    mResultTV.append(message);
                                    mResultTV.append("\r\n");
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        TextView backToPrinter = (TextView)findViewById(R.id.Jump2Printer);
        backToPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
                mHomeIntent.addCategory(Intent.CATEGORY_HOME);
                mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(mHomeIntent);
            }
        });

        mResultTV = (TextView)findViewById(R.id.Content);

        Spinner spinner = findViewById(R.id.Select);

        List<String> commands = new ArrayList<>();
        commands.add("000B|0000|1000|0|19456|0000|0|0000|0D0A");
        commands.add("000B|0000|SendBin|0|19456|0000|0|0000|0D0A");
        commands.add("000B|0000|1100|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|DelBin|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|1200|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Reset|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|600|1234,5678,,,,,,,,,www.print-test.com ,12|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Dynamic|1234,5678,,,,,,,,,www.print-test.com ,12|0|0000|0|0000|0D0A");
        commands.add("000B|0000|650|1234,5678,,,,,,,,,www.print-test.com ,12|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Dynamic1|1234,5678,,,,,,,,,www.print-test.com ,12|0|0000|0|0000|0D0A");
        commands.add("000B|0000|100|/mnt/sdcard/MSG/1/|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Print|/mnt/sdcard/MSG/1/|0|0000|0|0000|0D0A");
        commands.add("000B|0000|200|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Purge|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|300|/mnt/sdcard/MSG/4/1.TLK|255|0000|end|0000|0D0A");
        commands.add("000B|0000|SendFile|/mnt/sdcard/MSG/4/1.TLK|255|0000|end|0000|0D0A");
        commands.add("000B|0000|400|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Inquery|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|500|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Stop|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|700|/mnt/sdcard/MSG/4/1.TLK/|255|0000|0|0000|0D0A");
        commands.add("000B|0000|MakeTLK|/mnt/sdcard/MSG/4/1.TLK/|255|0000|0|0000|0D0A");
        commands.add("000B|0000|800|/mnt/sdcard/MSG/4/1.BMP/|0|0000|0|0000|0D0A");
        commands.add("000B|0000|DelFile|/mnt/sdcard/MSG/4/1.BMP/|0|0000|0|0000|0D0A");
        commands.add("000B|0000|900|/mnt/sdcard/MSG/4/|0|0000|0|0000|0D0A");
        commands.add("000B|0000|DelDir|/mnt/sdcard/MSG/4/|0|0000|0|0000|0D0A");
        commands.add("000B|0000|dotsize|3000|0|0000|0|0000|0D0A");
        commands.add("000B|0000|clean|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Counter|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Time|200716200101|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Settings|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|Heartbeat|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|SoftPho|0|0|0000|0|0000|0D0A");
        commands.add("000B|0000|ClearFIFO|0|0|0000|0|0000|0D0A");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,  // 默认项布局
                commands
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCmd = parent.getItemAtPosition(position).toString();
                mCommandET.setText(selectedCmd);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 当没有选择任何项时调用
            }
        });
    }
}
