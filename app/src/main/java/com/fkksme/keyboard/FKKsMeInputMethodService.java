package com.fkksme.keyboard;

import android.inputmethodservice.InputMethodService;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.InputConnection;
import android.widget.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;

public class FKKsMeInputMethodService extends InputMethodService {
    static FKKsMeInputMethodService activeService;
    LinearLayout root, keyArea;
    ArrayList<String> clipboardHistory = new ArrayList<>();
    ClipboardManager clipboardManager;
    ClipboardManager.OnPrimaryClipChangedListener clipboardListener;
    static final String CLIP_PREFS = "fkksme_clipboard_history";
    static final String CLIP_KEY = "items";

    @Override public void onCreate() {
        super.onCreate();
        activeService=this;
        loadClipboardHistory();
        clipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboardListener = () -> captureClipboard();
        if (clipboardManager != null) clipboardManager.addPrimaryClipChangedListener(clipboardListener);
    }

    @Override public void onDestroy() {
        if (clipboardManager != null && clipboardListener != null) clipboardManager.removePrimaryClipChangedListener(clipboardListener);
        if(activeService==this) activeService=null;
        super.onDestroy();
    }

    void loadClipboardHistory() {
        String stored = getSharedPreferences(CLIP_PREFS, MODE_PRIVATE).getString("ordered", "[]");
        clipboardHistory.clear();
        try {
            JSONArray array = new JSONArray(stored);
            for (int i = 0; i < array.length(); i++) clipboardHistory.add(array.getString(i));
        } catch (JSONException ignored) { }
    }

    void persistClipboardHistory() {
        JSONArray array = new JSONArray();
        for (String item : clipboardHistory) array.put(item);
        getSharedPreferences(CLIP_PREFS, MODE_PRIVATE).edit().putString("ordered", array.toString()).apply();
    }

    void captureClipboard() {
        if (clipboardManager == null) return;
        try {
            ClipData clip = clipboardManager.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            CharSequence text = clip.getItemAt(0).coerceToText(this);
            if (text == null || text.length() == 0) return;
            String value = text.toString().trim();
            if (value.isEmpty()) return;
            clipboardHistory.remove(value);
            clipboardHistory.add(0, value);
            while (clipboardHistory.size() > 30) clipboardHistory.remove(clipboardHistory.size()-1);
            persistClipboardHistory();
        } catch (Exception ignored) { }
    }

    void showClipboardHistory() {
        captureClipboard();
        loadClipboardHistory();
        if (clipboardHistory.isEmpty()) {
            Toast.makeText(this, "تاریخچهٔ کلیپ‌بورد خالی است؛ ابتدا متنی را کپی کنید.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] items = clipboardHistory.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("تاریخچهٔ کلیپ‌بورد")
            .setItems(items, (dialog, which) -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(items[which], 1);
            }).setNeutralButton("پاک‌کردن تاریخچه", (dialog, which) -> {
                clipboardHistory.clear();
                getSharedPreferences(CLIP_PREFS, MODE_PRIVATE).edit().remove("ordered").apply();
                Toast.makeText(this, "تاریخچه پاک شد", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("بستن", null).show();
    }
    final ArrayDeque<String> undoStack = new ArrayDeque<>();
    final ArrayDeque<String> redoStack = new ArrayDeque<>();
    boolean caps=false;
    boolean english=false;
    // Resize cycle: normal -> 75% -> 50% -> normal.
    int resizeLevel=0;
    float keyboardScale(){ return resizeLevel==1?0.75f:(resizeLevel==2?0.50f:1.0f); }
    final Handler repeatHandler = new Handler(Looper.getMainLooper());
    Runnable repeatDelete;
    int bg=Color.rgb(250,249,242), ink=Color.rgb(42,65,76);
    @Override public View onCreateInputView() { return buildKeyboard(); }

    GradientDrawable shape(int color) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(6)); d.setStroke(dp(1),Color.rgb(211,210,202)); return d;
    }
    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density*keyboardScale()+0.5f); }
    LinearLayout row(){ LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    TextView button(String label, int color, Runnable action) {
        TextView t=new TextView(this); t.setText(label); t.setTextColor(ink); t.setTextSize(15*keyboardScale()); t.setGravity(Gravity.CENTER); t.setBackground(shape(color)); t.setPadding(dp(2),dp(5),dp(2),dp(5));
        t.setOnClickListener(v->{ t.setBackground(shape(Color.rgb(255,242,176))); t.postDelayed(()->t.setBackground(shape(color)),450); if(action!=null) action.run(); });
        return t;
    }
    void addKey(LinearLayout r,String label,float weight,int color) {
        TextView t=button(label,color,()->{
            if(label.equals("Caps")){ caps=!caps; setInputView(buildKeyboard()); return; }
            if(label.equals("اعراب")){ showDiacritics(); return; }
            type(english && caps && label.length()==1 ? label.toUpperCase(Locale.ROOT) : label);
        });
        t.setTextSize((label.length()>2?13:25)*keyboardScale());
        t.setTypeface(Typeface.DEFAULT);
        if(label.equals("Caps") && caps) t.setBackground(shape(Color.rgb(255,242,176)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),weight); lp.setMargins(dp(1),dp(1),dp(1),dp(1)); r.addView(t,lp);
        if ("ایکوو".contains(label) && label.length()==1) t.setOnLongClickListener(v->{showVariants(label); return true;});
        if (label.equals("⌫")) {
            t.setOnLongClickListener(v -> { startDeleteRepeat(); return true; });
            t.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) stopDeleteRepeat();
                return false;
            });
        }
    }
    void showDiacritics(){
        String[] names={"َ  ـَ  فتحه","ِ  ـِ  کسره","ُ  ـُ  ضمه","ْ  ـْ  سکون","ّ  ـّ  تشدید","ً  ـً  تنوین فتح","ٍ  ـٍ  تنوین کسر","ٌ  ـٌ  تنوین ضم","ٰ  الف خنجری","ٓ  مدّ","ٔ  همزه بالا","ٕ  همزه پایین"};
        String[] marks={"\u064e","\u0650","\u064f","\u0652","\u0651","\u064b","\u064d","\u064c","\u0670","\u0653","\u0654","\u0655"};
        new AlertDialog.Builder(this).setTitle("حرکات کوتاه عربی").setItems(names,(d,w)->{InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(marks[w],1);}).setNegativeButton("بستن",null).show();
    }
    void type(String s) {
        // UI-only pickers must open even if the current editor temporarily has no InputConnection.
        switch(s){
            case "😀": case "Emoji": showEmojiPicker(); return;
            case "اعراب": case "Diacritics": showDiacritics(); return;
            case "علامت": case "علامتها": case "Symbols": showSymbolPicker(); return;
        }
        InputConnection ic=getCurrentInputConnection(); if(ic==null)return;
        switch(s){
            case "⌫": ic.deleteSurroundingText(1,0); return;
            case "Caps": caps=!caps; setInputView(buildKeyboard()); return;
            case "🌐 EN/فا": english=!english; setInputView(buildKeyboard()); return;
            case "Enter": { int action=getCurrentInputEditorInfo()!=null ? (getCurrentInputEditorInfo().imeOptions & android.view.inputmethod.EditorInfo.IME_MASK_ACTION) : 0; if(action!=0 && action!=android.view.inputmethod.EditorInfo.IME_ACTION_NONE && action!=android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action); else ic.commitText("\n",1); return; }
            case "Space": ic.commitText(" ",1); return;
            case "Paste": ic.performContextMenuAction(android.R.id.paste); return;
            case "Copy": ic.performContextMenuAction(android.R.id.copy); return;
            case "Select": ic.performContextMenuAction(android.R.id.selectAll); return;
            case "Undo": undoLast(); return;
            case "Redo": redoLast(); return;
            case "←": sendDpad(android.view.KeyEvent.KEYCODE_DPAD_LEFT); return;
            case "→": sendDpad(android.view.KeyEvent.KEYCODE_DPAD_RIGHT); return;
            case "↑": sendDpad(android.view.KeyEvent.KEYCODE_DPAD_UP); return;
            case "↓": sendDpad(android.view.KeyEvent.KEYCODE_DPAD_DOWN); return;
            case "File": runWebsiteControl(true); return;
            case "Send": runWebsiteControl(false); return;
            case "Tool": showCalculator(); return;
            case "Muse": showMuse(); return;
            case "Resize": resizeLevel=(resizeLevel+1)%3; setInputView(buildKeyboard()); Toast.makeText(this, resizeLevel==1?"اندازهٔ کیبورد: ۷۵٪":resizeLevel==2?"اندازهٔ کیبورد: ۵۰٪":"اندازهٔ عادی",Toast.LENGTH_SHORT).show(); return;
            case "History": showClipboardHistory(); return;
            case "Copy Screen": Toast.makeText(this,"برای کپی، متن انتخاب‌شده را کپی کنید",Toast.LENGTH_SHORT).show(); return;
        }
        ic.commitText(s,1);
        undoStack.push(s); redoStack.clear();
    }

    void undoLast(){
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return;
        if(undoStack.isEmpty()){Toast.makeText(this,"چیزی برای واگردانی ثبت نشده است",Toast.LENGTH_SHORT).show();return;}
        String value=undoStack.pop();
        ic.deleteSurroundingText(value.length(),0);
        redoStack.push(value);
    }
    void redoLast(){
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return;
        if(redoStack.isEmpty()){Toast.makeText(this,"چیزی برای انجام دوباره ثبت نشده است",Toast.LENGTH_SHORT).show();return;}
        String value=redoStack.pop();
        ic.commitText(value,1);
        undoStack.push(value);
    }

    void sendDpad(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long now = System.currentTimeMillis();
        ic.sendKeyEvent(new android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN, keyCode, 0));
        ic.sendKeyEvent(new android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_UP, keyCode, 0));
    }
    void startDeleteRepeat() {
        stopDeleteRepeat();
        repeatDelete = new Runnable() {
            @Override public void run() {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
                repeatHandler.postDelayed(this, 75);
            }
        };
        repeatHandler.post(repeatDelete);
    }
    void stopDeleteRepeat() {
        if (repeatDelete != null) repeatHandler.removeCallbacks(repeatDelete);
        repeatDelete = null;
    }
    void showVariants(String key){
        String[] variants;
        switch(key){case "و": variants=new String[]{"و","ؤ","ٶ","ۄ","ۆ","ۇ","ۈ","ۋ"};break;
        case "ا": variants=new String[]{"ا","أ","إ","آ","ٱ","ٲ","ٳ"};break;
        case "ی": variants=new String[]{"ی","ي","ى","ئ","ې","ے"};break;
        case "ک": variants=new String[]{"ک","ك","ګ","گ"};break;
        case "[": variants=new String[]{"[","{","（","［","⦃","⟦"};break;
        case "}": variants=new String[]{"}","]","）","］","⦄","⟧"};break;
        default: variants=new String[]{key};}
        LinearLayout choices=new LinearLayout(this); choices.setOrientation(LinearLayout.VERTICAL);
        for(String v:variants){ TextView choice=button(v,Color.rgb(250,249,242),()->{}); choice.setTextSize(25*keyboardScale()); choices.addView(choice,new LinearLayout.LayoutParams(-1,dp(44))); choice.setOnClickListener(view->{ type(v); if(choices.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup)choices.getParent()).removeView(choices); }); }
        ScrollView scroll=new ScrollView(this); scroll.addView(choices);
        new AlertDialog.Builder(this).setTitle("زیرگونه‌های «"+key+"» — یکی را انتخاب کنید").setView(scroll).setNegativeButton("بستن",null).show();
    }
    void showCalculator(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); EditText expr=new EditText(this); expr.setSingleLine(true); expr.setHint("مثلاً 12+3*4"); box.addView(expr);
        TextView result=new TextView(this); result.setTextSize(22*keyboardScale()); box.addView(result);
        Button calc=new Button(this); calc.setText("محاسبه"); calc.setOnClickListener(v->{try{double value=new ArithmeticParser(expr.getText().toString().replace('×','*').replace('÷','/').replace('−','-')).parse(); result.setText("= "+(value==(long)value?Long.toString((long)value):Double.toString(value)));}catch(Exception e){result.setText("عبارت نامعتبر است");}}); box.addView(calc);
        new AlertDialog.Builder(this).setTitle("ماشین حساب").setView(box).setPositiveButton("درج نتیجه", (d,w)->{if(getCurrentInputConnection()!=null)getCurrentInputConnection().commitText(result.getText(),1);}).setNegativeButton("بستن",null).show();
    }
    void runWebsiteControl(boolean upload){
        if(upload){
            try {
                Intent picker = new Intent(this, FilePickerActivity.class);
                picker.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(picker);
            } catch(Exception e) {
                Toast.makeText(this,"انتخاب‌گر فایل دستگاه باز نشد",Toast.LENGTH_LONG).show();
            }
            return;
        }
        InputConnection ic=getCurrentInputConnection();
        android.view.inputmethod.EditorInfo info=getCurrentInputEditorInfo();
        if(ic==null){ Toast.makeText(this,"کادر ورودی فعالی پیدا نشد",Toast.LENGTH_SHORT).show(); return; }
        int action=info==null?0:(info.imeOptions & android.view.inputmethod.EditorInfo.IME_MASK_ACTION);
        // Prefer the editor's declared submit action. Do not fall back to Enter,
        // because in chat sites Enter may insert a newline rather than send.
        if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
            action == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
            action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
            action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
            boolean done = ic.performEditorAction(action);
            if (!done) Toast.makeText(this,"ویرایشگر سایت فرمان ارسال را نپذیرفت؛ دکمهٔ ارسال خود سایت را لمس کنید.",Toast.LENGTH_LONG).show();
        } else {
            boolean done = ic.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
            if (!done) Toast.makeText(this,"این سایت فرمان ارسال را از کیبورد نمی‌پذیرد؛ برای ارسال، دکمهٔ خود سایت لازم است.",Toast.LENGTH_LONG).show();
        }
    }
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
    @Override public void onFinishInputView(boolean finishingInput) { stopDeleteRepeat(); super.onFinishInputView(finishingInput); }

    static class ArithmeticParser {
        final String s; int p=0;
        ArithmeticParser(String input){s=input.replace(" ","");}
        double parse(){double v=expr(); if(p<s.length())throw new IllegalArgumentException(); return v;}
        double expr(){double v=term(); while(p<s.length()){char c=s.charAt(p);if(c!='+'&&c!='-')break;p++;double n=term();v=c=='+'?v+n:v-n;}return v;}
        double term(){double v=factor();while(p<s.length()){char c=s.charAt(p);if(c!='*'&&c!='/')break;p++;double n=factor();if(c=='/'&&n==0)throw new ArithmeticException();v=c=='*'?v*n:v/n;}return v;}
        double factor(){if(p<s.length()&&(s.charAt(p)=='+'||s.charAt(p)=='-')){char c=s.charAt(p++);double v=factor();return c=='-'?-v:v;} if(p<s.length()&&s.charAt(p)=='('){p++;double v=expr();if(p>=s.length()||s.charAt(p++)!=')')throw new IllegalArgumentException();return v;}int start=p;while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.'))p++;if(start==p)throw new IllegalArgumentException();return Double.parseDouble(s.substring(start,p));}
    }
    void showEmojiPicker(){
        final String[] emojis={"😀","😂","🥰","😍","😎","😭","😡","👍","👎","👏","🙏","❤️","💔","🔥","🎉","🌹","😊","😉","🤔","😴","🤝","💯","✨","✅"};
        // Use the IME's own view instead of an AlertDialog. Some Android/IME window
        // configurations reject service-context dialogs, making the emoji key appear dead.
        final InputConnection openingConnection = getCurrentInputConnection();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(4), dp(4), dp(4), dp(4));
        panel.setBackgroundColor(bg);

        LinearLayout heading = row();
        TextView title = button("انتخاب ایموجی", Color.rgb(250,249,242), null);
        title.setTextSize(16 * keyboardScale());
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1));
        TextView close = button("✕ بازگشت", Color.rgb(250,220,225), () -> setInputView(buildKeyboard()));
        close.setTextSize(14 * keyboardScale());
        heading.addView(close, new LinearLayout.LayoutParams(dp(100), dp(40)));
        panel.addView(heading);

        for (int i = 0; i < emojis.length; i += 6) {
            LinearLayout line = row();
            int limit = Math.min(i + 6, emojis.length);
            for (int j = i; j < limit; j++) {
                final String emoji = emojis[j];
                TextView key = button(emoji, Color.rgb(255,255,255), () -> {
                    InputConnection target = getCurrentInputConnection();
                    if (target == null) target = openingConnection;
                    if (target != null) {
                        target.commitText(emoji, 1);
                        undoStack.push(emoji);
                        redoStack.clear();
                    } else {
                        Toast.makeText(this,"ابتدا کادر نوشتن را انتخاب کنید.",Toast.LENGTH_LONG).show();
                    }
                    setInputView(buildKeyboard());
                });
                key.setTextSize(27 * keyboardScale());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                line.addView(key, lp);
            }
            for (int j = limit - i; j < 6; j++) {
                View spacer = new View(this);
                line.addView(spacer, new LinearLayout.LayoutParams(0, dp(48), 1));
            }
            panel.addView(line);
        }
        setInputView(panel);
    }
    void showSymbolPicker(){
        String[] symbols={"!","@","#","$","%","^","&","*","(",")","_","-","+","=","/","\\","|","~","`","<",">","[","]","{","}","€","¥","£","✓","©","®","™","°","…","؟","،","؛","«","»"};
        new AlertDialog.Builder(this).setTitle("انتخاب علامت").setItems(symbols,(d,w)->type(symbols[w])).setNegativeButton("بستن",null).show();
    }
    View numberKey(String label, float weight, int color){
        String[] parts=label.split(" ",2); String symbol=parts.length>1?parts[0]:""; String digit=parts.length>1?parts[1]:label;
        // Companion symbol is shown small in the lower-left corner; number stays prominent.
        android.widget.FrameLayout frame=new android.widget.FrameLayout(this);
        frame.setBackground(shape(color));
        TextView num=new TextView(this); num.setText(digit); num.setTextSize(25*keyboardScale()); num.setTypeface(Typeface.DEFAULT,Typeface.BOLD); num.setTextColor(ink); num.setGravity(Gravity.CENTER);
        frame.addView(num,new android.widget.FrameLayout.LayoutParams(-1,-1));
        if(!symbol.isEmpty()){ TextView sym=new TextView(this); sym.setText(symbol); sym.setTextSize(11*keyboardScale()); sym.setTextColor(ink); sym.setGravity(Gravity.BOTTOM|Gravity.LEFT); sym.setPadding(dp(3),0,0,dp(1)); android.widget.FrameLayout.LayoutParams sp=new android.widget.FrameLayout.LayoutParams(-1,-1); frame.addView(sym,sp); }
        frame.setOnClickListener(v->{frame.setBackground(shape(Color.rgb(255,242,176)));frame.postDelayed(()->frame.setBackground(shape(color)),450);type(digit);});
        frame.setOnLongClickListener(v->{if(!symbol.isEmpty())type(symbol);return true;});
        frame.setContentDescription(digit+(symbol.isEmpty()?"":"، علامت "+symbol));
        frame.setFocusable(true);
        frame.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),weight));
        return frame;
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
        TextView file=button("File",Color.rgb(219,238,255),()->type("File"));nav.addView(file,new LinearLayout.LayoutParams(0,dp(34),1));TextView send=button("Send",Color.rgb(207,242,215),()->type("Send"));nav.addView(send,new LinearLayout.LayoutParams(0,dp(34),1));root.addView(nav);
        LinearLayout nums=row();
        String[] ns = english
            ? new String[]{"! 1","@ 2","# 3","$ 4","% 5","^ 6","& 7","* 8","( 9",") 0"}
            : new String[]{"! ۱","@ ۲","# ۳","$ ۴","% ۵","^ ۶","& ۷","* ۸","( ۹",") ۰"};
        for(String n:ns){ View nk=numberKey(n,0.95f,bg); LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,dp(48),0.95f); nlp.setMargins(dp(1),dp(1),dp(1),dp(1)); nums.addView(nk,nlp); }
        addKey(nums,"⌫",1.55f,Color.rgb(250,220,225));root.addView(nums);
        String[][] rows= english ? new String[][]{{caps?"Q":"q",caps?"W":"w",caps?"E":"e",caps?"R":"r",caps?"T":"t",caps?"Y":"y",caps?"U":"u",caps?"I":"i",caps?"O":"o",caps?"P":"p"},{caps?"A":"a",caps?"S":"s",caps?"D":"d",caps?"F":"f",caps?"G":"g",caps?"H":"h",caps?"J":"j",caps?"K":"k",caps?"L":"l"},{"Caps",caps?"Z":"z",caps?"X":"x",caps?"C":"c",caps?"V":"v",caps?"B":"b",caps?"N":"n",caps?"M":"m",",",".","Enter"}} : new String[][]{{"ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"},{"ش","س","ی","ب","ل","ا","ت","ن","م","ک","گ"},{"Caps","ظ","ط","ژ","ز","ر","ذ","د","پ","و","چ","Enter"}};
        for(int ri=0;ri<rows.length;ri++){LinearLayout r=row();for(String k:rows[ri]){ if(k.equals("Caps")){ addKey(r,k,1.15f,bg); } else if(k.equals("Enter")){ addKey(r,k,2.15f,Color.rgb(210,229,250)); } else addKey(r,k,1,bg); }root.addView(r);}
        LinearLayout bottom=row();
        String[] b={"😀","علامت","اعراب","🌐 EN/فا","Space","،",".","؟","←","→","↑","↓","=","-","[","}"};
        float[] weights={0.7f,0.75f,0.7f,1.0f,2.0f,0.55f,0.55f,0.55f,0.9f,0.9f,0.5f,0.5f,0.45f,0.45f,0.4f,0.4f};
        for(int i=0;i<b.length;i++){final String label=b[i];TextView t=button(label,bg,()->{if(label.equals("Space"))type("Space");else if(label.equals("Enter"))type("Enter");else type(label);});t.setTextSize((label.length()>2?13:21)*keyboardScale());if(label.equals("[")||label.equals("}"))t.setOnLongClickListener(v->{showVariants(label);return true;});bottom.addView(t,new LinearLayout.LayoutParams(0,dp(44),weights[i]));}
        root.addView(bottom);
        return root;
    }
}
