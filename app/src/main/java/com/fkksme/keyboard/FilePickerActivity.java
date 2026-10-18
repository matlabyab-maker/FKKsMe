package com.fkksme.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.content.ClipDescription;
import android.widget.Toast;

public class FilePickerActivity extends Activity {
  private static final int PICK_FILE=4107;
  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("*/*");
    try { startActivityForResult(i,PICK_FILE); }
    catch(Exception e){ Toast.makeText(this,"انتخاب‌گر فایل در دسترس نیست",Toast.LENGTH_LONG).show(); finish(); }
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode==PICK_FILE && resultCode==RESULT_OK && data!=null && data.getData()!=null){
      Uri uri=data.getData();
      if(Build.VERSION.SDK_INT>=25 && FKKsMeInputMethodService.activeService!=null){
        try {
          getContentResolver().takePersistableUriPermission(uri,data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch(Exception ignored) {}
        try {
          FKKsMeInputMethodService svc=FKKsMeInputMethodService.activeService;
          InputConnection ic=svc.getCurrentInputConnection();
          if(ic!=null){
            String mime=getContentResolver().getType(uri);
            if(mime==null) mime="application/octet-stream";
            ClipDescription desc=new ClipDescription("FKKsMe file",new String[]{mime});
            InputContentInfo content=new InputContentInfo(uri,desc,null);
            if(ic.commitContent(content,InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,null)){
              Toast.makeText(this,"فایل به کادر ورودی فرستاده شد؛ اگر سایت پشتیبانی کند پیوست می‌شود",Toast.LENGTH_LONG).show();
            } else Toast.makeText(this,"این کادر پیوست مستقیم فایل را پشتیبانی نمی‌کند؛ دکمهٔ پیوست خود سایت را بزنید",Toast.LENGTH_LONG).show();
          }
        } catch(Exception e){ Toast.makeText(this,"این سایت درج مستقیم فایل را پشتیبانی نمی‌کند؛ دکمهٔ پیوست خود سایت را بزنید",Toast.LENGTH_LONG).show(); }
      } else {
        Toast.makeText(this,"برای پیوست فایل از دکمهٔ پیوست خود سایت استفاده کنید",Toast.LENGTH_LONG).show();
      }
    }
    finish();
  }
}
