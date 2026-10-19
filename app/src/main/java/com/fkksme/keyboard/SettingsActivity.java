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
  TextView info=new TextView(this); info.setText("۱) کیبورد را فعال کنید.  ۲) آن را به‌عنوان کیبورد جاری انتخاب کنید.\n\nبرای استفاده از زیرگونه‌ها، کلیدهای ا، ی، ک و و را نگه دارید.\n\nFKKsMe خودش غلط‌گیر یا پیشنهاد واژه ندارد؛ خط قرمز ممکن است از غلط‌گیر اندروید باشد."); info.setTextSize(17); root.addView(info);
  Button enable=new Button(this); enable.setText("فعال‌سازی کیبورد"); enable.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))); root.addView(enable);
  Button choose=new Button(this); choose.setText("انتخاب کیبورد"); choose.setOnClickListener(v->{ ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker(); }); root.addView(choose);
  Button siteButtons=new Button(this); siteButtons.setText("فعال‌سازی ارسال و فایل سایت‌ها (اختیاری)"); siteButtons.setOnClickListener(v->{ try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch(Exception ignored) { } }); root.addView(siteButtons);
  TextView siteNote=new TextView(this); siteNote.setText("برای کلیک روی دکمهٔ واقعی ارسال/پیوست داخل سایت‌ها، این سرویس اختیاری را در تنظیمات دسترس‌پذیری روشن کنید. بدون آن، Send فقط فرمان استاندارد کادر ورودی را امتحان می‌کند و ممکن است در بعضی سایت‌ها ارسال نکند."); siteNote.setTextSize(14); root.addView(siteNote);
  Button spellcheck=new Button(this); spellcheck.setText("تنظیمات غلط‌گیر اندروید"); spellcheck.setOnClickListener(v->{ try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch(Exception e) { startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)); } }); root.addView(spellcheck);
  TextView spellNote=new TextView(this); spellNote.setText("برای حذف خط قرمز زیر واژه‌ها، در تنظیمات گوشی وارد بخش زبان و ورودی شوید و گزینهٔ غلط‌گیر املایی / Spell checker را خاموش کنید. این گزینه متعلق به اندروید یا برنامهٔ میزبان است و کیبورد به‌تنهایی اجازهٔ خاموش‌کردن سراسری آن را ندارد."); spellNote.setTextSize(15); root.addView(spellNote);
  setContentView(root);
 }
}
