/**
 *   920 Text Editor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   920 Text Editor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with 920 Text Editor.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jecelyin.widget;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.jecelyin.colorschemes.ColorScheme;
import com.jecelyin.editor.JecEditor;
import com.jecelyin.editor.R;
import com.jecelyin.editor.UndoParcel;
import com.jecelyin.editor.UndoParcel.TextChange;
import com.jecelyin.highlight.Highlight;
import com.jecelyin.util.FileUtil;
import com.jecelyin.util.TextUtil;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.Touch;
import android.text.style.ParagraphStyle;
import android.text.style.TabStopSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.Toast;

public class JecEditText extends EditText
{
    // private Rect mRect;
    private Paint mWhiteSpacePaint;
    private Paint mLineNumberPaint;
    private boolean mShowWhiteSpace = false;
    private boolean mShowLineNum = true;
    private Path mLineBreakPath = new Path();
    private Path mTabPath = new Path();
    private Path[] mWhiteSpacePaths = new Path[]{ mTabPath, mLineBreakPath };
    private TextPaint mTextPaint;
    private TextPaint mWorkPaint;
    private int paddingLeft = 0;
    private int lastPaddingLeft = 0;
    private int realLineNum = 0;
    private boolean hasNewline = true;
    private static float TAB_INCREMENT = 20F;
    private static Rect sTempRect = new Rect();
    private FastScroller mFastScroller;
    private Layout mLayout;
    private Editable mText = null;
    private UndoParcel mUndoParcel = new UndoParcel(); // 撤销与重做缓存
    private UndoParcel mRedoParcel = new UndoParcel(); // 撤销与重做缓存
    private boolean mUndoRedo = false; // 是否撤销过
    private boolean mAutoIndent = false;
    private HashMap<Integer, String> mLineStr = new HashMap<Integer, String>();
    private int mLineNumber = 0; // 总行数
    private int mLineNumberWidth = 0; // 行数栏宽度
    private int mLineNumberLength = 0; // 行数字数
    private ArrayList<Integer> mLastEditBuffer = new ArrayList<Integer>();
    private final static int LAST_EDIT_DISTANCE_LIMIT = 60; //最后编辑位置距离限制，不做同行判断
    private int mLastEditIndex = -1; //最后编辑位置功能的游标

    private final static String TAG = "JecEditText";
    private VelocityTracker mVelocityTracker;
    private FlingRunnable mFlingRunnable;
    
    private String current_encoding = "UTF-8"; // 当前文件的编码,用于正确回写文件
    private String current_path = ""; // 当前打开的文件路径
    private String current_ext = ""; // 当前扩展名
    private int src_text_length; //原始文本内容长度
    private boolean mNoWrapMode = false;
    private int mLineNumX = 0; //行数位置
    
    private Highlight mHighlight;

    // we need this constructor for LayoutInflater
    public JecEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        //init();
    }

    private static class JecSaveState extends BaseSavedState
    {
        UndoParcel mRedoParcelState;
        UndoParcel mUndoParcelState;

        JecSaveState(Parcelable superState)
        {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeParcelable(mUndoParcelState, 0);
            out.writeParcelable(mRedoParcelState, 0);
        }

        private JecSaveState(Parcel in)
        {
            super(in);
            mUndoParcelState = in.readParcelable(UndoParcel.class.getClassLoader());
            mRedoParcelState = in.readParcelable(UndoParcel.class.getClassLoader());
        }
    }

    /**
     * 保存文本各个操作状态
     */
    @Override
    public Parcelable onSaveInstanceState()
    {
        Parcelable superState = super.onSaveInstanceState();
        JecSaveState mJecSaveState = new JecSaveState(superState);
        mJecSaveState.mUndoParcelState = mUndoParcel;
        mJecSaveState.mRedoParcelState = mRedoParcel;
        return mJecSaveState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        Log.v("EditText", String.valueOf(state instanceof JecSaveState));
        if(!(state instanceof JecSaveState))
        {
            super.onRestoreInstanceState(state);
            return;
        }
        JecSaveState mJecSaveState = (JecSaveState) state;
        super.onRestoreInstanceState(mJecSaveState.getSuperState());
        mUndoParcel = mJecSaveState.mUndoParcelState;
        mRedoParcel = mJecSaveState.mRedoParcelState;
        setUndoRedoButtonStatus();
    }

    public void init()
    {
        mHighlight = new Highlight();
        mWorkPaint = new TextPaint();
        mTextPaint = getPaint(); // new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mLineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mWhiteSpacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // 横屏的时候关闭完成按钮和编辑状态不使用系统的全屏编辑框
        // IME_FLAG_NO_EXTRACT_UI: Flag of imeOptions: used to specify that the
        // IME does not need to show its extracted text
        // UI. For input methods that may be fullscreen, often when in landscape
        // mode, this allows them to be smaller and let
        // part of the application be shown behind. Though there will likely be
        // limited access to the application available
        // from the user, it can make the experience of a (mostly) fullscreen
        // IME less jarring. Note that when this flag is
        // specified the IME may not be set up to be able to display text, so it
        // should only be used in situations where this
        // is not needed.
        // IME_ACTION_DONE: Bits of IME_MASK_ACTION: the action key performs a
        // "done" operation, typically meaning the IME will be closed.
        setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        // 设置填充
        paddingLeft = getPaddingLeft();
        mFastScroller = new FastScroller(getContext(), this);
        addTextChangedListener(mUndoWatcher);
        clearFocus();
        
        float textSize = mTextPaint.getTextSize();

        mLineNumberPaint.setTextSize(textSize-2);
        mLineNumberPaint.setTypeface(Typeface.MONOSPACE);
        mLineNumberPaint.setStrokeWidth(1);
        mLineNumberPaint.setColor(Color.parseColor(ColorScheme.color_font));

        mWhiteSpacePaint.setStrokeWidth(0.75F);
        // mWhiteSpacePaint.setTextSize(textSize);
        // mWhiteSpacePaint.setTypeface(mTextPaint.getTypeface());
        mWhiteSpacePaint.setStyle(Paint.Style.STROKE);
        mWhiteSpacePaint.setColor(Color.GRAY);

        float textHeight;

        // 绘制换行符
        mLineBreakPath.reset();
        float width = mTextPaint.measureText("L");
        // descent为根据当前字体及其大小的基线到下面的距离(正数),ascent则相反
        float mDescent = mTextPaint.descent();
        float mAscent = mTextPaint.ascent();
        textHeight = mDescent - mAscent;
        /**
         * lineTo在没有moveTo的情况下,默认坐标是0,0；但是要注意,这个坐标是在 "正" 默认坐标是在正字左下角
         */
        // 移到底部中央
        mLineBreakPath.moveTo(width * 0.6F, 0);
        // 竖线
        mLineBreakPath.lineTo(width * 0.6F, -textHeight * 0.7F);
        // 左箭头
        mLineBreakPath.moveTo(width * 0.6F, 0);
        mLineBreakPath.lineTo(width * 0.25F, -textHeight * 0.3F);
        // 右箭头
        mLineBreakPath.moveTo(width * 0.6F, 0);
        mLineBreakPath.lineTo(width * 0.95F, -textHeight * 0.3F);

        // 绘制制表符
        mTabPath.reset();
        width = mTextPaint.measureText("\t\t"); // 制表符4个空格
        textHeight = mTextPaint.descent() - mTextPaint.ascent();
        // 绘制 >> 符号
        mTabPath.moveTo(0, -textHeight * 0.5F);
        // 绘制箭头下面那部分
        mTabPath.lineTo(width * 0.1F, -textHeight * 0.35F);
        // 绘制箭头上面部分
        mTabPath.lineTo(0, -textHeight * 0.2F);
        // two >
        mTabPath.moveTo(width * 0.15F, -textHeight * 0.5F);
        // 绘制箭头下面那部分
        mTabPath.lineTo(width * 0.25F, -textHeight * 0.35F);
        // 绘制箭头上面部分
        mTabPath.lineTo(width * 0.15F, -textHeight * 0.2F);
    }
    
    private OnTextChangedListener mOnTextChangedListener = null;
    public interface OnTextChangedListener
    {
        void onTextChanged(JecEditText mEditText);
    }
    
    public void setOnTextChangedListener(OnTextChangedListener l)
    {
        mOnTextChangedListener = l;
    }

    private TextWatcher mUndoWatcher = new TextWatcher() {
        TextChange lastChange;

        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
            //Log.v(TAG, "isLoading:" + JecEditor.isLoading);
            if(JecEditor.isLoading)
                return;
            mHighlight.redraw();
            //撤销，重做
            if(lastChange != null)
            {
                if(count < UndoParcel.MAX_SIZE)
                {
                    lastChange.newtext = s.subSequence(start, start + count);
                    if(start == lastChange.start && (lastChange.oldtext.length() > 0 || lastChange.newtext.length() > 0)
                            && !equalsCharSequence(lastChange.newtext, lastChange.oldtext))
                    {
                        mUndoParcel.push(lastChange);
                        mRedoParcel.removeAll();
                    }
                    setUndoRedoButtonStatus();
                }else
                {
                    mUndoParcel.removeAll();
                    mRedoParcel.removeAll();
                }
                lastChange = null;
            }
            //记住最后修改位置
            int bufSize = mLastEditBuffer.size();
            int lastLoc = 0;
            if(bufSize != 0)
            {
                lastLoc = mLastEditBuffer.get(bufSize-1);
            }
            //不在附近位置才记住它，不做是否同一行判断，性能问题
            if(Math.abs(start - lastLoc) > LAST_EDIT_DISTANCE_LIMIT)
            {
                mLastEditBuffer.add(start);
                mLastEditIndex = mLastEditBuffer.size() - 1;
                if(mOnTextChangedListener != null)
                    mOnTextChangedListener.onTextChanged(JecEditText.this);
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
            Log.v(TAG, "isLoading:" + JecEditor.isLoading);
            if(JecEditor.isLoading)
                return;
            if(mUndoRedo)
            {
                mUndoRedo = false;
            }else
            {
                if(count < UndoParcel.MAX_SIZE)
                {
                    lastChange = new TextChange();
                    lastChange.start = start;
                    lastChange.oldtext = s.subSequence(start, start + count);
                }else
                {
                    mUndoParcel.removeAll();
                    mRedoParcel.removeAll();
                    lastChange = null;
                }
            }
        }

        public void afterTextChanged(Editable s)
        {
        }
    };


    private boolean equalsCharSequence(CharSequence s1, CharSequence s2)
    {
        if(s1 == null || s2 == null)
        {
            return false;
        }
        if(s1.length() != s2.length())
        {
            return false;
        }
        return s1.toString().equals(s2.toString());
    }

    private void setUndoRedoButtonStatus()
    {
        if(mOnTextChangedListener != null)
            mOnTextChangedListener.onTextChanged(this);
    }
    
    public boolean canUndo()
    {
        return mUndoParcel.canUndo();
    }
    
    public boolean canRedo()
    {
        return mRedoParcel.canUndo();
    }
    
    public void show()
    {
        setVisibility(View.VISIBLE);
        if(mOnTextChangedListener != null)
            mOnTextChangedListener.onTextChanged(this);
    }
    
    public void hide()
    {
        setVisibility(View.GONE);
    }

    /**
     * 撤销
     */
    public void unDo()
    {
        TextChange textchange = mUndoParcel.pop();
        if(textchange != null)
        {
            Editable text = getText();
            mUndoRedo = true;
            text.replace(textchange.start, textchange.start + textchange.newtext.length(), textchange.oldtext);
            Selection.setSelection(text, textchange.start + textchange.oldtext.length());
            mRedoParcel.push(textchange);
            setUndoRedoButtonStatus();
        }
    }

    /**
     * 重做
     */
    public void reDo()
    {
        TextChange textchange = mRedoParcel.pop();
        if(textchange != null)
        {
            Editable text = getText();
            mUndoRedo = true;
            text.replace(textchange.start, textchange.start + textchange.oldtext.length(), textchange.newtext);
            Selection.setSelection(text, textchange.start + textchange.newtext.length());
            mUndoParcel.push(textchange);
            setUndoRedoButtonStatus();
        }
    }

    /**
     * 重置撤销，重做状态
     */
    public void resetUndoStatus()
    {
        mRedoParcel.clean();
        mUndoParcel.clean();
        setUndoRedoButtonStatus();
        mLastEditBuffer.clear();
    }

    private void setLineNumberWidth(int lastline)
    {
        mLineNumberWidth = (int) mLineNumberPaint.measureText(lastline + "|");
        
        mLineNumber = lastline;
        mLineNumberLength = Integer.toString(lastline).length();
        setShowLineNum(mShowLineNum);
    }

    public void setShowLineNum(boolean b)
    {
        mShowLineNum = b;

        int left;

        if(!mShowLineNum)
        {
            left = paddingLeft;
        }else
        {
            left = paddingLeft + mLineNumberWidth;
        }
        setPaddingLeft(left);
    }

    public void setShowWhitespace(boolean b)
    {
        mShowWhiteSpace = b;
    }
    
    public void setText2(CharSequence text)
    {
        try {
            super.setText(text);
        } catch (OutOfMemoryError e) {
            Toast.makeText(getContext(), R.string.out_of_memory, Toast.LENGTH_SHORT).show();
            Log.d(TAG, e.getMessage());
        }
    }
    
    public String getString()
    {
        return getText().toString();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        mLayout = getLayout();
        mText = (Editable) getText();

        super.onDraw(canvas);

        drawView(canvas);

        if(mFastScroller != null)
        {
            mFastScroller.draw(canvas);
        }

    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if(mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        //是否按住滚动条拖动
        if(mFastScroller != null)
        {
            boolean intercepted;
            intercepted = mFastScroller.onTouchEvent(event);
            //Log.v(TAG, "intercepted2:"+intercepted);
            if(intercepted)
            {
                return true;
            }
            intercepted = mFastScroller.onInterceptTouchEvent(event);
            //Log.v(TAG, "intercepted1:"+intercepted);
            if(intercepted)
            {
                return true;
            }
            
        }
        //处理文本快速顺畅地滚动
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if(mFlingRunnable != null)
                {
                    mFlingRunnable.endFling();
                    cancelLongPress();
                }

                break;
            case MotionEvent.ACTION_UP:
                
                int mMinimumVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
                int mMaximumVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final int initialVelocity = (int) mVelocityTracker.getYVelocity();

                if(Math.abs(initialVelocity) > mMinimumVelocity)
                {
                    if(mFlingRunnable == null)
                    {
                        mFlingRunnable = new FlingRunnable(getContext());
                    }
                    mHighlight.stop();
                    mFlingRunnable.start(this, -initialVelocity);
                }else
                {
                    moveCursorToVisibleOffset();
                }

                if(mVelocityTracker != null)
                {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * Responsible for fling behavior. Use {@link #start(int)} to initiate a
     * fling. Each frame of the fling is handled in {@link #run()}. A
     * FlingRunnable will keep re-posting itself until the fling is done.
     * 
     */
    private static class FlingRunnable implements Runnable
    {

        static final int TOUCH_MODE_REST = -1;
        static final int TOUCH_MODE_FLING = 3;

        int mTouchMode = TOUCH_MODE_REST;

        /**
         * Tracks the decay of a fling scroll
         */
        private final Scroller mScroller;

        /**
         * Y value reported by mScroller on the previous fling
         */
        private int mLastFlingY;

        private JecEditText mWidget = null;

        FlingRunnable(Context context)
        {
            mScroller = new Scroller(context);
        }

        void start(JecEditText parent, int initialVelocity)
        {
            mWidget = parent;
            int initialX = parent.getScrollX(); // initialVelocity < 0 ?
                                                // Integer.MAX_VALUE : 0;
            int initialY = parent.getScrollY(); // initialVelocity < 0 ?
                                                // Integer.MAX_VALUE : 0;
            mLastFlingY = initialY;
            mScroller.fling(initialX, initialY, 0, initialVelocity, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            mTouchMode = TOUCH_MODE_FLING;

            mWidget.post(this);

        }

        private void endFling()
        {
            mTouchMode = TOUCH_MODE_REST;

            if(mWidget != null)
            {
                try {
                    mWidget.removeCallbacks(this);
                    mWidget.moveCursorToVisibleOffset();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                
                mWidget = null;
            }

        }

        public void run()
        {
            switch(mTouchMode)
            {
                default:
                    return;

                case TOUCH_MODE_FLING:
                {

                    final Scroller scroller = mScroller;
                    boolean more = scroller.computeScrollOffset();

                    int x = scroller.getCurrX();
                    int y = scroller.getCurrY();

                    Layout layout = mWidget.getLayout();

                    int padding;
                    try {
                        padding = mWidget.getTotalPaddingTop() + mWidget.getTotalPaddingBottom();
                    } catch(Exception e) {
                        padding = 0;
                    }
                    

                    y = Math.min(y, layout.getHeight() - (mWidget.getHeight() - padding));
                    y = Math.max(y, 0);

                    Touch.scrollTo(mWidget, layout, x, y);
                    int delta = mLastFlingY - y;
                    //Log.d(TAG, "delta:"+delta);
                    if(Math.abs(delta) <= 5)
                    {
                        mWidget.mHighlight.redraw();
                    }
                    if(more && delta != 0)
                    {
                        mWidget.invalidate();
                        mLastFlingY = y;
                        mWidget.post(this);
                    }else
                    {
                        endFling();

                    }
                    break;
                }
            }

        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {

        if(mFastScroller != null)
        {
            mFastScroller.onSizeChanged(w, h, oldw, oldh);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);

        if(mFastScroller != null && mLayout != null)
        {
            int h = getVisibleHeight();
            int h2 = mLayout.getHeight();
            mFastScroller.onScroll(this, t, h, h2);
        }

    }

    public int getVisibleHeight()
    {
        int b = getBottom();
        int t = getTop();
        int pb = getExtendedPaddingBottom();
        int pt = getExtendedPaddingTop();
        return b - t - pb - pt;
    }

    /**
     * Draw this Layout on the specified canvas, with the highlight path drawn
     * between the background and the text.
     * 
     * @param c
     *            the canvas
     * @param highlight
     *            the path of the highlight or cursor; can be null
     * @param highlightPaint
     *            the paint for the highlight
     * @param cursorOffsetVertical
     *            the amount to temporarily translate the canvas while rendering
     *            the highlight
     */
    public void drawView(Canvas c)
    {
        int dtop, dbottom;

        synchronized (sTempRect)
        {
            if(!c.getClipBounds(sTempRect))
            {
                return;
            }

            dtop = sTempRect.top;
            dbottom = sTempRect.bottom;
        }
        if(mLayout == null)
            return;

        int textLength = mText.length();

        int top = 0;
        int lineCount = mLayout.getLineCount();
        int bottom = mLayout.getLineTop(lineCount);

        if(dtop > top)
        {
            top = dtop;
        }
        if(dbottom < bottom)
        {
            bottom = dbottom;
        }

        int first = mLayout.getLineForVertical(top);
        int last = mLayout.getLineForVertical(bottom);

        int previousLineBottom = mLayout.getLineTop(first);
        int previousLineEnd = mLayout.getLineStart(first);

        TextPaint paint = mTextPaint;

        ParagraphStyle[] spans = NO_PARA_SPANS;
        
        //Log.d("Highlight", first+"-"+last+"="+dtop+":"+dbottom);
        //这里不要使用getScrollY，因为修改时，光标会变，滚动条不会变，但是高亮需要变
        int previousLineEnd2 = mLayout.getLineStart(first >= 3 ? first-3 : 0);
        mHighlight.render(mText, previousLineEnd2,  mLayout.getLineStart(last+3 > lineCount ? lineCount : last+3));

        if(!mShowLineNum && !mShowWhiteSpace)
        {
            return;
        }
        
        // 显示行数
        int lastline = lineCount < 1 ? 1 : lineCount;
        if(lastline != mLineNumber)
        {
            setLineNumberWidth(lastline);
        }
        //设置显示行号的位置
        if(mNoWrapMode)
        {
            mLineNumX = mLineNumberWidth + getScrollX();
        } else {
            mLineNumX = mLineNumberWidth;
        }

        int right = getWidth();
        int left = getPaddingLeft();
        // 真实行数
        if(previousLineEnd > 1)
        {
            if(previousLineEnd >= mText.length())
                return;
            realLineNum = TextUtil.countMatches(mText, '\n', 0, previousLineEnd);
            // Log.v("edittext",
            // "curVisibleLineEnd:"+curVisibleLineEnd+" realLineNum:"+realLineNum);
            // 如果当前行是新行，则需要+1
            if(mText.charAt(previousLineEnd) != '\n')
            {
                realLineNum++;
            }
        }else
        {
            realLineNum = 1;
        }
        // Log.v("tag", "f:"+first+" l:"+last);
        hasNewline = true;

        // 为了空白时也默认有一行
        if(last == 0)
        {
            c.drawLine(mLineNumX, top, mLineNumX, mTextPaint.getTextSize(), mLineNumberPaint);
            if(hasNewline)
            {
                String lineString = mLineStr.get(realLineNum);
                if(lineString == null)
                {
                    lineString = "      " + realLineNum;
                    mLineStr.put(realLineNum, lineString);
                }
                c.drawText(lineString, lineString.length() - mLineNumberLength, lineString.length(), mLineNumX-mLineNumberWidth, mTextPaint.getTextSize(), mLineNumberPaint);
            }
            return;
        }

        // Next draw the lines, one at a time.
        // the baseline is the top of the following line minus the current
        // line's descent.
        for (int i = first; i <= last; i++)
        {
            int start = previousLineEnd;

            previousLineEnd = mLayout.getLineStart(i + 1);
            int end = getLineVisibleEnd(i, start, previousLineEnd);

            int ltop = previousLineBottom;
            int lbottom = mLayout.getLineTop(i + 1);
            previousLineBottom = lbottom;
            int lbaseline = lbottom;// - mLayout.getLineDescent(i);

            int dir = mLayout.getParagraphDirection(i);

            // Adjust the point at which to start rendering depending on the
            // alignment of the paragraph.
            int x;
            if(dir == DIR_LEFT_TO_RIGHT)
            {
                x = left;
            }else
            {
                x = right;
            }

            // jecelyin: 默认左到右，肯定不会有右到左出现
            // Directions directions = getLineDirections(i);
            Directions directions = DIRS_ALL_LEFT_TO_RIGHT;
            // android.text.Layout.Directions directions =
            // mLayout.getLineDirections(i);
            boolean hasTab = mLayout.getLineContainsTab(i);
            drawText(c, start, end, dir, directions, x, ltop, lbaseline, lbottom, paint, mWorkPaint, hasTab, spans, textLength, i + 1 == last);

        }
    }

    private void drawText(Canvas canvas, int start, int end, int dir, Directions directions, final float x, int top, int y, int bottom, TextPaint paint, TextPaint workPaint,
            boolean hasTabs, Object[] parspans, int textLength, boolean islastline)
    {
        // linenum
        if(mShowLineNum)
        {
            // 竖线
            // drawLine (float startX, float startY, float stopX, float stopY,
            // Paint paint)
            canvas.drawLine(mLineNumX, top, mLineNumX, islastline ? bottom + (bottom - top) : bottom, mLineNumberPaint);
            if(hasNewline)
            {
                String lineString = mLineStr.get(realLineNum);
                if(lineString == null)
                {
                    lineString = "      " + realLineNum;
                    mLineStr.put(realLineNum, lineString);
                }
                canvas.drawText(lineString, lineString.length() - mLineNumberLength, lineString.length(), mLineNumX-mLineNumberWidth + 1, y-2, mLineNumberPaint);
                realLineNum++;
                hasNewline = false;
            }
        }

        float h = 0;
        int here = 0;
        for (int i = 0; i < directions.mDirections.length; i++)
        // for (int i = 0; i < 1; i++)
        {
            int there = here + directions.mDirections[i];
            if(there > end - start)
                there = end - start;

            int segstart = here;
            for (int j = hasTabs ? here : there; j <= there; j++)
            {
                if(start + j > end)
                    break;
                char at = start + j == end ? 0 : mText.charAt(start + j);
                if(j == there || at == '\t')
                {

                    h += Styled.drawText(null, mText, start + segstart, start + j, dir, (i & 1) != 0, x + h, top, y, bottom, paint, workPaint, (start + j == end) || hasTabs);

                    if(j != there && at == '\t' && mShowWhiteSpace)
                    {
                        if(x+h > mLineNumX)
                        {
                            canvas.translate(x + h, y);
                            canvas.drawPath(mWhiteSpacePaths[0], mWhiteSpacePaint);
                            canvas.translate(-x - h, -y);
                        }
                        h = dir * nextTabPos(mText, start, end, h * dir, parspans);
                    }else if(j == there)
                    {
                        if(end < textLength && mText.charAt(end) == '\n')
                        {
                            if(mShowWhiteSpace && x+h > mLineNumX)
                            {
                                canvas.translate(x + h, y);
                                canvas.drawPath(mWhiteSpacePaths[1], mWhiteSpacePaint);
                                canvas.translate(-x - h, -y);
                            }

                            hasNewline = true;
                            break;
                        }
                    }

                    segstart = j + 1;
                }
            }// end for
            here = there;
        }

    }

    /**
     * Returns the position of the next tab stop after h on the line.
     * 
     * @param text
     *            the text
     * @param start
     *            start of the line
     * @param end
     *            limit of the line
     * @param h
     *            the current horizontal offset
     * @param tabs
     *            the tabs, can be null. If it is null, any tabs in effect on
     *            the line will be used. If there are no tabs, a default offset
     *            will be used to compute the tab stop.
     * @return the offset of the next tab stop.
     */
    /* package */static float nextTabPos(CharSequence text, int start, int end, float h, Object[] tabs)
    {
        float nh = Float.MAX_VALUE;
        boolean alltabs = false;

        if(text instanceof Spanned)
        {
            if(tabs == null)
            {
                tabs = ((Spanned) text).getSpans(start, end, TabStopSpan.class);
                alltabs = true;
            }

            for (int i = 0; i < tabs.length; i++)
            {
                if(!alltabs)
                {
                    if(!(tabs[i] instanceof TabStopSpan))
                        continue;
                }

                int where = ((TabStopSpan) tabs[i]).getTabStop();

                if(where < nh && where > h)
                    nh = where;
            }

            if(nh != Float.MAX_VALUE)
                return nh;
        }

        return ((int) ((h + TAB_INCREMENT) / TAB_INCREMENT)) * TAB_INCREMENT;
    }

    /**
     * Stores information about bidirectional (left-to-right or right-to-left)
     * text within the layout of a line. TODO: This work is not complete or
     * correct and will be fleshed out in a later revision.
     */
    public static class Directions
    {
        private short[] mDirections;

        // The values in mDirections are the offsets from the first character
        // in the line to the next flip in direction. Runs at even indices
        // are left-to-right, the others are right-to-left. So, for example,
        // a line that starts with a right-to-left run has 0 at mDirections[0],
        // since the 'first' (ltr) run is zero length.
        //
        // The code currently assumes that each run is adjacent to the previous
        // one, progressing in the base line direction. This isn't sufficient
        // to handle nested runs, for example numeric text in an rtl context
        // in an ltr paragraph.
        /* package */Directions(short[] dirs)
        {
            mDirections = dirs;
        }
    }

    private static final ParagraphStyle[] NO_PARA_SPANS = new ParagraphStyle[]{};

    /* package */static final Directions DIRS_ALL_LEFT_TO_RIGHT = new Directions(new short[]{ 32767 });
    /* package */static final Directions DIRS_ALL_RIGHT_TO_LEFT = new Directions(new short[]{ 0, 32767 });
    public static final int DIR_LEFT_TO_RIGHT = 1;
    public static final int DIR_RIGHT_TO_LEFT = -1;

    /**
     * Return the text offset after the last visible character (so whitespace is
     * not counted) on the specified line.
     */
    public int getLineVisibleEnd(int line)
    {
        return getLineVisibleEnd(line, mLayout.getLineStart(line), mLayout.getLineStart(line + 1));
    }

    private int getLineVisibleEnd(int line, int start, int end)
    {

        CharSequence text = getText();
        char ch;
        if(line == getLineCount() - 1)
        {
            return end;
        }
        // fix IndexOutOfBoundsException SpannableStringBuilder.charAt()

        if(end < 1)
            return 0;

        for (; end > start; end--)
        {
            try
            {
                ch = text.charAt(end - 1);
            }catch (Exception e)
            {
                return end;
            }

            if(ch == '\n')
            {
                return end - 1;
            }

            if(ch != ' ' && ch != '\t')
            {
                break;
            }

        }

        return end;
    }

    public boolean gotoLine(int line)
    {
        if(line < 1)
            return false;
        int count = 0;
        int strlen = mText.length();
        for (int index = 0; index < strlen; index++)
        {
            if(mText.charAt(index) == '\n')
            {
                count++;
                if(count == line)
                {
                    Selection.setSelection((Spannable) mText, index, index);
                    return true;
                }
            }
        }

        return false;
    }
    
    public boolean gotoBackEditLocation()
    {
        if(mLastEditIndex < 1)
            return false;
        mLastEditIndex--;
        int offset = mLastEditBuffer.get(mLastEditIndex);
        setSelection(offset, offset);
        return true;
    }
    
    public boolean gotoForwardEditLocation()
    {
        if(mLastEditIndex >= mLastEditBuffer.size())
            return false;
        mLastEditIndex++;
        int offset = mLastEditBuffer.get(mLastEditIndex);
        setSelection(offset, offset);
        return true;
    }
    
    public boolean isCanBackEditLocation()
    {
        if(mLastEditIndex < 1)
            return false;
        return mLastEditIndex < mLastEditBuffer.size();
    }
    
    public boolean isCanForwardEditLocation()
    {
        if(mLastEditIndex >= mLastEditBuffer.size()-1)
            return false;
        //return mLastEditIndex < mLastEditBuffer.size();
        return true;
    }

    public void setAutoIndent(boolean open)
    {
        mAutoIndent = open;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        boolean result = super.onKeyDown(keyCode, event);
        // 自动缩进
        if(mAutoIndent && keyCode == KeyEvent.KEYCODE_ENTER)
        {
            Editable mEditable = (Editable) mText;
            if(mEditable == null)
                return result;

            int start = getSelectionStart();
            int end = getSelectionEnd();
            if(start == end)
            {
                int prev = start - 2;
                while (prev >= 0 && mEditable.charAt(prev) != '\n')
                {
                    prev--;
                }
                prev++;
                int pos = prev;
                while (mEditable.charAt(pos) == ' ' || mEditable.charAt(pos) == '\t' || mEditable.charAt(pos) == '\u3000')
                {
                    pos++;
                }
                int len = pos - prev;
                if(len > 0)
                {
                    try
                    {
                        char[] dest = new char[len];
                        mEditable.getChars(prev, pos, dest, 0);
                        mEditable.replace(start, end, new String(dest));
                        setSelection(start + len);
                    }catch (Exception e)
                    {

                    }

                }
            }
        }
        return result;
    }
    
    public void setEncoding(String encoding)
    {
        current_encoding = encoding;
    }
    
    public void setPath(String path)
    {
        if("".equals(path))
            return;
        current_path = path;

        File f = new File(current_path);
        long fsize = f.length() / 1024;
        if(fsize > Highlight.getLimitFileSize())
        {
            Toast.makeText(getContext(), getResources().getString(R.string.highlight_stop_msg), Toast.LENGTH_LONG).show();
            return;
        }
        setCurrentFileExt(FileUtil.getExt(path));
    }
    
    public void setCurrentFileExt(String ext)
    {
        current_ext = ext;
        
        mHighlight.redraw();
        mHighlight.setSyntaxType(current_ext);
    }
    
    public String getCurrentFileExt()
    {
        return current_ext;
    }
    
    public String getEncoding()
    {
        return current_encoding;
    }
    
    public String getPath()
    {
        return current_path;
    }
    
    public void setTextFinger()
    {
        src_text_length = getText().length();
    }
    
    public boolean isTextChanged()
    {
        //简单判断一下内容是否改变
        return src_text_length != getText().length();
    }
    
    public void setHorizontallyScrolling(boolean whether)
    {
        mNoWrapMode = whether;
        super.setHorizontallyScrolling(whether);
    }
    
    public void setPaddingLeft(int padding)
    {
        if(lastPaddingLeft == padding)
            return;
        if(padding < paddingLeft)
            padding = paddingLeft;
        lastPaddingLeft = padding;
        setPadding(padding, 0, getPaddingRight(), getPaddingBottom());
    }

}
