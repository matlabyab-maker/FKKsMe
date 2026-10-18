package com.fkksme.keyboard;

import android.inputmethodservice.InputMethodService;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.view.inputmethod.InputConnection;
import android.widget.*;
import java.util.*;

public class FKKsMeInputMethodService extends InputMethodService {
    LinearLayout root, keyArea;
    ArrayList<String> history = new ArrayList<>();
    boolean caps=false;
    int bg=Color.rgb(250,249,242), ink=Color.rgb(42,65,76);
    @Override public View onCreateInputView() { return buildKeyboard(); }

    GradientDrawable shape(int color) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(6)); d.setStroke(dp(1),Color.rgb(211,210,202)); return d;
    }
    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    LinearLayout row(){ LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    TextView button(String label, int color, Runnable action) {
        TextView t=new TextView(this); t.setText(label); t.setTextColor(ink); t.setTextSize(15); t.setGravity(Gravity.CENTER); t.setBackground(shape(color)); t.setPadding(dp(2),dp(5),dp(2),dp(5));
        t.setOnClickListener(v->{ t.setBackground(shape(Color.rgb(255,242,176))); t.postDelayed(()->t.setBackground(shape(color)),450); if(action!=null) action.run(); });
        return t;
    }
    void addKey(LinearLayout r,String label,float weight,int color) {
        TextView t=button(label,color,()->type(label));
        t.setTextSize(label.length()>2?13:25);
        t.setTypeface(Typeface.DEFAULT);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),weight); lp.setMargins(dp(1),dp(1),dp(1),dp(1)); r.addView(t,lp);
        if ("ایکوو".contains(label) && label.length()==1) t.setOnLongClickListener(v->{showVariants(label); return true;});
    }
    void type(String s) {
        InputConnection ic=getCurrentInputConnection(); if(ic==null)return;
        switch(s){
            case "⌫": ic.deleteSurroundingText(1,0); return;
            case "Enter": ic.commitText("\n",1); return;
            case "Space": ic.commitText(" ",1); return;
            case "Paste": ic.performContextMenuAction(android.R.id.paste); return;
            case "Copy": ic.performContextMenuAction(android.R.id.copy); return;
            case "Select": ic.performContextMenuAction(android.R.id.selectAll); return;
            case "Undo": ic.performContextMenuAction(0x01020000); return;
            case "Redo": ic.performContextMenuAction(0x01020001); return;
            case "←": ic.commitText("←",1); return;
            case "→": ic.commitText("→",1); return;
            case "↑": ic.sendKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_UP)); return;
            case "↓": ic.sendKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_DOWN)); return;
            case "File": showFileInfo(); return;
            case "Tool": showCalculator(); return;
            case "Muse": showMuse(); return;
            case "Resize": Toast.makeText(this,"برای تغییر اندازه از این دکمه دوباره استفاده کنید",Toast.LENGTH_SHORT).show(); root.getLayoutParams().height=dp(270); root.requestLayout(); return;
            case "History": new AlertDialog.Builder(this).setTitle("تاریخچه").setItems(history.toArray(new String[0]),(d,w)->{InputConnection c=getCurrentInputConnection();if(c!=null)c.commitText(history.get(w),1);}).setPositiveButton("بستن",null).show(); return;
            case "Copy Screen": Toast.makeText(this,"برای کپی، متن انتخاب‌شده را کپی کنید",Toast.LENGTH_SHORT).show(); return;
        }
        ic.commitText(s,1); history.add(s); if(history.size()>50)history.remove(0);
    }
    void showVariants(String key){
        String[] variants;
        switch(key){case "و": variants=new String[]{"و","ؤ","ٶ","ۄ","ۆ","ۇ","ۈ","ۋ"};break;
        case "ا": variants=new String[]{"ا","أ","إ","آ","ٱ","ٲ","ٳ"};break;
        case "ی": variants=new String[]{"ی","ي","ى","ئ","ې","ے"};break;
        case "ک": variants=new String[]{"ک","ك","ګ","گ"};break;
        default: variants=new String[]{key};}
        new AlertDialog.Builder(this).setTitle("زیرگونه‌های «"+key+"»").setItems(variants,(d,w)->type(variants[w])).show();
    }
    void showCalculator(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); EditText expr=new EditText(this); expr.setSingleLine(true); expr.setHint("مثلاً 12+3*4"); box.addView(expr);
        TextView result=new TextView(this); result.setTextSize(22); box.addView(result);
        Button calc=new Button(this); calc.setText("محاسبه"); calc.setOnClickListener(v->{try{double value=new ArithmeticParser(expr.getText().toString().replace('×','*').replace('÷','/').replace('−','-')).parse(); result.setText("= "+(value==(long)value?Long.toString((long)value):Double.toString(value)));}catch(Exception e){result.setText("عبارت نامعتبر است");}}); box.addView(calc);
        new AlertDialog.Builder(this).setTitle("ماشین حساب").setView(box).setPositiveButton("درج نتیجه", (d,w)->{if(getCurrentInputConnection()!=null)getCurrentInputConnection().commitText(result.getText(),1);}).setNegativeButton("بستن",null).show();
    }
    void showFileInfo(){ new AlertDialog.Builder(this).setTitle("File").setMessage("برای درج فایل در برنامهٔ مقصد، از گزینهٔ پیوست/بارگذاری همان برنامه استفاده کنید.").setPositiveButton("باشه",null).show(); }
    void showMuse(){
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setBackgroundColor(Color.rgb(245,246,250));
        TextView pad=new TextView(this); pad.setText("میدان لمسی\\nحرکت نشانگر\\n\\nTouchpad for Cursor Movement"); pad.setTextSize(19); pad.setGravity(Gravity.CENTER); pad.setBackground(shape(Color.rgb(235,238,246))); panel.addView(pad,new LinearLayout.LayoutParams(-1,dp(135)));
        LinearLayout actions=row();
        TextView left=button("کلیک چپ",Color.rgb(159,215,233),()->{InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText("",1);});
        TextView drag=button("Drag",Color.rgb(250,238,186),()->{});
        TextView zoom=button("Point Zoom",Color.rgb(234,176,226),()->Toast.makeText(this,"بزرگ‌نمایی نشانگر",Toast.LENGTH_SHORT).show());
        TextView close=button("Close",Color.rgb(244,137,95),()->{});
        for(TextView t:new TextView[]{left,drag,zoom,close}){actions.addView(t,new LinearLayout.LayoutParams(0,dp(52),1));}
        panel.addView(actions);
        pad.setOnTouchListener(new View.OnTouchListener(){float x,y; public boolean onTouch(View v,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();return true;} if(e.getAction()==MotionEvent.ACTION_MOVE){pad.setText("میدان لمسی\\nحرکت: "+(int)(e.getX()-x)+", "+(int)(e.getY()-y)+"\\nTouchpad for Cursor Movement");return true;} if(e.getAction()==MotionEvent.ACTION_UP){return true;} return true;}});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Muse — Touchpad").setView(panel).setNegativeButton("بستن",null).create();
        close.setOnClickListener(v->dialog.dismiss()); drag.setOnClickListener(v->Toast.makeText(this,"حالت Drag",Toast.LENGTH_SHORT).show()); dialog.show();
    }
    @Override public void onStartInputView(android.view.inputmethod.EditorInfo info, boolean restarting){super.onStartInputView(info,restarting);}

    static class ArithmeticParser {
        final String s; int p=0;
        ArithmeticParser(String input){s=input.replace(" ","");}
        double parse(){double v=expr(); if(p<s.length())throw new IllegalArgumentException(); return v;}
        double expr(){double v=term(); while(p<s.length()){char c=s.charAt(p);if(c!='+'&&c!='-')break;p++;double n=term();v=c=='+'?v+n:v-n;}return v;}
        double term(){double v=factor();while(p<s.length()){char c=s.charAt(p);if(c!='*'&&c!='/')break;p++;double n=factor();if(c=='/'&&n==0)throw new ArithmeticException();v=c=='*'?v*n:v/n;}return v;}
        double factor(){if(p<s.length()&&(s.charAt(p)=='+'||s.charAt(p)=='-')){char c=s.charAt(p++);double v=factor();return c=='-'?-v:v;} if(p<s.length()&&s.charAt(p)=='('){p++;double v=expr();if(p>=s.length()||s.charAt(p++)!=')')throw new IllegalArgumentException();return v;}int start=p;while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.'))p++;if(start==p)throw new IllegalArgumentException();return Double.parseDouble(s.substring(start,p));}
    }
    View buildKeyboard(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg); root.setPadding(dp(1),dp(1),dp(1),dp(1));
        LinearLayout top=row();
        String[] tools={"Paste","Copy","Copy\nScreen","Select","Undo","Redo","8\nHistory","Tool","Muse","Resize"};
        for(String s:tools){TextView t=button(s,Color.rgb(250,249,242),()->type(s.equals("8\nHistory")?"History":s.replace("\n"," "))); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(43),1);lp.setMargins(dp(1),0,dp(1),0);top.addView(t,lp);}
        root.addView(top);
        LinearLayout nav=row();
        for(int i=0;i<5;i++){TextView blank=button("",bg,null);nav.addView(blank,new LinearLayout.LayoutParams(0,dp(34),1));}
        TextView up=button("↑",bg,()->type("↑")); TextView down=button("↓",bg,()->type("↓")); nav.addView(up,new LinearLayout.LayoutParams(0,dp(34),0.5f));nav.addView(down,new LinearLayout.LayoutParams(0,dp(34),0.5f));
        TextView file=button("File",bg,()->type("File"));nav.addView(file,new LinearLayout.LayoutParams(0,dp(34),1));TextView send=button("Send",bg,()->{InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.performEditorAction(getCurrentInputEditorInfo().imeOptions & (android.view.inputmethod.EditorInfo.IME_MASK_ACTION));});nav.addView(send,new LinearLayout.LayoutParams(0,dp(34),1));root.addView(nav);
        LinearLayout nums=row(); String[] ns={"! ۱","@ ۲","# ۳","$ ۴","% ۵","^ ۶","& ۷","* ۸","( ۹",") ۰","⌫"}; for(String n:ns)addKey(nums,n,1,n.equals("⌫")?Color.rgb(250,220,225):bg);root.addView(nums);
        String[][] rows={{"ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"},{"ش","س","ی","ب","ل","ا","ت","ن","م","ک","گ"},{"Caps","ظ","ط","ژ","ز","ر","ذ","د","پ","و","چ","=","-"}};
        for(int ri=0;ri<rows.length;ri++){LinearLayout r=row();for(String k:rows[ri])addKey(r,k,1,bg);root.addView(r);}
        LinearLayout bottom=row();
        String[] b={"😀","&@#\n*¥✓","اعراب","🌐 FA","Space","،",".","؟","←","→","↑","↓","[","]"};
        float[] weights={0.8f,0.8f,0.8f,1,2.2f,0.7f,0.7f,0.7f,1.1f,1.1f,0.55f,0.55f,0.5f,0.5f};
        for(int i=0;i<b.length;i++){final String label=b[i];TextView t=button(label,bg,()->{if(label.equals("😀"))type("😀");else if(label.equals("Space"))type("Space");else if(label.equals("🌐 FA"))((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();else type(label);});t.setTextSize(label.length()>2?13:21);bottom.addView(t,new LinearLayout.LayoutParams(0,dp(44),weights[i]));}
        root.addView(bottom);
        return root;
    }
}
