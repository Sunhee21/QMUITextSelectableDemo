package com.example.newtextdemo.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;

import androidx.annotation.IntDef;

import com.example.newtextdemo.R;

/**
 * @author sunhee
 * @intro
 * @date 2019/11/1
 */
public class SelectableHelper {


    public QMUIQQFaceView2.DrawElementIndex mSlectionStart, mSlectionEnd;


    private QMUIQQFaceView2 mTextView;
    private Context mContext;

    private boolean isSelectable;

    private boolean isSelecting;

    public SelectionHandleView mStarHandlerView;
    public SelectionHandleView mEndHandleView;

    public int selectionColor = Color.RED;

    private int mLastShowX;
    private int mLastShowY;

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;
    private View.OnAttachStateChangeListener mOnAttachStateChangeListener;

    public SelectableHelper(QMUIQQFaceView2 textView) {
        this.mTextView = textView;
        mContext = mTextView.getContext().getApplicationContext();
    }

    public boolean isSelectable() {
        return isSelectable;
    }

    public void setSelectable(boolean selectable) {
        isSelectable = selectable;
    }

    public void selectAll() {
        mStarHandlerView = new SelectionHandleView(HANDLEVIEW_START);
        mEndHandleView = new SelectionHandleView(HANDLEVIEW_END);
        setSelection(mTextView.getFirstElementIndex(), mTextView.getLastElementIndex());
    }

    public void setSelection(QMUIQQFaceView2.DrawElementIndex start, QMUIQQFaceView2.DrawElementIndex end) {
        mSlectionStart = start;
        mSlectionEnd = end;
        mTextView.invalidate();
    }

    private static final String TAG = "SelectableHelper";

    public void selectElementIndex(QMUIQQFaceView2.DrawElementIndex start, QMUIQQFaceView2.DrawElementIndex end) {
        if (start != null) {
            mSlectionStart = start;
        }
        if (end != null) {
            mSlectionEnd = end;
        }

        if (mSlectionStart.index > mSlectionEnd.index) {
            QMUIQQFaceView2.DrawElementIndex temp = mSlectionStart;
            mSlectionStart = mSlectionEnd;
            mSlectionEnd = temp;
        }
        Log.d(TAG, String.format("update selectElementIndex:  %d , %d", mSlectionStart.index, mSlectionEnd.index));
        mTextView.invalidate();
        CharSequence charSequence = mTextView.getText();

        notifyTextSelect(charSequence.subSequence(mSlectionStart.index, mSlectionEnd.index+mSlectionEnd.charWidth), mSlectionStart.index, mSlectionEnd.index);
    }

//    public boolean isInSelectLine(int line) {
//        return line >= mSlectionStart.line && line <= mSlectionEnd.line;
//    }


    public boolean isInSelectArea(int line, int x) {
        return mSlectionStart != null && mSlectionEnd != null
                && line >= mSlectionStart.line && line <= mSlectionEnd.line
                && x >= mSlectionStart.startX && x < mSlectionEnd.endX;
    }


    public int[] qujiaoji(int s1, int e1, int s2, int e2) {
        if (e1 < s2 || e2 < s1) {
            return null;
        } else {
            return new int[]{Math.max(s1, s2), Math.min(e1, e2)};
        }
    }


    public boolean isSelecting() {
        return isSelecting;
    }

    public void setSelecting(boolean selecting) {
        isSelecting = selecting;
    }

    public void turnOnSelectable() {
        initSelectable();
    }

    public void turnOffSelectable() {
        notifyWindowHide();//弹窗关闭
        setSelecting(false);
        mTextView.invalidate();
        mTextView.removeOnAttachStateChangeListener(mOnAttachStateChangeListener);
        mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        mTextView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        if (mStarHandlerView != null) {
            mStarHandlerView.hide();
        }
        if (mEndHandleView != null) {
            mEndHandleView.hide();
        }
        mStarHandlerView = null;
        mEndHandleView = null;
    }

    private boolean isHideWhenScroll;

    private final Runnable mShowSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSelecting) return;
            if (mStarHandlerView != null) {
                showSelectionHandleView(mStarHandlerView);
            }
            if (mEndHandleView != null) {
                showSelectionHandleView(mEndHandleView);
            }
            int[] point = calculatePoint();
            notifyWindowShow(point[0], point[1]);
        }
    };

    private void initSelectable() {
        mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (isHideWhenScroll) {
                    isHideWhenScroll = false;
                    mTextView.removeCallbacks(mShowSelectViewRunnable);
                    mTextView.postDelayed(mShowSelectViewRunnable, 100);
                }
                return true;
            }
        };
        mOnAttachStateChangeListener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                turnOffSelectable();
            }
        };
        mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (!isHideWhenScroll && isSelecting) {
                    isHideWhenScroll = true;
                    notifyWindowHide();
                    if (mStarHandlerView != null) mStarHandlerView.hide();
                    if (mEndHandleView != null) mEndHandleView.hide();
                }
            }
        };
        mStarHandlerView = new SelectionHandleView(HANDLEVIEW_START);
        mEndHandleView = new SelectionHandleView(HANDLEVIEW_END);
    }

    public void performLongClickSelect(MotionEvent e) {
        if (isSelectable && !isSelecting) {
            isSelecting = true;
            selectAll();
            showSelectionHandleView(mStarHandlerView);
            showSelectionHandleView(mEndHandleView);
            notifyWindowShow(mLastShowX = (int) e.getRawX(), mLastShowY = (int) e.getRawY());
            notifyTextSelect(mTextView.getText(), 0, mTextView.getText().length() - 1);
            mTextView.addOnAttachStateChangeListener(mOnAttachStateChangeListener);
            mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
            mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);
        }
    }


    public void showSelectionHandleView(SelectionHandleView handleView) {
        QMUIQQFaceView2.DrawElementIndex offset = handleView.isStart() ? mSlectionStart : mSlectionEnd;
        Log.d(TAG, String.format("showSelectionHandleView: offset.startX %s  // offset.line %s // mTextView.getLineBottom(offset.line) %s"
                , offset.startX
                , offset.line
                , mTextView.getLineBottom(offset.line)));
        handleView.show(handleView.isStart() ? offset.startX : offset.endX, mTextView.getLineBottom(offset.line));
    }


    private SelectionHandleView getSelectionHandleView(@HandleViewType int handleViewType) {
        if (handleViewType == HANDLEVIEW_START) {
            return mStarHandlerView.isStart() ? mStarHandlerView : mEndHandleView;
        } else {
            return !mStarHandlerView.isStart() ? mStarHandlerView : mEndHandleView;
        }
    }

    private int[] calculatePoint() {
        int[] point = new int[2];
        int[] relativeOnWindow = new int[2];
        mTextView.getLocationOnScreen(relativeOnWindow);
        point[0] = mSlectionStart.startX + relativeOnWindow[0];

        point[1] = (int) (relativeOnWindow[1]
                + mTextView.getPaddingTop()
                + mTextView.getLineBottom(mSlectionStart.line)
                - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 37, mTextView.getContext().getResources().getDisplayMetrics()));
        if (point[1] <= 0) {
            Rect rect = new Rect();
            if (mTextView.getGlobalVisibleRect(rect)) {
                point[1] = rect.top;
            }
        }
        return point;
    }


    @IntDef({HANDLEVIEW_START, HANDLEVIEW_END})
    public @interface HandleViewType {
    }

    static final int HANDLEVIEW_START = 0;
    static final int HANDLEVIEW_END = 1;


    /**
     * 起始游标
     */
    class SelectionHandleView extends View implements PopupWindow.OnDismissListener {

        @HandleViewType
        private int mHandleViewType = HANDLEVIEW_START;

        private final PopupWindow mContainer;
        private Paint mPaint;


        private int mCircleRadius = 50 / 2;
        private int mHandleWidth = mCircleRadius * 2;
        private int mHandleHeight = mCircleRadius * 2;
        private QMUIQQFaceView2.DrawElementIndex mBeforeDragStart;
        private QMUIQQFaceView2.DrawElementIndex mBeforeDragEnd;

        public SelectionHandleView(@HandleViewType int type) {
            super(mContext);
            int paddingLeft = mContext.getResources().getDimensionPixelSize(R.dimen.dp_10);
            mHandleViewType = type;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(Color.BLUE);
            mContainer = new PopupWindow(mTextView.getContext());
            mContainer.setClippingEnabled(false);
            mContainer.setWidth(mHandleWidth + paddingLeft * 2);
            mContainer.setHeight(mHandleHeight + paddingLeft);
            mContainer.setContentView(this);
            mContainer.setBackgroundDrawable(null);
            setBackground(new ColorDrawable(Color.GREEN));
            setPadding(paddingLeft, 0, paddingLeft, paddingLeft);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(mCircleRadius + getPaddingLeft(), mCircleRadius, mCircleRadius, mPaint);
            if (mHandleViewType == HANDLEVIEW_START) {
                canvas.drawRect(mCircleRadius + getPaddingLeft(), 0, mCircleRadius * 2 + getPaddingLeft(), mCircleRadius, mPaint);
            } else {
                canvas.drawRect(getPaddingLeft(), 0, mCircleRadius + getPaddingLeft(), mCircleRadius, mPaint);
            }
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mBeforeDragStart = mSlectionStart;
                    mBeforeDragEnd = mSlectionEnd;
                    notifyWindowHide();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    int[] point = calculatePoint();
                    notifyWindowShow(mLastShowX = point[0], mLastShowY = point[1]);
                    break;
                case MotionEvent.ACTION_MOVE: {
                    int rawX = (int) event.getRawX();
                    int rawY = (int) event.getRawY();
                    int[] relativeOnScreen = new int[2];
                    mTextView.getLocationOnScreen(relativeOnScreen);
                    update(rawX - relativeOnScreen[0], rawY - relativeOnScreen[1]);
                }
                break;
            }
            return true;
        }

        private void update(int x, int y) {
            int[] relativeOnWindow = new int[2];
            mTextView.getLocationOnScreen(relativeOnWindow);
            QMUIQQFaceView2.DrawElementIndex oldOffset;
            if (isStart()) {
                oldOffset = mSlectionStart;
            } else {
                oldOffset = mSlectionEnd;
            }

            QMUIQQFaceView2.DrawElementIndex newOffset = mTextView.getElementIndexForHorizontal(mTextView.getLineForVertical(y), x);
            if (newOffset.index != oldOffset.index) {
                if (isStart()) {
                    if (newOffset.index > mBeforeDragEnd.index) {
                        Log.d(TAG, String.format("update isStart: > new %s //  old %s", newOffset.index, mBeforeDragEnd.index));
                        SelectionHandleView handleView = getSelectionHandleView(HANDLEVIEW_END);
                        Log.d(TAG, "update: handleView type == " + handleView.isStart());
                        handleView.changeDirection();
                        Log.d(TAG, "update: handleView type == " + handleView.isStart());
                        changeDirection();
                        Log.d(TAG, "update: this type == " + isStart());
                        mBeforeDragStart = mBeforeDragEnd;
                        selectElementIndex(mBeforeDragEnd, newOffset);
                        handleView.updateHandlerView();
                    } else if (newOffset.index == mBeforeDragEnd.index) {
                        Log.d(TAG, String.format("update isStart: == new %s //  old %s", newOffset.index, mBeforeDragEnd.index));
                        selectElementIndex(newOffset, mBeforeDragEnd);
                    } else {
                        Log.d(TAG, String.format("update isStart: < new %s //  old %s", newOffset.index, mBeforeDragEnd.index));
                        selectElementIndex(newOffset, null);
                    }
                    updateHandlerView();
                } else {
                    if (newOffset.index < mBeforeDragStart.index) {
                        Log.d(TAG, String.format("update isEnd: > new %s //  old %s", newOffset.index, mBeforeDragStart.index));
                        SelectionHandleView handleView = getSelectionHandleView(HANDLEVIEW_START);
                        handleView.changeDirection();
                        changeDirection();
                        mBeforeDragEnd = mBeforeDragStart;
                        selectElementIndex(newOffset, mBeforeDragStart);
                        handleView.updateHandlerView();
                    } else if (newOffset.index == mBeforeDragStart.index) {
                        Log.d(TAG, String.format("update isEnd: == new %s //  old %s", newOffset.index, mBeforeDragStart.index));
                        selectElementIndex(mBeforeDragStart, newOffset);
                    } else {
                        Log.d(TAG, String.format("update isEnd: < new %s //  old %s", newOffset.index, mBeforeDragStart.index));
                        selectElementIndex(mBeforeDragStart, newOffset);
                    }
                    updateHandlerView();
                }
            }

        }

        private void updateHandlerView() {
            QMUIQQFaceView2.DrawElementIndex offset = isStart() ? mSlectionStart : mSlectionEnd;
            int x = getShowX(isStart() ? offset.startX : offset.endX);
            int y = getShowY(mTextView.getLineBottom(offset.line));
            isInGlobalVisibleRect(x, y);
            if (isStart()) {
                Log.d(TAG, "updateHandlerView: start------------>" + mSlectionStart.startX + " index-----  " + mSlectionStart.index);
                mContainer.update(x, y, -1, -1);
            } else {
                Log.d(TAG, "updateHandlerView: end------------>" + mSlectionEnd.endX + " index-----  " + mSlectionEnd.index);
                mContainer.update(x, y, -1, -1);
            }
        }


//        public int getExtraX() {
//            int[] relativeOnWindow = new int[2];
//            mTextView.getLocationOnScreen(relativeOnWindow);
//            return relativeOnWindow[0] - getPaddingLeft() + mTextView.getPaddingLeft();
//        }
//
//        public int getExtraY() {
//            int[] relativeOnWindow = new int[2];
//            mTextView.getLocationOnScreen(relativeOnWindow);
//            return relativeOnWindow[1] + mTextView.getPaddingTop();
//        }

        public int getShowX(int textHorizontalOffset) {
            int[] relativeOnWindow = new int[2];
            mTextView.getLocationOnScreen(relativeOnWindow);
            return relativeOnWindow[0] + textHorizontalOffset - (isStart() ? getPaddingLeft() + mHandleWidth : getPaddingLeft());
        }

        public int getShowY(int textVerticalOffset) {
            int[] relativeOnWindow = new int[2];
            mTextView.getLocationOnScreen(relativeOnWindow);
            return relativeOnWindow[1] + mTextView.getPaddingTop() + textVerticalOffset;
        }

        public void show(int textHorizontalOffset, int textVerticalOffset) {
            int showX = getShowX(textHorizontalOffset);
            int showY = getShowY(textVerticalOffset);
            isInGlobalVisibleRect(showX, showY);

            mContainer.showAtLocation(mTextView, Gravity.NO_GRAVITY, showX, showY);

        }

        private void isInGlobalVisibleRect(int showX, int showY) {
            Rect globalVisibleRect = new Rect();
            Log.d(TAG, "isInGlobalVisibleRect: " + showX + "," + showY);
            int cursorX = isStart() ? showX + getPaddingLeft() + mHandleWidth : showX + getPaddingLeft();//游标尖角的坐标
            int cursorY = showY;
            if (mTextView.getGlobalVisibleRect(globalVisibleRect) && globalVisibleRect.contains(cursorX, cursorY)) {
                setVisibility(VISIBLE);
                Log.d(TAG, String.format("GlobalVisibleRect: %s,%s,%s,%s", globalVisibleRect.left, globalVisibleRect.top, globalVisibleRect.right, globalVisibleRect.bottom));
            } else {
                setVisibility(GONE);
            }
        }


        private void changeDirection() {
            if (mHandleViewType == HANDLEVIEW_START) {
                mHandleViewType = HANDLEVIEW_END;
            } else {
                mHandleViewType = HANDLEVIEW_START;
            }
            invalidate();
        }


        public void hide() {
            mContainer.dismiss();
        }


        @Override
        public void onDismiss() {

        }

        public boolean isStart() {
            return mHandleViewType == HANDLEVIEW_START;
        }

    }

    public void notifyTextSelect(CharSequence charSequence, int start, int end) {
        if (onTextSelectListener != null) {
            onTextSelectListener.onTextSelected(charSequence, start, end);
        }
    }

    public void notifyWindowShow(int x, int y) {
        if (onTextSelectListener != null) {
            onTextSelectListener.onPerformShow(new Point(x, y));
        }
    }

    public void notifyWindowHide() {
        if (onTextSelectListener != null) {
            onTextSelectListener.onPerformDismiss();
        }
    }


    private OnTextSelectListener onTextSelectListener;


    public void setOnTextSelectListener(OnTextSelectListener onTextSelectListener) {
        this.onTextSelectListener = onTextSelectListener;
    }

    /**
     * 控制弹窗显示隐藏的时机及文本选中内容
     */
    public interface OnTextSelectListener {
        void onTextSelected(CharSequence content, int start, int end);

        //+2个弹窗该弹出该隐藏的时机的回调
        void onPerformShow(Point point);

        void onPerformDismiss();
    }

}
