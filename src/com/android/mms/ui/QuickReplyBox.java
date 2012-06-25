package com.android.mms.ui;

import com.android.mms.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class QuickReplyBox extends Activity implements OnDismissListener, OnClickListener {

    private boolean keysAreShowing;
    private TextView mNameLabel;
    private String mPhoneNumber;
    private String mContactName;
    private long messageId;
    private ImageButton mSendSmsButton;
    private EditText mEditBox;
    private Context mContext;

    private KeyguardManager.KeyguardLock kl;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        KeyguardManager km = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        kl = km.newKeyguardLock("IN");
        kl.disableKeyguard();

        LayoutInflater inflater = LayoutInflater.from(this);
        final View mView = inflater.inflate(R.layout.quick_reply_box, null);
        AlertDialog alert = new AlertDialog.Builder(this).setView(mView).create();

        Bundle extras = getIntent().getExtras();
        mPhoneNumber = extras.getString("numbers");
        mContactName = extras.getString("name");
        messageId = extras.getLong("id");
        mNameLabel = (TextView) mView.findViewById(R.id.name_label);
        mNameLabel.setText(mContactName);
        mSendSmsButton = (ImageButton) mView.findViewById(R.id.send_sms_button);
        mSendSmsButton.setOnClickListener(this);
        mEditBox = (EditText) mView.findViewById(R.id.edit_box);
        mEditBox.setOnClickListener(this);
        alert.setOnDismissListener(this);
        alert.show();
    }

    @Override
    public void onDestroy() {
        kl.reenableKeyguard();
        finish();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
    	Handler h = new Handler();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (v == mSendSmsButton) {
            if (keysAreShowing) {
                imm.hideSoftInputFromWindow(mEditBox.getWindowToken(), 0);
                keysAreShowing = false;
            }
            sendSms();
            // adding in a toastbox to show message is sending
            // also made a runtime delay so it wouldnt quickly go back to the lockscreen, as the lockscreen covers toasts
            Toast.makeText(this, R.string.quick_reply_sending, Toast.LENGTH_SHORT).show();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                	finish();
                }
            },250);
        } else if (v == mEditBox) {
            imm.showSoftInput(mEditBox, 0);
            keysAreShowing = true;
            mEditBox.requestFocus();
        }
    }

    private void sendSms() {
        String mMessage = null;
        mMessage = mEditBox.getText().toString();
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(mPhoneNumber, null, mMessage, null, null);
        } catch (IllegalArgumentException e) {
        }
        setRead();
        addMessageToSent(mMessage);
    }

    private void setRead() {
        ContentValues values = new ContentValues();
        values.put("READ", 1);
        values.put("SEEN", 1);
        getContentResolver().update(Uri.parse("content://sms/"),
                values, "_id="+messageId, null);
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // 123 is the code for SMS notification
        nm.cancel(123);
    }

    private void addMessageToSent(String messageSent) {
        ContentValues sentSms = new ContentValues();
        sentSms.put("address", mPhoneNumber);
        sentSms.put("body", messageSent);
        getContentResolver().insert(Uri.parse("content://sms/sent"), sentSms);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
    	//re-did the dismiss to allow you to stay, close, or mark as read.
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle("Nothing Has Been Sent");
    	alert.setMessage("Did you want to cancel this message?");
    	
    	alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	finish();
            }
        });
        
    	alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	LayoutInflater inflater = LayoutInflater.from(QuickReplyBox.this);
                final View mView = inflater.inflate(R.layout.quick_reply_box, null);
                AlertDialog alert = new AlertDialog.Builder(QuickReplyBox.this).setView(mView).create();
                
                mNameLabel = (TextView) mView.findViewById(R.id.name_label);
                mNameLabel.setText(mContactName);
                mSendSmsButton = (ImageButton) mView.findViewById(R.id.send_sms_button);
                mSendSmsButton.setOnClickListener(QuickReplyBox.this);
                mEditBox = (EditText) mView.findViewById(R.id.edit_box);
                mEditBox.setOnClickListener(QuickReplyBox.this);
                alert.setOnDismissListener(QuickReplyBox.this);
                alert.show();
            }
        });
    	
    	alert.setNeutralButton("Mark Read", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			setRead();
    			finish();
            }
    	});
    	
    	alert.show();
    }
}
