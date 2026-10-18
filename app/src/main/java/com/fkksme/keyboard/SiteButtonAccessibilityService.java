package com.fkksme.keyboard;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Optional helper: activates visible website upload/attach/send controls. */
public class SiteButtonAccessibilityService extends AccessibilityService {
    private static SiteButtonAccessibilityService instance;
    @Override public void onServiceConnected() { super.onServiceConnected(); instance=this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { if(instance==this) instance=null; super.onDestroy(); }

    public static boolean isEnabled() { return instance != null; }
    public static boolean clickWebsiteControl(boolean upload) {
        SiteButtonAccessibilityService s=instance;
        if(s==null) return false;
        AccessibilityNodeInfo root=s.getRootInActiveWindow();
        if(root==null) return false;
        ArrayList<AccessibilityNodeInfo> nodes=new ArrayList<>(); collect(root,nodes);
        String[][] terms = upload ? new String[][]{
            {"attach file","upload file","choose file","add file","file upload","attachment","attach","upload","browse","پیوست","بارگذاری","آپلود","افزودن فایل","انتخاب فایل","فایل"},
            {"send"}
        } : new String[][]{
            {"send message","send","submit","ارسال پیام","ارسال","فرستادن","ثبت"},
            {"attach","upload","file","پیوست","بارگذاری"}
        };
        for(String term:terms[0]) for(AccessibilityNodeInfo n:nodes) {
            if(matches(n,term) && clickNode(n)) return true;
        }
        // If exact labels are absent, try a clickable node whose resource id hints its role.
        for(AccessibilityNodeInfo n:nodes) {
            String id=""; try { if(n.getViewIdResourceName()!=null) id=n.getViewIdResourceName().toLowerCase(Locale.ROOT); } catch(Exception ignored){}
            boolean relevant=upload ? (id.contains("upload")||id.contains("attach")||id.contains("file")) : (id.contains("send")||id.contains("submit"));
            if(relevant && clickNode(n)) return true;
        }
        return false;
    }
    private static boolean matches(AccessibilityNodeInfo n,String term) {
        String t=term.toLowerCase(Locale.ROOT);
        CharSequence a=n.getText(), b=n.getContentDescription();
        return (a!=null&&a.toString().toLowerCase(Locale.ROOT).contains(t)) || (b!=null&&b.toString().toLowerCase(Locale.ROOT).contains(t));
    }
    private static boolean clickNode(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo p=n;
        for(int i=0;p!=null&&i<8;i++) {
            if(p.isClickable() && p.isEnabled()) return p.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            p=p.getParent();
        }
        return false;
    }
    private static void collect(AccessibilityNodeInfo node,List<AccessibilityNodeInfo> out) {
        if(node==null) return;
        out.add(node);
        for(int i=0;i<node.getChildCount();i++) collect(node.getChild(i),out);
    }
}
