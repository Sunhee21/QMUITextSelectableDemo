package com.example.newtextdemo.face;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @author : SEA
 * @class : com.mostone.lib.widget.face.QDQQFaceView
 * @time : 2019/8/1 20:49
 */
public class QDQQFaceView extends QMUIQQFaceView2 {
    public QDQQFaceView(Context context) {
        this(context, null);
    }

    public QDQQFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCompiler(QMUIQQFaceCompiler2.getInstance(QDQQFaceManager.getInstance()));
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        return false;
//    }
}