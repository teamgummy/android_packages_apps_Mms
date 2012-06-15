package com.android.mms.ui;

import com.android.mms.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

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
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (v == mSendSmsButton) {
            if (keysAreShowing) {
                imm.hideSoftInputFromWindow(mEditBox.getWindowToken(), 0);
                keysAreShowing = false;
            }
            sendSms();
            finish();
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
    }

    private void setRead() {
        ContentValues values = new ContentValues();
        values.put("READ", 1);
        values.put("SEEN", 1);
        getContentResolver().update(Uri.parse("content://sms/"),
                values, "_id="+messageId, null);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
