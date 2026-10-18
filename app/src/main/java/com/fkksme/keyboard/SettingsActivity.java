package com.fkksme.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24,24,24,24);
  TextView title=new TextView(this); title.setText("FKKsMe — صفحه راه‌اندازی کیبورد"); title.setTextSize(22); root.addView(title);
  TextView info=new TextView(this); info.setText("۱) کیبورد را فعال کنید.  ۲) آن را به‌عنوان کیبورد جاری انتخاب کنید.\n\nبرای استفاده از زیرگونه‌ها، کلیدهای ا، ی، ک و و را نگه دارید."); info.setTextSize(17); root.addView(info);
  Button enable=new Button(this); enable.setText("فعال‌سازی کیبورد"); enable.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))); root.addView(enable);
  Button choose=new Button(this); choose.setText("انتخاب کیبورد"); choose.setOnClickListener(v->{ ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker(); }); root.addView(choose);
  Button accessibility=new Button(this); accessibility.setText("فعال‌سازی File و Send برای سایت‌ها"); accessibility.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); root.addView(accessibility);
  TextView note=new TextView(this); note.setText("پس از بازکردن تنظیمات دسترس‌پذیری، سرویس «FKKsMe — کنترل دکمه‌های سایت» را فعال کنید. این سرویس برای شناسایی دکمه‌های صفحه به اجازهٔ شما نیاز دارد و ممکن است با بعضی سایت‌ها سازگار نباشد."); note.setTextSize(15); root.addView(note);
  setContentView(root);
 }
}
