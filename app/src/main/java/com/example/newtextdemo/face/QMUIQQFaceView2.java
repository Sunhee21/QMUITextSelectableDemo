package com.example.newtextdemo.face;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.qmuiteam.qmui.QMUILog;
import com.qmuiteam.qmui.R;
import com.qmuiteam.qmui.link.ITouchableSpan;
import com.qmuiteam.qmui.span.QMUITouchableSpan;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUILangHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.view.View.MeasureSpec.AT_MOST;

/**
 * @Author CaiRj
 * @Date 2019/8/2 19:09
 * @Desc
 */
public class QMUIQQFaceView2 extends View implements GestureDetector.OnGestureListener {
    private static final String TAG = "QMUIQQFaceView";
    private CharSequence mOriginText;
    private QMUIQQFaceCompiler2.ElementList mElementList;
    private QMUIQQFaceCompiler2 mCompiler;
    private boolean mOpenQQFace = true;
    private TextPaint mPaint;
    private Paint mSpanBgPaint;
    private Paint mSelectionPaint;
    private int mTextSize;
    private int mTextColor;
    private int mLineSpace = -1;
    private int mFontHeight;
    private int mQQFaceSize = 0;
    private int mFirstBaseLine;
    private int mMaxLine = Integer.MAX_VALUE;
    private boolean mIsSingleLine = false;
    private int mLines = 0;
    private Set<SpanInfo> mSpanInfos = new HashSet<>();
    private boolean mIsTouchDownInMoreText = false;
    private Rect mMoreHitRect = new Rect();
    private static final String mEllipsizeText = " ... ";
    private String mMoreActionText;
    private int mMoreActionColor;
    private int mMoreActionTextLength = 0;
    private int mEllipsizeTextLength = 0;
    private TextUtils.TruncateAt mEllipsize = TextUtils.TruncateAt.END;
    private boolean mIsNeedEllipsize = false;
    private int mNeedDrawLine = 0;
    private int mQQFaceSizeAddon = 0; // 可以让QQ表情高度比字体高度小一点或大一点
    private QQFaceViewListener mListener;
    private int mMaxWidth = Integer.MAX_VALUE;
    private PressCancelAction mPendingPressCancelAction = null;
    private boolean mJumpHandleMeasureAndDraw = false;
    private Runnable mDelayTextSetter = null;
    private boolean mIncludePad = true;
    private Typeface mTypeface = null;
    private int mParagraphSpace = 0; // 段间距
    private int mSpecialDrawablePadding = 0;
    private int mGravity = Gravity.NO_GRAVITY;


    public QMUIQQFaceView2(Context context) {
        this(context, null);
    }

    public QMUIQQFaceView2(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.QMUIQQFaceStyle);
    }

    public QMUIQQFaceView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = getContext().obtainStyledAttributes(attrs,
                R.styleable.QMUIQQFaceView, defStyleAttr, 0);
        mQQFaceSizeAddon = -QMUIDisplayHelper.dp2px(context, 2); // 默认表情小一点好看
        mTextSize = array.getDimensionPixelSize(R.styleable.QMUIQQFaceView_android_textSize,
                QMUIDisplayHelper.dp2px(context, 14));
        mTextColor = array.getColor(R.styleable.QMUIQQFaceView_android_textColor, Color.BLACK);
        mIsSingleLine = array.getBoolean(R.styleable.QMUIQQFaceView_android_singleLine, false);
        mMaxLine = array.getInt(R.styleable.QMUIQQFaceView_android_maxLines, mMaxLine);
        int lineSpace = array.getDimensionPixelOffset(R.
                styleable.QMUIQQFaceView_android_lineSpacingExtra, 0);
        setLineSpace(lineSpace);
        int ellipsize = -1;
        ellipsize = array.getInt(R.styleable.QMUIQQFaceView_android_ellipsize, ellipsize);
        switch (ellipsize) {
            case 1:
                mEllipsize = TextUtils.TruncateAt.START;
                break;
            case 2:
                mEllipsize = TextUtils.TruncateAt.MIDDLE;
                break;
            case 3:
            default:
                mEllipsize = TextUtils.TruncateAt.END;
                break;
        }
        mMaxWidth = array.getDimensionPixelSize(R.styleable.QMUIQQFaceView_android_maxWidth, mMaxWidth);
        mSpecialDrawablePadding = array.getDimensionPixelSize(R.styleable.QMUIQQFaceView_qmui_special_drawable_padding, 0);
        final String text = array.getString(R.styleable.QMUIQQFaceView_android_text);
        if (!QMUILangHelper.isNullOrEmpty(text)) {
            mDelayTextSetter = new Runnable() {
                @Override
                public void run() {
                    setText(text);
                }
            };
        }
        mMoreActionText = array.getString(R.styleable.QMUIQQFaceView_qmui_more_action_text);
        mMoreActionColor = array.getColor(R.styleable.QMUIQQFaceView_qmui_more_action_color, mTextColor);

        array.recycle();
        mPaint = new TextPaint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(mTextColor);
        mEllipsizeTextLength = (int) Math.ceil(mPaint.measureText(mEllipsizeText));
        measureMoreActionTextLength();
        mSpanBgPaint = new Paint();
        mSpanBgPaint.setAntiAlias(true);
        mSpanBgPaint.setStyle(Paint.Style.FILL);
        mSelectionPaint = new Paint();
        mSelectionPaint.setAntiAlias(true);
        mSelectionPaint.setStyle(Paint.Style.FILL);
    }

    public void setOpenQQFace(boolean openQQFace) {
        mOpenQQFace = openQQFace;
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    public int getGravity() {
        return mGravity;
    }

    public void setMaxWidth(int maxWidth) {
        if (mMaxWidth != maxWidth) {
            mMaxWidth = maxWidth;
            requestLayout();
        }
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    SpanInfo mTouchSpanInfo = null;

    private GestureDetector mGesture;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mGesture == null) mGesture = new GestureDetector(getContext(), this);
        mGesture.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();


        if (mSpanInfos.isEmpty() && mMoreHitRect.isEmpty()) {
            return super.onTouchEvent(event);
        }
        final int action = event.getAction();

        if (action != MotionEvent.ACTION_DOWN && (!mIsTouchDownInMoreText && mTouchSpanInfo == null)) {
            return super.onTouchEvent(event);
        }

        // touch事件前先消耗掉还存在的mPendingPressCancelAction
        if (mPendingPressCancelAction != null) {
            mPendingPressCancelAction.run();
            mPendingPressCancelAction = null;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchSpanInfo = null;
                mIsTouchDownInMoreText = false;

                if (mMoreHitRect.contains(x, y)) {
                    mIsTouchDownInMoreText = true;
                } else {
                    for (SpanInfo spanInfo : mSpanInfos) {
                        if (spanInfo.onTouch(x, y)) {
                            mTouchSpanInfo = spanInfo;
                            break;
                        }
                    }
                }

                if (mTouchSpanInfo != null) {
                    mTouchSpanInfo.setPressed(true);
                    mTouchSpanInfo.invalidateSpan();
                } else if (!mIsTouchDownInMoreText) {
                    return super.onTouchEvent(event);
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                mPendingPressCancelAction = null;
                if (mTouchSpanInfo != null) {
                    mTouchSpanInfo.setPressed(false);
                    mTouchSpanInfo.invalidateSpan();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchSpanInfo != null && !mTouchSpanInfo.onTouch(x, y)) {
                    mTouchSpanInfo.setPressed(false);
                    mTouchSpanInfo.invalidateSpan();
                    mTouchSpanInfo = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mTouchSpanInfo != null) {
                    mTouchSpanInfo.onClick();
                    mPendingPressCancelAction = new PressCancelAction(mTouchSpanInfo);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mPendingPressCancelAction != null) {
                                mPendingPressCancelAction.run();
                            }
                        }
                    }, 100);
                } else if (mIsTouchDownInMoreText) {
                    if (mListener != null) {
                        mListener.onMoreTextClick();
                    } else if (isClickable()) {
                        performClick();
                    }
                }

                break;
        }
        return true;
    }

    public void setCompiler(QMUIQQFaceCompiler2 compiler) {
        mCompiler = compiler;
        if (mDelayTextSetter != null) {
            mDelayTextSetter.run();
        }
    }

    public void setTypeface(Typeface typeface) {
        if (mTypeface != typeface) {
            mTypeface = typeface;
            needReCalculateFontHeight = true;
            mPaint.setTypeface(typeface);
            requestLayout();
            invalidate();
        }
    }

    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mPaint.setFakeBoldText(false);
            mPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    public void setParagraphSpace(int paragraphSpace) {
        if (mParagraphSpace != paragraphSpace) {
            mParagraphSpace = paragraphSpace;
            requestLayout();
            invalidate();
        }
    }

    public void setMoreActionText(String moreActionText) {
        if (mMoreActionText == null || !mMoreActionText.equals(moreActionText)) {
            mMoreActionText = moreActionText;
            measureMoreActionTextLength();
            requestLayout();
            invalidate();
        }
    }

    public void setMoreActionColor(int color) {
        if (color != mMoreActionColor) {
            mMoreActionColor = color;
            invalidate();
        }
    }

    private void measureMoreActionTextLength() {
        if (QMUILangHelper.isNullOrEmpty(mMoreActionText)) {
            mMoreActionTextLength = 0;
        } else {
            mMoreActionTextLength = (int) Math.ceil(mPaint.measureText(mMoreActionText));
        }
    }


    public void setSpecialDrawablePadding(int specialDrawablePadding) {
        if (mSpecialDrawablePadding != specialDrawablePadding) {
            mSpecialDrawablePadding = specialDrawablePadding;
            requestLayout();
            invalidate();
        }
    }

    public void setIncludeFontPadding(boolean includepad) {
        if (mIncludePad != includepad) {
            needReCalculateFontHeight = true;
            mIncludePad = includepad;
            requestLayout();
            invalidate();
        }
    }

    public void setQQFaceSizeAddon(int QQFaceSizeAddon) {
        if (mQQFaceSizeAddon != QQFaceSizeAddon) {
            mQQFaceSizeAddon = QQFaceSizeAddon;
            mNeedReCalculateLines = true;
            requestLayout();
            invalidate();
        }
    }

    public void setLineSpace(int lineSpace) {
        if (mLineSpace != lineSpace) {
            mLineSpace = lineSpace;
            requestLayout();
            invalidate();
        }
    }

    public void setEllipsize(TextUtils.TruncateAt where) {
        if (mEllipsize != where) {
            mEllipsize = where;
            requestLayout();
            invalidate();
        }
    }

    public void setMaxLine(int maxLine) {
        if (mMaxLine != maxLine) {
            mMaxLine = maxLine;
            requestLayout();
            invalidate();
        }
    }

    public int getMaxLine() {
        return mMaxLine;
    }

    public int getLineCount() {
        return mLines;
    }

    public boolean isNeedEllipsize() {
        return mIsNeedEllipsize;
    }

    public void setSingleLine(boolean singleLine) {
        if (mIsSingleLine != singleLine) {
            mIsSingleLine = singleLine;
            requestLayout();
            invalidate();
        }
    }

    public void setTextColor(@ColorInt int textColor) {
        if (mTextColor != textColor) {
            mTextColor = textColor;
            mPaint.setColor(textColor);
            invalidate();
        }
    }

    public TextPaint getPaint() {
        return mPaint;
    }

    public void setTextSize(int textSize) {
        if (mTextSize != textSize) {
            mTextSize = textSize;
            mPaint.setTextSize(mTextSize);
            needReCalculateFontHeight = true;
            mNeedReCalculateLines = true;
            mEllipsizeTextLength = (int) Math.ceil(mPaint.measureText(mEllipsizeText));
            measureMoreActionTextLength();
            requestLayout();
            invalidate();
        }
    }

    public int getTextSize() {
        return mTextSize;
    }

    public CharSequence getText() {
        return mOriginText;
    }

    /**
     * make sense only work after draw
     *
     * @return
     */
    public Rect getMoreHitRect() {
        return mMoreHitRect;
    }

    public void setText(CharSequence charSequence) {
        mDelayTextSetter = null;
        CharSequence oldText = mOriginText;
        if (mOriginText != null && mOriginText.equals(charSequence)) {
            return;
        }
        mOriginText = charSequence;
        setContentDescription(charSequence);
        if (mOpenQQFace && mCompiler == null) {
            throw new RuntimeException("mCompiler == null");
        }

        if (QMUILangHelper.isNullOrEmpty(mOriginText)) {
            if (!QMUILangHelper.isNullOrEmpty(oldText)) {
                mElementList = null;
                requestLayout();
                invalidate();
            }
            return;
        }

        if (mOpenQQFace && mCompiler != null) {
            mElementList = mCompiler.compile(mOriginText);
        } else {
            mElementList = new QMUIQQFaceCompiler2.ElementList(0, mOriginText.length());
            String[] strings = mOriginText.toString().split("\\n");
            for (int i = 0; i < strings.length; i++) {
                mElementList.add(QMUIQQFaceCompiler2.Element.createTextElement(strings[i]));
                if (i != strings.length - 1) {
                    mElementList.add(QMUIQQFaceCompiler2.Element.createNextLineElement());
                }
            }
        }
        mNeedReCalculateLines = true;
        if (getLayoutParams() == null) {
            return;
        }
        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT ||
                getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            requestLayout();
            invalidate();
            return;
        }
        int paddingHor = getPaddingLeft() + getPaddingRight();
        int paddingVer = getPaddingBottom() + getPaddingTop();
        if (getWidth() > paddingHor && getHeight() > paddingVer) {
            mLines = 0;
            calculateLinesAndContentWidth(getWidth());
            int oldDrawLine = mNeedDrawLine;
            int maxLine = Math.min((getHeight() - paddingVer + mLineSpace) / (mFontHeight + mLineSpace), mMaxLine);
            calculateNeedDrawLine(maxLine);
            // 优化： 如果高度固定或者绘制的行数相同，则不进行requestLayout
            if (oldDrawLine == mNeedDrawLine) {
                invalidate();
            } else {
                requestLayout();
                invalidate();
            }
        }
    }

    private boolean needReCalculateFontHeight = true;

    protected int calculateFontHeight() {
        if (needReCalculateFontHeight) {
            Paint.FontMetricsInt fontMetricsInt = mPaint.getFontMetricsInt();
            if (fontMetricsInt == null) {
                mFontHeight = mQQFaceSize = 0;
            } else {
                needReCalculateFontHeight = false;
                int top = getFontHeightCalTop(fontMetricsInt, mIncludePad);
                int bottom = getFontHeightCalBottom(fontMetricsInt, mIncludePad);
                int fontHeight = bottom - top;
                mQQFaceSize = fontHeight + mQQFaceSizeAddon;
                int specialMaxDrawableHeight = mCompiler.getSpecialBoundsMaxHeight();
                int drawableSize = Math.max(mQQFaceSize, specialMaxDrawableHeight);
                if (fontHeight >= drawableSize) {
                    mFontHeight = fontHeight;
                    mFirstBaseLine = -top;
                } else {
                    mFontHeight = drawableSize;
                    mFirstBaseLine = -top + (mFontHeight - drawableSize) / 2;
                }
            }
        }
        return mFontHeight;
    }

    public int getFontHeight() {
        return mFontHeight;
    }

    public int getLineSpace() {
        return mLineSpace;
    }

    protected int getFontHeightCalTop(Paint.FontMetricsInt fontMetricsInt, boolean includePad) {
        return includePad ? fontMetricsInt.top : fontMetricsInt.ascent;
    }

    protected int getFontHeightCalBottom(Paint.FontMetricsInt fontMetricsInt, boolean includePad) {
        return includePad ? fontMetricsInt.bottom : fontMetricsInt.descent;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (getPaddingLeft() != left || getPaddingRight() != right) {
            mNeedReCalculateLines = true;
        }
        super.setPadding(left, top, right, bottom);
    }

    private int mCurrentCalWidth = 0;
    private int mCurrentCalLine = 0;
    private int mContentCalMaxWidth = 0;
    private boolean mNeedReCalculateLines = false; // 缓存，避免onMeasure重复计算
    private int mLastCalLimitWidth = 0;
    private int mLastCalContentWidth = 0;
    private int mLastCalLines = 0;


    protected int calculateLinesAndContentWidth(int limitWidth) {
        Log.d(TAG, "calculateLinesAndContentWidth: ");
        if (limitWidth <= (getPaddingRight() + getPaddingLeft()) || isElementEmpty()) {
            mLines = 0;
            mLastCalLines = 0;
            mLastCalContentWidth = 0;
            return mLastCalContentWidth;
        }

        if (!mNeedReCalculateLines && mLastCalLimitWidth == limitWidth) {
            mLines = mLastCalLines;
            return mLastCalContentWidth;
        }
        mLastCalLimitWidth = limitWidth;
        List<QMUIQQFaceCompiler2.Element> elements = mElementList.getElements();
        mSpanInfos.clear();
        mCurrentCalLine = 1;
        mCurrentCalWidth = getPaddingLeft();
        calculateLinesInner(elements, limitWidth);
        if (mCurrentCalLine != mLines) {
            if (mListener != null) {
                mListener.onCalculateLinesChange(mCurrentCalLine);
            }
            mLines = mCurrentCalLine;
        }

        if (mLines == 1) {
            mLastCalContentWidth = mCurrentCalWidth + getPaddingRight();
        } else {
            mLastCalContentWidth = limitWidth;
        }
        mLastCalLines = mLines;

        return mLastCalContentWidth;
    }

    private void calculateNeedDrawLine(int maxline) {
        mNeedDrawLine = mLines;
        if (mIsSingleLine) {
            mNeedDrawLine = Math.min(1, mLines);
        } else if (maxline < mLines) {
            mNeedDrawLine = maxline;
        }

        mIsNeedEllipsize = mLines > mNeedDrawLine;
    }

    private void calculateLinesInner(List<QMUIQQFaceCompiler2.Element> elements, int limitWidth) {
        QMUIQQFaceCompiler2.Element element;
        int widthStart = getPaddingLeft(), widthEnd = limitWidth - getPaddingRight();
        for (int i = 0; i < elements.size(); i++) {
            if (mJumpHandleMeasureAndDraw) {
                break;
            }
            if (mCurrentCalLine > mMaxLine && mEllipsize == TextUtils.TruncateAt.END
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // 针对4.x的手机，如果超过最大行数，就打断测量，但这样存在的问题是getLines获取不到真实的行数
                break;
            }
            element = elements.get(i);
            Log.d(TAG, "Element " + i + " type " + element.getType());
            if (element.getType() == QMUIQQFaceCompiler2.ElementType.DRAWABLE) {
                if (mCurrentCalWidth + mQQFaceSize > widthEnd) {
                    Log.d(TAG, "mCurrentCalWidth + mQQFaceSize > widthEnd");
                    gotoCalNextLine(widthStart);//开启下一行mCurrentCalWidth重置为widthStart
                    putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth + mQQFaceSize, true);
                    mCurrentCalWidth += mQQFaceSize;
                }
//                else if (mCurrentCalWidth + mQQFaceSize == widthEnd) {
//                    Log.d(TAG, "calculateLinesInner: widthEnd = " + widthEnd);
//                    putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth + mQQFaceSize, true);
//                    gotoCalNextLine(widthStart);
//                }
                else {
                    Log.d(TAG, "calculateLinesInner: widthEnd = " + widthEnd);
                    putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth + mQQFaceSize, true);
                    mCurrentCalWidth += mQQFaceSize;
                }
                if (widthEnd - widthStart < mQQFaceSize) {
                    // 一个表情的宽度都容不下
                    mJumpHandleMeasureAndDraw = true;
                }
            } else if (element.getType() == QMUIQQFaceCompiler2.ElementType.TEXT) {
                CharSequence text = element.getText();
                Log.d(TAG, "calculateLinesInner: ElementType.TEXT " + text);
                measureText(text, widthStart, widthEnd);
            } else if (element.getType() == QMUIQQFaceCompiler2.ElementType.SPAN) {
                Log.d(TAG, "calculateLinesInner: ElementType.SPAN ");
                QMUIQQFaceCompiler2.ElementList spanElementList = element.getChildList();
                ITouchableSpan span = element.getTouchableSpan();
                if (spanElementList != null && spanElementList.getElements().size() > 0) {
                    if (span == null) {
                        calculateLinesInner(spanElementList.getElements(), limitWidth);
                        continue;
                    }
                    SpanInfo spanInfo = new SpanInfo(span);
                    spanInfo.setStart(mCurrentCalLine, mCurrentCalWidth);
                    calculateLinesInner(spanElementList.getElements(), limitWidth);
                    spanInfo.setEnd(mCurrentCalLine, mCurrentCalWidth);
                    mSpanInfos.add(spanInfo);
                }
            } else if (element.getType() == QMUIQQFaceCompiler2.ElementType.NEXTLINE) {
                Log.d(TAG, "calculateLinesInner:  line ----- > " + mCurrentCalLine);
                putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth, false);
                gotoCalNextLine(widthStart);
            } else if (element.getType() == QMUIQQFaceCompiler2.ElementType.SPECIAL_BOUNDS_DRAWABLE) {
                Drawable drawable = element.getSpecialBoundsDrawable();
                int width = drawable.getIntrinsicWidth();
                if (i == 0 || i == elements.size() - 1) {
                    width += mSpecialDrawablePadding;
                } else {
                    width += mSpecialDrawablePadding * 2;
                }
                if (mCurrentCalWidth + width > widthEnd) {
                    gotoCalNextLine(widthStart);
                    putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth + width, false);
                    mCurrentCalWidth += width;
                } else if (mCurrentCalWidth + width == widthEnd) {
                    putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth + width, false);
                    gotoCalNextLine(widthStart);
                } else {
                    putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth + width, false);
                    mCurrentCalWidth += width;
                }
                if (widthEnd - widthStart < width) {
                    // 一个表情的宽度都容不下
                    mJumpHandleMeasureAndDraw = true;
                }
            }
        }
    }

    private boolean isElementEmpty() {
        return mElementList == null ||
                mElementList.getElements() == null ||
                mElementList.getElements().isEmpty();
    }

    private void setContentCalMaxWidth(int width) {
        mContentCalMaxWidth = Math.max(width, mContentCalMaxWidth);
    }

    private void gotoCalNextLine(int widthStart) {
        mCurrentCalLine++;
        setContentCalMaxWidth(mCurrentCalWidth);
        mCurrentCalWidth = widthStart;
    }

    private void measureText(CharSequence text, int widthStart, int widthEnd) {

        float[] widths = new float[text.length()];
        mPaint.getTextWidths(text.toString(), widths);
        int contentWidth = widthEnd - widthStart;
        long loop_start = System.currentTimeMillis();
        for (int i = 0; i < widths.length; i++) {
            if (contentWidth < widths[i]) {
                // mCurrentCalWidth已经是最小值，但又一个字都容纳不下，只能说明widthEnd太小，可能还在测量中
                mJumpHandleMeasureAndDraw = true;
                return;
            }
            if (System.currentTimeMillis() - loop_start > 2000) {
                // 3s还没有measure完，那就忽略本次measure以及draw
                QMUILog.d(TAG, "measureText: text = %s, mCurrentCalWidth = %d, " +
                                "widthStart = %d, widthEnd = %d",
                        text, mCurrentCalWidth, widthStart, widthEnd);
                mJumpHandleMeasureAndDraw = true;
                break;
            }
            if (mCurrentCalWidth + widths[i] > widthEnd) {
                gotoCalNextLine(widthStart);
            }
            Log.d(TAG, String.format("mCurrentCalLine : %s , i : %s ,start : %s , end : %s", mCurrentCalLine, i, mCurrentCalWidth, mCurrentCalWidth + (int) Math.ceil(widths[i])));
            putElementIndexOfLine(mCurrentCalLine, mCurrentCalWidth, mCurrentCalWidth + (int) Math.ceil(widths[i]), false);
            mCurrentCalWidth += Math.ceil(widths[i]);
        }
    }

    public void setListener(QQFaceViewListener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        clearElementIndexOfLine();
        long start = System.currentTimeMillis();
        mJumpHandleMeasureAndDraw = false;
        calculateFontHeight();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        Log.i(TAG, "widthSize = " + widthSize + "; heightSize = " + heightSize);

        mLines = 0;
        int width, height;
        switch (widthMode) {
            case AT_MOST:
            default:
                if (mOriginText == null || mOriginText.length() == 0) {
                    width = 0;
                } else {
                    width = calculateLinesAndContentWidth(Math.min(widthSize, mMaxWidth));
                }
                break;
            case MeasureSpec.EXACTLY:
            case MeasureSpec.UNSPECIFIED:
                width = widthSize;
                calculateLinesAndContentWidth(width);
        }

        if (mJumpHandleMeasureAndDraw) {
            setMeasuredDimension(width, heightMode == AT_MOST ? 0 : heightSize);
            return;
        }

        int maxLine = mMaxLine;

        switch (heightMode) {
            case AT_MOST:
                // calculate line count first
                maxLine = (heightSize - getPaddingTop() - getPaddingBottom() + mLineSpace) / (mFontHeight + mLineSpace);
                maxLine = Math.min(maxLine, mMaxLine);
                calculateNeedDrawLine(maxLine);
                height = getPaddingTop() + getPaddingBottom();
                if (mNeedDrawLine < 2) {
                    height += mNeedDrawLine * mFontHeight;
                } else {
                    height += (mNeedDrawLine - 1) * (mFontHeight + mLineSpace) + mFontHeight;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                // calculate line count first
                calculateNeedDrawLine(mMaxLine);
                height = getPaddingTop() + getPaddingBottom();
                if (mNeedDrawLine < 2) {
                    height += mNeedDrawLine * mFontHeight;
                } else {
                    height += (mNeedDrawLine - 1) * (mFontHeight + mLineSpace) + mFontHeight;
                }
                break;
            case MeasureSpec.EXACTLY:
                height = heightSize;
                maxLine = (height - getPaddingTop() - getPaddingBottom() + mLineSpace) / (mFontHeight + mLineSpace);
                maxLine = Math.min(maxLine, mMaxLine);
                calculateNeedDrawLine(maxLine);
                break;
        }
        setMeasuredDimension(width, height);
        Log.i(TAG, "mLines = " + mLines + " ; width = " + width + " ; height = "
                + height + " ; maxLine = " + maxLine + "; measure time = "
                + (System.currentTimeMillis() - start));
    }

//    private int getParagraphCount(){
//        if(mElementList == null || mElementList.getElements() == null || mElementList.getElements().isEmpty()){
//            return 0;
//        }
//        List<QMUIQQFaceCompiler2.Element> elementList = mElementList.getElements();
//        int paragraphCount = 0;
//        for(int i = 0; i < elementList.size(); i++){
//            QMUIQQFaceCompiler2.Element element = elementList.get(i);
//            if(i == elementList.size() - 1){
//                if(element.getType() != QMUIQQFaceCompiler2.ElementType.NEXTLINE){
//                    paragraphCount ++;
//                }
//            }else{
//                if(element.getType() == QMUIQQFaceCompiler2.ElementType.NEXTLINE){
//                    paragraphCount++;
//                }
//            }
//        }
//        return paragraphCount;
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mJumpHandleMeasureAndDraw || mOriginText == null || mLines == 0 || isElementEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        List<QMUIQQFaceCompiler2.Element> elements = mElementList.getElements();
        mCurrentDrawBaseLine = getPaddingTop() + mFirstBaseLine;
        mCurrentDrawLine = 1;
        setStartDrawUsedWidth(getPaddingLeft(), getWidth() - getPaddingLeft() - getPaddingRight());
        mIsExecutedMiddleEllipsize = false;
        drawElements(canvas, elements, getWidth() - getPaddingLeft() - getPaddingRight());
        Log.i(TAG, "onDraw spend time = " + (System.currentTimeMillis() - start));
    }

    private int mCurrentDrawBaseLine;
    private int mCurrentDrawLine;
    private int mCurrentDrawUsedWidth;
    private boolean mIsInDrawSpan = false;
    private QMUITouchableSpan mCurrentDrawSpan;

    private void drawElements(Canvas canvas, List<QMUIQQFaceCompiler2.Element> elements, int usefulWidth) {
        int startLeft = getPaddingLeft(), endWidth = usefulWidth + startLeft;
        if (mIsNeedEllipsize && mEllipsize == TextUtils.TruncateAt.START) {
            int tempColor = mPaint.getColor();
            mPaint.setColor(mTextColor);
            canvas.drawText(mEllipsizeText, 0, mEllipsizeText.length(), startLeft, mFirstBaseLine, mPaint);
            mPaint.setColor(tempColor);
        }

        QMUIQQFaceCompiler2.Element element;
        for (int i = 0; i < elements.size(); i++) {
            element = elements.get(i);
            QMUIQQFaceCompiler2.ElementType type = element.getType();
//            LogUtil.d(type + "===================" + element.getText());
            if (type == QMUIQQFaceCompiler2.ElementType.DRAWABLE) {
                onDrawQQFace(canvas, element.getDrawableRes(), null, startLeft, endWidth, i == 0, i == elements.size() - 1);
            } else if (type == QMUIQQFaceCompiler2.ElementType.SPECIAL_BOUNDS_DRAWABLE) {
                onDrawQQFace(canvas, 0, element.getSpecialBoundsDrawable(), startLeft, endWidth, i == 0, i == elements.size() - 1);
            } else if (type == QMUIQQFaceCompiler2.ElementType.TEXT) {
                CharSequence text = element.getText();
                Log.d(TAG, "drawElements: " + text);
                float[] fontWidths = new float[text.length()];
                mPaint.getTextWidths(text.toString(), fontWidths);
                onDrawText(canvas, text, fontWidths, 0, startLeft, endWidth);
            } else if (type == QMUIQQFaceCompiler2.ElementType.SPAN) {
                QMUIQQFaceCompiler2.ElementList spanElementList = element.getChildList();
                mCurrentDrawSpan = element.getTouchableSpan();
                if (spanElementList != null && !spanElementList.getElements().isEmpty()) {
                    if (mCurrentDrawSpan == null) {
                        drawElements(canvas, spanElementList.getElements(), usefulWidth);
                        continue;
                    }
                    mIsInDrawSpan = true;
                    @ColorInt int spanColor = mCurrentDrawSpan.isPressed() ?
                            mCurrentDrawSpan.getPressedTextColor() :
                            mCurrentDrawSpan.getNormalTextColor();
                    mPaint.setColor(spanColor == 0 ? mTextColor : spanColor);
                    drawElements(canvas, spanElementList.getElements(), usefulWidth);
                    mPaint.setColor(mTextColor);
                    mIsInDrawSpan = false;
                }
            } else if (type == QMUIQQFaceCompiler2.ElementType.NEXTLINE) {
                int ellipsizeLength = mEllipsizeTextLength + mMoreActionTextLength;
                if (mIsNeedEllipsize && mEllipsize == TextUtils.TruncateAt.END &&
                        mCurrentDrawUsedWidth <= endWidth - ellipsizeLength && mCurrentDrawLine == mNeedDrawLine) {
                    drawText(canvas, mEllipsizeText, 0, mEllipsizeText.length(), mEllipsizeTextLength);
                    mCurrentDrawUsedWidth += mEllipsizeTextLength;
                    drawMoreActionText(canvas, endWidth);
                    return;
                }
                toNewDrawLine(startLeft, true, usefulWidth);
            }
        }
    }

    private void drawMoreActionText(Canvas canvas, int widthEnd) {
        if (!QMUILangHelper.isNullOrEmpty(mMoreActionText)) {
            mPaint.setColor(mMoreActionColor);
            int top = getPaddingTop();
            if (mCurrentDrawLine > 1) {
                top = (mCurrentDrawLine - 1) * (mFontHeight + mLineSpace) + top;
            }
            mMoreHitRect.set(mCurrentDrawUsedWidth, top, widthEnd, top + mFontHeight);
            canvas.drawText(mMoreActionText, 0, mMoreActionText.length(), mCurrentDrawUsedWidth, mCurrentDrawBaseLine, mPaint);
            mPaint.setColor(mTextColor);
        }
    }

    private void toNewDrawLine(int startLeft, int usefulWidth) {
        toNewDrawLine(startLeft, false, usefulWidth);
    }

    /**
     * 控制段落切换
     */
    private void toNewDrawLine(int startLeft, boolean paragraph, int usefulWidth) {
        int addOn = (paragraph ? mParagraphSpace : 0) + mLineSpace;
        mCurrentDrawLine++;
        if (mIsNeedEllipsize) {
            if (mEllipsize == TextUtils.TruncateAt.START) {
                if (mCurrentDrawLine > mLines - mNeedDrawLine + 1) {
                    mCurrentDrawBaseLine += mFontHeight + addOn;
                }
            } else if (mEllipsize == TextUtils.TruncateAt.MIDDLE) {
                if (!mIsExecutedMiddleEllipsize || mMiddleEllipsizeWidthRecord == -1) {
                    mCurrentDrawBaseLine += mFontHeight + addOn;
                }
            } else {
                mCurrentDrawBaseLine += mFontHeight + addOn;
            }
            if (mEllipsize != TextUtils.TruncateAt.END && mCurrentDrawBaseLine > getHeight() - getPaddingBottom()) {
                QMUILog.d(TAG, "draw outside the visible height, the ellipsize is inaccurate: " +
                                "mEllipsize = %s; mCurrentDrawLine = %d; mNeedDrawLine = %d;" +
                                "viewWidth = %d; viewHeight = %d; paddingLeft = %d; " +
                                "paddingRight = %d; paddingTop = %d; paddingBottom = %d; text = %s",
                        mEllipsize.name(), mCurrentDrawLine, mNeedDrawLine,
                        getWidth(), getHeight(), getPaddingLeft(), getPaddingRight(),
                        getPaddingTop(), getPaddingBottom(), mOriginText);
            }
        } else {
            mCurrentDrawBaseLine += mFontHeight + addOn;
        }
        setStartDrawUsedWidth(startLeft, usefulWidth);
    }

    private void setStartDrawUsedWidth(int startLeft, int usefulWidth) {
        if (mIsNeedEllipsize) {
            mCurrentDrawUsedWidth = startLeft;
            return;
        }
        if (mCurrentDrawLine == mNeedDrawLine) {
            if (mGravity == Gravity.CENTER) {
                mCurrentDrawUsedWidth = (usefulWidth - (mCurrentCalWidth - startLeft)) / 2 + startLeft;
            } else if (mGravity == Gravity.RIGHT) {
                mCurrentDrawUsedWidth = (usefulWidth - (mCurrentCalWidth - startLeft)) + startLeft;
            } else {
                mCurrentDrawUsedWidth = startLeft;
            }
        } else {
            mCurrentDrawUsedWidth = startLeft;
        }
    }

    private void onRealDrawText(Canvas canvas, CharSequence text, float[] fontWidths, int offset, int widthStart, int widthEnd) {
        int startPos = offset;
        int targetUsedWidth = mCurrentDrawUsedWidth;
        for (int i = offset; i < fontWidths.length; i++) {
            Log.d(TAG, "onRealDrawText: fontWidths" + fontWidths[i] + " startPos +" + startPos);
            if (targetUsedWidth + fontWidths[i] > widthEnd) {
                drawText(canvas, text, startPos, i, widthEnd - mCurrentDrawUsedWidth);
                toNewDrawLine(widthStart, widthEnd - widthStart);
                targetUsedWidth = mCurrentDrawUsedWidth;
                startPos = i;
            }
            targetUsedWidth += fontWidths[i];
        }


        if (startPos < fontWidths.length) {
            drawText(canvas, text, startPos, fontWidths.length, targetUsedWidth - mCurrentDrawUsedWidth);
            mCurrentDrawUsedWidth = targetUsedWidth;
        }
    }

    private int getMiddleEllipsizeLine() {
        int ellipsizeLine;
        if (mNeedDrawLine % 2 == 0) {//双数取平均值下一行
            ellipsizeLine = mNeedDrawLine / 2 + 1;
        } else {//单数情况下,以平均值
            ellipsizeLine = (mNeedDrawLine + 1) / 2;
        }
        return ellipsizeLine;
    }


    private int mMiddleEllipsizeWidthRecord = -1;
    private boolean mIsExecutedMiddleEllipsize = false;

    private void onDrawText(Canvas canvas, CharSequence text, float[] fontWidths, int offset, int widthStart, int widthEnd) {
        if (offset >= text.length()) {
            return;
        }
        if (mIsNeedEllipsize) {
            if (mEllipsize == TextUtils.TruncateAt.START) {
                if (mCurrentDrawLine > mLines - mNeedDrawLine) {
                    onRealDrawText(canvas, text, fontWidths, offset, widthStart, widthEnd);
                } else if (mCurrentDrawLine < mLines - mNeedDrawLine) {
                    for (int i = offset; i < text.length(); i++) {
                        if (mCurrentDrawUsedWidth + fontWidths[i] <= widthEnd) {
                            mCurrentDrawUsedWidth += fontWidths[i];
                        } else {
                            toNewDrawLine(widthStart, widthEnd - widthStart);
                            onDrawText(canvas, text, fontWidths, i, widthStart, widthEnd);
                            return;
                        }
                    }
                } else {
                    int needStopWidth = mCurrentCalWidth + mEllipsizeTextLength;
                    for (int i = offset; i < text.length(); i++) {
                        if (mCurrentDrawUsedWidth + fontWidths[i] <= needStopWidth) {
                            mCurrentDrawUsedWidth += fontWidths[i];
                        } else {
                            int newStart = i + 1;
                            if (mCurrentDrawUsedWidth > needStopWidth) {
                                newStart = i;
                            }
                            toNewDrawLine(widthStart + mEllipsizeTextLength, widthEnd - widthStart);
                            onDrawText(canvas, text, fontWidths, newStart, widthStart, widthEnd);
                            return;
                        }
                    }
                }
            } else if (mEllipsize == TextUtils.TruncateAt.MIDDLE) {
                int ellipsizeLine = getMiddleEllipsizeLine();
                if (mCurrentDrawLine < ellipsizeLine) {
                    int targetDrawWidth = mCurrentDrawUsedWidth;
                    for (int i = offset; i < fontWidths.length; i++) {
                        if (targetDrawWidth + fontWidths[i] <= widthEnd) {
                            targetDrawWidth += fontWidths[i];
                        } else {
                            drawText(canvas, text, offset, i, widthEnd - mCurrentDrawUsedWidth);
                            toNewDrawLine(widthStart, widthEnd - widthStart);
                            onDrawText(canvas, text, fontWidths, i, widthStart, widthEnd);
                            return;
                        }
                    }
                    drawText(canvas, text, offset, text.length(), targetDrawWidth - mCurrentDrawUsedWidth);
                    mCurrentDrawUsedWidth = targetDrawWidth;
                } else if (mCurrentDrawLine == ellipsizeLine) {
                    if (mIsExecutedMiddleEllipsize) {
                        handleTextAfterMiddleEllipsize(canvas, text, fontWidths, offset,
                                ellipsizeLine, widthStart, widthEnd);
                    } else {
                        int needStop = (widthEnd + widthStart) / 2 - mEllipsizeTextLength / 2;
                        int targetDrawWidth = mCurrentDrawUsedWidth;
                        for (int i = offset; i < fontWidths.length; i++) {
                            if (targetDrawWidth + fontWidths[i] <= needStop) {
                                targetDrawWidth += fontWidths[i];
                            } else {
                                drawText(canvas, text, offset, i, targetDrawWidth - mCurrentDrawUsedWidth);
                                mCurrentDrawUsedWidth = targetDrawWidth;
                                drawText(canvas, mEllipsizeText, 0, mEllipsizeText.length(), mEllipsizeTextLength);
                                mMiddleEllipsizeWidthRecord = mCurrentDrawUsedWidth + mEllipsizeTextLength;
                                mIsExecutedMiddleEllipsize = true;
                                handleTextAfterMiddleEllipsize(canvas, text, fontWidths, i,
                                        ellipsizeLine, widthStart, widthEnd);
                                return;
                            }
                        }
                        drawText(canvas, text, offset, text.length(), targetDrawWidth - mCurrentDrawUsedWidth);
                        mCurrentDrawUsedWidth = targetDrawWidth;
                    }
                } else {
                    handleTextAfterMiddleEllipsize(canvas, text, fontWidths, offset,
                            ellipsizeLine, widthStart, widthEnd);
                }
            } else {
                if (mCurrentDrawLine < mNeedDrawLine) {
                    int targetUsedWidth = mCurrentDrawUsedWidth;
                    for (int i = offset; i < fontWidths.length; i++) {
                        if (targetUsedWidth + fontWidths[i] <= widthEnd) {
                            targetUsedWidth += fontWidths[i];
                        } else {
                            drawText(canvas, text, offset, i, widthEnd - mCurrentDrawUsedWidth);
                            toNewDrawLine(widthStart, widthEnd - widthStart);
                            onDrawText(canvas, text, fontWidths, i, widthStart, widthEnd);
                            return;
                        }
                    }
                    drawText(canvas, text, offset, fontWidths.length, targetUsedWidth - mCurrentDrawUsedWidth);
                    mCurrentDrawUsedWidth = targetUsedWidth;
                } else if (mCurrentDrawLine == mNeedDrawLine) {
                    int ellipsizeLength = mEllipsizeTextLength + mMoreActionTextLength;
                    int targetUsedWidth = mCurrentDrawUsedWidth;
                    for (int i = offset; i < fontWidths.length; i++) {
                        if (targetUsedWidth + fontWidths[i] <= widthEnd - ellipsizeLength) {
                            targetUsedWidth += fontWidths[i];
                        } else {
                            drawText(canvas, text, offset, i, targetUsedWidth - mCurrentDrawUsedWidth);
                            mCurrentDrawUsedWidth = targetUsedWidth;
                            drawText(canvas, mEllipsizeText, 0, mEllipsizeText.length(), mEllipsizeTextLength);
                            mCurrentDrawUsedWidth += mEllipsizeTextLength;
                            drawMoreActionText(canvas, widthEnd);
                            // 依然要去到下一行，使得后续不会进入这个逻辑
                            toNewDrawLine(widthStart, widthEnd - widthStart);
                            return;
                        }
                    }
                    drawText(canvas, text, offset, fontWidths.length, targetUsedWidth - mCurrentDrawUsedWidth);
                    mCurrentDrawUsedWidth = targetUsedWidth;
                }
            }

        } else {
            onRealDrawText(canvas, text, fontWidths, 0, widthStart, widthEnd);
        }
    }

    private void handleTextAfterMiddleEllipsize(Canvas canvas, CharSequence text, float[] fontWidths,
                                                int offset, int ellipsizeLine, int widthStart, int widthEnd) {
        if (offset >= text.length()) {
            return;
        }
        if (mMiddleEllipsizeWidthRecord == -1) {
            onRealDrawText(canvas, text, fontWidths, offset, widthStart, widthEnd);
            return;
        }
        int endLines = mNeedDrawLine - ellipsizeLine;
        int borrowWidth = widthEnd - mCurrentCalWidth - (mMiddleEllipsizeWidthRecord - widthStart);
        int needStopLine = borrowWidth > 0 ? mLines - endLines - 1 : mLines - endLines;
        int needStopWidth = borrowWidth > 0 ? widthEnd - borrowWidth :
                mMiddleEllipsizeWidthRecord - (widthEnd - mCurrentCalWidth);


        if (mCurrentDrawLine < needStopLine) {
            for (int i = offset; i < fontWidths.length; i++) {
                if (mCurrentDrawUsedWidth + fontWidths[i] <= widthEnd) {
                    mCurrentDrawUsedWidth += fontWidths[i];
                } else {
                    toNewDrawLine(widthStart, widthStart - widthEnd);
                    handleTextAfterMiddleEllipsize(canvas, text, fontWidths, i, ellipsizeLine, widthStart, widthEnd);
                    return;
                }
            }
        } else if (mCurrentDrawLine == needStopLine) {
            for (int i = offset; i < fontWidths.length; i++) {
                if (mCurrentDrawUsedWidth + fontWidths[i] <= needStopWidth) {
                    mCurrentDrawUsedWidth += fontWidths[i];
                } else {
                    int newStart = i + 1;
                    if (mCurrentDrawUsedWidth >= needStopWidth) {
                        newStart = i;
                    }
                    mCurrentDrawUsedWidth = mMiddleEllipsizeWidthRecord;
                    mMiddleEllipsizeWidthRecord = -1;
                    mLastNeedStopLineRecord = needStopLine;
                    onRealDrawText(canvas, text, fontWidths, newStart, widthStart, widthEnd);
                    return;
                }
            }
        } else {
            onRealDrawText(canvas, text, fontWidths, offset, widthStart, widthEnd);
        }
    }

    private void drawText(Canvas canvas, CharSequence text, int start, int end, int textWidth) {
        if (end <= start || end > text.length() || start >= text.length()) {
            return;
        }
        if (mIsInDrawSpan && mCurrentDrawSpan != null) {
            @ColorInt int color = mCurrentDrawSpan.isPressed() ? mCurrentDrawSpan.getPressedBackgroundColor() :
                    mCurrentDrawSpan.getNormalBackgroundColor();
            if (color != Color.TRANSPARENT) {
                mSpanBgPaint.setColor(color);
                canvas.drawRect(mCurrentDrawUsedWidth, mCurrentDrawBaseLine - mFirstBaseLine,
                        mCurrentDrawUsedWidth + textWidth,
                        mCurrentDrawBaseLine - mFirstBaseLine + mFontHeight, mSpanBgPaint);
            }
        }
        if (selectableHelper!=null&&selectableHelper.isSelecting()) {
            mSelectionPaint.setColor(selectableHelper.selectionColor);
            float[] tempWidth = new float[text.length()];
            mPaint.getTextWidths(text, start, end, tempWidth);
            float tempCalWidth = 0;
            for (float v : tempWidth) {
                tempCalWidth += v;
            }
            tempCalWidth += mCurrentDrawUsedWidth; //后来指定Right
            if (mCurrentDrawLine == selectableHelper.mSlectionStart.line && mCurrentDrawLine == selectableHelper.mSlectionEnd.line) {
                //选择的在同一行的情况
                int[] drawRect = selectableHelper.qujiaoji(mCurrentDrawUsedWidth, (int) tempCalWidth, selectableHelper.mSlectionStart.startX, selectableHelper.mSlectionEnd.endX);
                if (drawRect != null) {
                    canvas.drawRect(drawRect[0], mCurrentDrawBaseLine - mFirstBaseLine,
                            drawRect[1],
                            mCurrentDrawBaseLine - mFirstBaseLine + mFontHeight, mSelectionPaint);
                }
            } else if (mCurrentDrawLine == selectableHelper.mSlectionStart.line) {
                //起点游标单独在首行
                int left = Math.max(mCurrentDrawUsedWidth, selectableHelper.mSlectionStart.startX);
                canvas.drawRect(left, mCurrentDrawBaseLine - mFirstBaseLine,
                        mCurrentDrawUsedWidth + textWidth,
                        mCurrentDrawBaseLine - mFirstBaseLine + mFontHeight, mSelectionPaint);
            } else if (mCurrentDrawLine == selectableHelper.mSlectionEnd.line) {
                //终点游标单独在尾行
                int right = Math.min(mCurrentDrawUsedWidth + textWidth, selectableHelper.mSlectionEnd.endX);
                canvas.drawRect(mCurrentDrawUsedWidth, mCurrentDrawBaseLine - mFirstBaseLine,
                        right,
                        mCurrentDrawBaseLine - mFirstBaseLine + mFontHeight, mSelectionPaint);
            } else if (mCurrentDrawLine > selectableHelper.mSlectionStart.line && mCurrentDrawLine < selectableHelper.mSlectionEnd.line) {
                //中间行全绘制
                canvas.drawRect(mCurrentDrawUsedWidth, mCurrentDrawBaseLine - mFirstBaseLine,
                        mCurrentDrawUsedWidth + textWidth,
                        mCurrentDrawBaseLine - mFirstBaseLine + mFontHeight, mSelectionPaint);

            }


        }
        Log.d(TAG, "drawText(" + text + "): start = " + start + " end = " + end + " width = " + mCurrentDrawUsedWidth + " y = " + mCurrentDrawBaseLine);
        canvas.drawText(text, start, end, mCurrentDrawUsedWidth, mCurrentDrawBaseLine, mPaint);
    }

    private void onDrawQQFace(Canvas canvas, int res, @Nullable Drawable specialDrawable, int widthStart, int widthEnd, boolean isFirst, boolean isLast) {
        int size = res != -1 || specialDrawable == null ? mQQFaceSize : specialDrawable.getIntrinsicWidth() + (isFirst || isLast ? mSpecialDrawablePadding : mSpecialDrawablePadding * 2);
        if (mIsNeedEllipsize) {
            if (mEllipsize == TextUtils.TruncateAt.START) {
                if (mCurrentDrawLine > mLines - mNeedDrawLine) {
                    onRealDrawQQFace(canvas, res, specialDrawable, mNeedDrawLine - mLines, widthStart, widthEnd, isFirst, isLast);
                } else if (mCurrentDrawLine < mLines - mNeedDrawLine) {
                    if (size + mCurrentDrawUsedWidth > widthEnd) {
                        toNewDrawLine(widthStart, widthEnd - widthStart);
                        onDrawQQFace(canvas, res, specialDrawable, widthStart, widthEnd, isFirst, isLast);
                    } else {
                        mCurrentDrawUsedWidth += size;
                    }
                } else {
                    int needStopWidth = mCurrentCalWidth + mEllipsizeTextLength;
                    if (size + mCurrentDrawUsedWidth < needStopWidth) {
                        mCurrentDrawUsedWidth += size;
                    } else {
                        toNewDrawLine(widthStart + mEllipsizeTextLength, widthEnd - widthStart);
                    }
                }
            } else if (mEllipsize == TextUtils.TruncateAt.MIDDLE) {
                int ellipsizeLine = getMiddleEllipsizeLine();
                if (mCurrentDrawLine < ellipsizeLine) {
                    if (size + mCurrentDrawUsedWidth > widthEnd) {
                        onRealDrawQQFace(canvas, res, specialDrawable, 0, widthStart, widthEnd, isFirst, isLast);
                    } else {
                        drawQQFace(canvas, res, specialDrawable, mCurrentDrawLine, isFirst, isLast);
                        mCurrentDrawUsedWidth += size;
                    }
                } else if (mCurrentDrawLine == ellipsizeLine) {
                    int needStop = getWidth() / 2 - mEllipsizeTextLength / 2;
                    if (mIsExecutedMiddleEllipsize) {
                        handleQQFaceAfterMiddleEllipsize(canvas, res, specialDrawable, widthStart, widthEnd, ellipsizeLine, isFirst, isLast);
                    } else if (size + mCurrentDrawUsedWidth <= needStop) {
                        drawQQFace(canvas, res, specialDrawable, mCurrentDrawLine, isFirst, isLast);
                        mCurrentDrawUsedWidth += size;
                    } else {
                        drawText(canvas, mEllipsizeText, 0, mEllipsizeText.length(), mEllipsizeTextLength);
                        mMiddleEllipsizeWidthRecord = mCurrentDrawUsedWidth + mEllipsizeTextLength;
                        mIsExecutedMiddleEllipsize = true;
                        handleQQFaceAfterMiddleEllipsize(canvas, res, specialDrawable, widthStart, widthEnd, ellipsizeLine, isFirst, isLast);
                    }
                } else {
                    handleQQFaceAfterMiddleEllipsize(canvas, res, specialDrawable, widthStart, widthEnd, ellipsizeLine, isFirst, isLast);
                }
            } else {
                if (mCurrentDrawLine == mNeedDrawLine) {
                    int ellipsizeLength = mEllipsizeTextLength + mMoreActionTextLength;
                    if (size + mCurrentDrawUsedWidth >= widthEnd - ellipsizeLength) {
                        if (size + mCurrentDrawUsedWidth == widthEnd - ellipsizeLength) {
                            drawQQFace(canvas, res, specialDrawable, mCurrentDrawLine, isFirst, isLast);
                            mCurrentDrawUsedWidth += size;
                        }
                        drawText(canvas, mEllipsizeText, 0, mEllipsizeText.length(), mEllipsizeTextLength);
                        mCurrentDrawUsedWidth += mEllipsizeTextLength;
                        drawMoreActionText(canvas, widthEnd);
                        // 去新的一行，避免再次走入这一行的逻辑
                        toNewDrawLine(widthStart, widthEnd - widthStart);
                    } else {
                        drawQQFace(canvas, res, specialDrawable, mCurrentDrawLine, isFirst, isLast);
                        mCurrentDrawUsedWidth += size;
                    }
                } else if (mCurrentDrawLine < mNeedDrawLine) {
                    if (size + mCurrentDrawUsedWidth > widthEnd) {
                        onRealDrawQQFace(canvas, res, specialDrawable, 0, widthStart, widthEnd, isFirst, isLast);
                    } else {
                        drawQQFace(canvas, res, specialDrawable, mCurrentDrawLine, isFirst, isLast);
                        mCurrentDrawUsedWidth += size;
                    }
                }
            }

        } else {
            onRealDrawQQFace(canvas, res, specialDrawable, 0, widthStart, widthEnd, isFirst, isLast);
        }
    }

    private int mLastNeedStopLineRecord = -1;

    private void handleQQFaceAfterMiddleEllipsize(Canvas canvas, int res, Drawable specialDrawable, int widthStart,
                                                  int widthEnd, int ellipsizeLine, boolean isFirst, boolean isLast) {
        int size = res != 0 ? mQQFaceSize : specialDrawable.getIntrinsicWidth() + (isFirst || isLast ? mSpecialDrawablePadding : mSpecialDrawablePadding * 2);
        if (mMiddleEllipsizeWidthRecord == -1) {
            onRealDrawQQFace(canvas, res, specialDrawable, ellipsizeLine - mLastNeedStopLineRecord, widthStart, widthEnd, isFirst, isLast);
            return;
        }

        int endLines = mNeedDrawLine - ellipsizeLine;
        int borrowWidth = widthEnd - mCurrentCalWidth - (mMiddleEllipsizeWidthRecord - widthStart);
        int needStopLine = borrowWidth > 0 ? mLines - endLines - 1 : mLines - endLines;
        int needStopWidth = borrowWidth > 0 ? widthEnd - borrowWidth :
                mMiddleEllipsizeWidthRecord - (widthEnd - mCurrentCalWidth);

        if (mCurrentDrawLine < needStopLine) {
            if (size + mCurrentDrawUsedWidth > widthEnd) {
                toNewDrawLine(widthStart, widthEnd - widthStart);
                onDrawQQFace(canvas, res, specialDrawable, widthStart, widthEnd, isFirst, isLast);
            } else {
                mCurrentDrawUsedWidth += size;
            }
        } else if (mCurrentDrawLine == needStopLine) {
            if (size + mCurrentDrawUsedWidth <= needStopWidth) {
                mCurrentDrawUsedWidth += size;
            } else {
                boolean drawCurrentFace = false;
                if (mCurrentDrawUsedWidth >= needStopWidth) {
                    drawCurrentFace = true;
                }
                mCurrentDrawUsedWidth = mMiddleEllipsizeWidthRecord;
                mMiddleEllipsizeWidthRecord = -1;
                mLastNeedStopLineRecord = needStopLine;
                if (drawCurrentFace) {
                    onDrawQQFace(canvas, res, specialDrawable, widthStart, widthEnd, isFirst, isLast);
                }
            }
        } else {
            onRealDrawQQFace(canvas, res, specialDrawable, ellipsizeLine - needStopLine, widthStart, widthEnd, isFirst, isLast);
        }
    }

    private void onRealDrawQQFace(Canvas canvas, int res, @Nullable Drawable specialDrawable, int adjustLine,
                                  int widthStart, int widthEnd, boolean isFirst, boolean isLast) {
        int size = res != 0 || specialDrawable == null ? mQQFaceSize : specialDrawable.getIntrinsicWidth() + (isFirst || isLast ? mSpecialDrawablePadding : mSpecialDrawablePadding * 2);
        if (mCurrentDrawUsedWidth + size > widthEnd) {
            toNewDrawLine(widthStart, widthEnd - widthStart);
        }
        drawQQFace(canvas, res, specialDrawable, mCurrentDrawLine + adjustLine, isFirst, isLast);
        mCurrentDrawUsedWidth += size;
    }

    private void drawQQFace(Canvas canvas, int res, @Nullable Drawable specialDrawable, int line, boolean isFirst, boolean isLast) {
        Drawable drawable = res != 0 ? ContextCompat.getDrawable(getContext(), res) : specialDrawable;
        int size = res != 0 || specialDrawable == null ? mQQFaceSize : specialDrawable.getIntrinsicWidth() + (isFirst || isLast ? mSpecialDrawablePadding : mSpecialDrawablePadding * 2);
        if (drawable == null) {
            return;
        }
        int drawableTop;
        if (res != 0) {
            drawableTop = (mFontHeight - mQQFaceSize) / 2;
            drawable.setBounds(0, drawableTop, mQQFaceSize, drawableTop + mQQFaceSize);
        } else {
            drawableTop = (mFontHeight - drawable.getIntrinsicHeight()) / 2;
            int left = isLast ? mSpecialDrawablePadding : 0;
            drawable.setBounds(left, drawableTop, left + drawable.getIntrinsicWidth(), drawableTop + drawable.getIntrinsicHeight());
        }
        int top = getPaddingTop();
        if (line > 1) {
            top = (line - 1) * (mFontHeight + mLineSpace) + top;
        }
        canvas.save();
        canvas.translate(mCurrentDrawUsedWidth, top);
        if (mIsInDrawSpan && mCurrentDrawSpan != null) {
            @ColorInt int color = mCurrentDrawSpan.isPressed() ? mCurrentDrawSpan.getPressedBackgroundColor() :
                    mCurrentDrawSpan.getNormalBackgroundColor();
            if (color != Color.TRANSPARENT) {
                mSpanBgPaint.setColor(color);
                canvas.drawRect(0, 0, size, mFontHeight, mSpanBgPaint);
            }
        }
        if (selectableHelper!=null&&selectableHelper.isSelecting()) {
            if (line == selectableHelper.mSlectionStart.line && line == selectableHelper.mSlectionEnd.line) {
                if (mCurrentDrawUsedWidth >= selectableHelper.mSlectionStart.startX && mCurrentDrawUsedWidth <= selectableHelper.mSlectionEnd.endX){
                    mSelectionPaint.setColor(selectableHelper.selectionColor);
                    canvas.drawRect(0, 0, size, mFontHeight, mSelectionPaint);
                }
            } else if (line== selectableHelper.mSlectionStart.line) {
                if (mCurrentDrawUsedWidth >= selectableHelper.mSlectionStart.startX){
                    mSelectionPaint.setColor(selectableHelper.selectionColor);
                    canvas.drawRect(0, 0, size, mFontHeight, mSelectionPaint);
                }
            }else if (line == selectableHelper.mSlectionEnd.line){
                if (mCurrentDrawUsedWidth < selectableHelper.mSlectionEnd.endX){
                    mSelectionPaint.setColor(selectableHelper.selectionColor);
                    canvas.drawRect(0, 0, size, mFontHeight, mSelectionPaint);
                }
            }else if (line > selectableHelper.mSlectionStart.line && line < selectableHelper.mSlectionEnd.line){
                mSelectionPaint.setColor(selectableHelper.selectionColor);
                canvas.drawRect(0, 0, size, mFontHeight, mSelectionPaint);
            }
        }
//        (line < selectableHelper.mSlectionEnd.line || selectableHelper.isInSelectArea(line, mCurrentDrawUsedWidth))) {
//            mSelectionPaint.setColor(selectableHelper.selectionColor);
//            canvas.drawRect(0, 0, size, mFontHeight, mSelectionPaint);
//        }
        drawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (selectableHelper != null) selectableHelper.performLongClickSelect(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    private class SpanInfo {
        private ITouchableSpan mTouchableSpan;
        private int mStartPoint;
        private int mEndPoint;
        private int mStartLine;
        private int mEndLine;

        public SpanInfo(ITouchableSpan touchableSpan) {
            mTouchableSpan = touchableSpan;
        }

        public void setStart(int startLine, int startPoint) {
            mStartLine = startLine;
            mStartPoint = startPoint;
        }

        public void setPressed(boolean pressed) {
            mTouchableSpan.setPressed(pressed);
        }

        public void setEnd(int endLine, int endPoint) {
            mEndLine = endLine;
            mEndPoint = endPoint;
        }

        public void onClick() {
            mTouchableSpan.onClick(QMUIQQFaceView2.this);
        }

        public void invalidateSpan() {
            int top = getPaddingTop();
            if (mStartLine > 1) {
                top = (mStartLine - 1) * (mFontHeight + mLineSpace) + top;
            }

            int bottom = (mEndLine - 1) * (mFontHeight + mLineSpace) + top + mFontHeight;
            Rect bounds = new Rect();
            bounds.top = top;
            bounds.bottom = bottom;
            bounds.left = getPaddingLeft();
            bounds.right = getWidth() - getPaddingRight();
            if (mStartLine == mEndLine) {
                bounds.left = mStartPoint;
                bounds.right = mEndPoint;
            }
            invalidate(bounds);
        }

        @SuppressWarnings("SimplifiableIfStatement")
        public boolean onTouch(int x, int y) {
            int top = getPaddingTop();
            if (mStartLine > 1) {
                top = (mStartLine - 1) * (mFontHeight + mLineSpace) + top;
            }

            int bottom = (mEndLine - 1) * (mFontHeight + mLineSpace) + getPaddingTop() + mFontHeight;

            if (y < top || y > bottom) {
                return false;
            }

            if (mStartLine == mEndLine) {
                return x >= mStartPoint && x <= mEndPoint;
            }

            int startLineBottom = top + mFontHeight;
            int endLineTop = bottom - mFontHeight;
            if (y > startLineBottom && y < endLineTop) {
                //noinspection SimplifiableIfStatement
                if (mEndLine - mStartLine == 1) {
                    return x >= mStartPoint && x <= mEndPoint;
                }
                return true;
            } else if (y <= startLineBottom) {
                return x >= mStartPoint;
            } else {
                return x <= mEndPoint;
            }

        }
    }

    public static class PressCancelAction implements Runnable {
        private WeakReference<SpanInfo> mWeakReference;

        public PressCancelAction(SpanInfo spanInfo) {
            mWeakReference = new WeakReference<>(spanInfo);
        }

        @Override
        public void run() {
            SpanInfo spanInfo = mWeakReference.get();
            if (spanInfo != null) {
                spanInfo.setPressed(false);
                spanInfo.invalidateSpan();
            }
        }
    }

    public interface QQFaceViewListener {
        void onCalculateLinesChange(int lines);

        void onMoreTextClick();
    }


// <editor-fold defaultstate="collapsed" desc="加入文本支持选中">

    private SelectableHelper selectableHelper;

    public void createIfNeed() {
        if (this.selectableHelper == null) {
            this.selectableHelper = new SelectableHelper(this);
        }
    }

    public void setSelectable(boolean selectable) {
        if (selectable) {
            createIfNeed();
            if (this.selectableHelper != null) {
                this.selectableHelper.turnOnSelectable();
                this.selectableHelper.setSelectable(true);
            }
        } else {
            if (this.selectableHelper != null) {
                this.selectableHelper.turnOffSelectable();
                this.selectableHelper.setSelectable(true);
            }
        }
    }

    /**
     * @param onTextSelectListener
     * @link setSelectable(true) 必须先执行
     */
    public void setOnTextSelectListener(SelectableHelper.OnTextSelectListener onTextSelectListener) {
        if (selectableHelper != null)
            selectableHelper.setOnTextSelectListener(onTextSelectListener);
    }


    public void turnOffSelectable() {
        if (this.selectableHelper != null) {
            this.selectableHelper.turnOffSelectable();
        }
    }

    public int getLineBottom(int line) {
        if (line > 0) {
            return (line - 1) * mLineSpace + line * mFontHeight;
        } else {
            return 0;
        }
    }


    /**
     * 根据传入的垂直坐标值(必须为getY()不是getRawY())返回第几行(仅限wrap_content行数全部展示的情况下)
     *
     * @param y
     * @return
     */
    public int getLineForVertical(int y) {
        int belongLine = 1;
        if (mLines >= 2) {
            if (y <= getPaddingTop()) {
                belongLine = 1;
            } else if (y >= getHeight() - getPaddingBottom()) {
                belongLine = mLines;
            } else {
                int inTextAreaY = y - getPaddingTop();
                belongLine = inTextAreaY / (mFontHeight + mLineSpace) + 1;
            }
        }
        return belongLine;
    }

    public int getOffsetForHorizontal(int line, int x) {
        int offset = 0;
        List<DrawElementIndex> drawElementIndexList = mElementIndexInLine.get(line);
        if (drawElementIndexList != null) {
            DrawElementIndex resultElement = null;
            for (DrawElementIndex drawElmentIndex : drawElementIndexList) {
                if (drawElmentIndex.inRect(x)) resultElement = drawElmentIndex;
            }//遍历一行中所有的元素X坐标
            if (resultElement == null) { //如果找不到则表示当前触摸X坐标处于首字符坐标或者尾字符右边
                if (x >= getPaddingLeft()) {//首字符左则依然offset是0，因为该行所有字符都找不到且又不在左边，则只能有尾字符右，取尾字符
                    if (!drawElementIndexList.isEmpty()) {
                        DrawElementIndex temp = drawElementIndexList.get(drawElementIndexList.size() - 1);
                        offset = temp.index;
                    }
                }
            } else {
                if (!drawElementIndexList.isEmpty()) {
                    DrawElementIndex temp = drawElementIndexList.get(0);
                    offset = temp.index;
                }
            }
        }
        return offset;
    }


    /**
     * 根据行和x坐标得到字符绘制数据
     *
     * @param line
     * @param x
     * @return
     */
    public DrawElementIndex getElementIndexForHorizontal(int line, int x) {
        DrawElementIndex selectElement = null;
        int tempLine = line;
        if (line == mLines&&!TextUtils.isEmpty(getText())){
            if (getText().charAt(getText().length()-1) == '\n'){
                tempLine = mLines-1;
            }
        }
        List<DrawElementIndex> drawElementIndexList = mElementIndexInLine.get(tempLine);
        if (drawElementIndexList != null) {
            for (DrawElementIndex drawElmentIndex : drawElementIndexList) {
                if (drawElmentIndex.inRect(x)) selectElement = drawElmentIndex;
            }//遍历一行中所有的元素X坐标
            if (selectElement == null) { //如果找不到则表示当前触摸X坐标处于首字符坐标或者尾字符右边
                if (x >= getPaddingLeft()) {//首字符左则依然offset是0，因为该行所有字符都找不到且又不在左边，则只能有尾字符右，取尾字符
                    if (!drawElementIndexList.isEmpty()) {
                        DrawElementIndex temp = drawElementIndexList.get(drawElementIndexList.size() - 1);
                        selectElement = temp;
                    }
                } else {
                    if (!drawElementIndexList.isEmpty()) {
                        DrawElementIndex temp = drawElementIndexList.get(0);
                        selectElement = temp;
                    }
                }
            }
        }
        return selectElement;
    }


    private int drawElmentIndex = 0;

    private void putElementIndexOfLine(int line, int startX, int endX, boolean isDrawable) {
        Log.d(TAG, String.format("putElementIndexOfLine: line :%s  , startX :%s, endX:%s ", line, startX, endX));
        List<DrawElementIndex> drawElementIndexList = mElementIndexInLine.get(line);
        if (drawElementIndexList == null) {
            drawElementIndexList = new ArrayList<>();
        }
        drawElementIndexList.add(new DrawElementIndex(line, drawElmentIndex, startX, endX, isDrawable ? 2 : 1));
        mElementIndexInLine.put(line, drawElementIndexList);
        if (!isDrawable)
            drawElmentIndex++;
        else
            drawElmentIndex += 2;
    }


    public DrawElementIndex getFirstElementIndex() {
        List<DrawElementIndex> drawElementIndexList = mElementIndexInLine.get(1);
        if (drawElementIndexList != null && !drawElementIndexList.isEmpty()) {
            Log.d(TAG, "getFirstElementIndex: " + drawElementIndexList.get(0));
            return drawElementIndexList.get(0);
        }
        return null;
    }


    /**
     * 获得该行的最后一个索引
     *
     * @param line
     * @return
     */
    public DrawElementIndex getLineLastElementIndex(int line) {
        List<DrawElementIndex> drawElementIndexList = mElementIndexInLine.get(line);
        if (drawElementIndexList != null && !drawElementIndexList.isEmpty()) {
            Log.d(TAG, "getLineLastElementIndex: " + drawElementIndexList.get(drawElementIndexList.size() - 1));
            return drawElementIndexList.get(drawElementIndexList.size() - 1);
        }
        return null;
    }


    public DrawElementIndex getLastElementIndex() {
        int lastLine = mLines;
        if (!TextUtils.isEmpty(getText())){
            if (getText().charAt(getText().length()-1) == '\n'){
                lastLine = mLines-1;
            }
        }
        List<DrawElementIndex> drawElementIndexList = mElementIndexInLine.get(lastLine);
        if (drawElementIndexList != null && !drawElementIndexList.isEmpty()) {
            Log.d(TAG, "getLastElementIndex: " + drawElementIndexList.get(drawElementIndexList.size() - 1));
            return drawElementIndexList.get(drawElementIndexList.size() - 1);
        }
        return null;
    }


    private void clearElementIndexOfLine() {
        mElementIndexInLine.clear();
        drawElmentIndex = 0;
    }


    /**
     * 每个元素索引及其 在每一行的起始X坐标集合（如果有需要行可以对行行信息行信息里包含list)
     */
    private SparseArray<List<DrawElementIndex>> mElementIndexInLine = new SparseArray<>();


    /**
     *
     */
    public static class DrawElementIndex {

        public int line;
        public int index;
        public int startX;
        public int endX;
        public int charWidth;

        public DrawElementIndex(int line, int index, int startX, int endX, int charWidth) {
            this.line = line;
            this.index = index;
            this.startX = startX;
            this.endX = endX;
            this.charWidth = charWidth;
        }

        /**
         * 触摸X坐标是否处于当前字符
         *
         * @param x
         * @return
         */
        public boolean inRect(int x) {
            return x >= startX && x < endX;
        }

        @Override
        public String toString() {
            return "DrawElementIndex{" +
                    "line=" + line +
                    ", index=" + index +
                    ", startX=" + startX +
                    ", endX=" + endX +
                    '}';
        }
    }


// </editor-fold>
}
