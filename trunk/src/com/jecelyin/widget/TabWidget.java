/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jecelyin.widget;

import com.jecelyin.editor.R;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

/**
 *
 * Displays a list of tab labels representing each page in the parent's tab
 * collection. The container object for this widget is
 * {@link android.widget.TabHost TabHost}. When the user selects a tab, this
 * object sends a message to the parent container, TabHost, to tell it to switch
 * the displayed page. You typically won't use many methods directly on this
 * object. The container TabHost is used to add labels, add the callback
 * handler, and manage callbacks. You might call this object to iterate the list
 * of tabs, or to tweak the layout of the tab list, but most methods should be
 * called on the containing TabHost object.
 *
 * <p>See the <a href="{@docRoot}resources/tutorials/views/hello-tabwidget.html">Tab Layout
 * tutorial</a>.</p>
 * 
 * @attr ref android.R.styleable#TabWidget_divider
 * @attr ref android.R.styleable#TabWidget_tabStripEnabled
 * @attr ref android.R.styleable#TabWidget_tabStripLeft
 * @attr ref android.R.styleable#TabWidget_tabStripRight
 */
public class TabWidget extends LinearLayout {
    private OnTabSelectionChanged mSelectionChangedListener;

    private int mSelectedTab = 0;

    public TabWidget(Context context) {
        super(context);
        //this(context, attrs, com.android.internal.R.attr.tabWidgetStyle);
        initTabWidget();
    }
    
    public TabWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        //this(context, attrs, com.android.internal.R.attr.tabWidgetStyle);
        initTabWidget();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void initTabWidget() {
        // Deal with focus, as we don't want the focus to go by default
        // to a tab other than the current tab
        setFocusable(true);
    }

    /**
     * Returns the tab indicator view at the given index.
     *
     * @param index the zero-based index of the tab indicator view to return
     * @return the tab indicator view at the given index
     */
    public View getChildTabViewAt(int index) {

        return getChildAt(index);
    }

    /**
     * Returns the number of tab indicator views.
     * @return the number of tab indicator views.
     */
    public int getTabCount() {
        int children = getChildCount();

        return children;
    }

    /**
     * Sets the current tab.
     * This method is used to bring a tab to the front of the Widget,
     * and is used to post to the rest of the UI that a different tab
     * has been brought to the foreground.
     *
     * Note, this is separate from the traditional "focus" that is
     * employed from the view logic.
     *
     * For instance, if we have a list in a tabbed view, a user may be
     * navigating up and down the list, moving the UI focus (orange
     * highlighting) through the list items.  The cursor movement does
     * not effect the "selected" tab though, because what is being
     * scrolled through is all on the same tab.  The selected tab only
     * changes when we navigate between tabs (moving from the list view
     * to the next tabbed view, in this example).
     *
     * To move both the focus AND the selected tab at once, please use
     * {@link #setCurrentTab}. Normally, the view logic takes care of
     * adjusting the focus, so unless you're circumventing the UI,
     * you'll probably just focus your interest here.
     *
     *  @param index The tab that you want to indicate as the selected
     *  tab (tab brought to the front of the widget)
     *
     *  @see #focusCurrentTab
     */
    public void setCurrentTab(int index) {
        if (index < 0 || index >= getTabCount()) {
            return;
        }

        getChildTabViewAt(mSelectedTab).setSelected(false);
        getChildTabViewAt(mSelectedTab).setBackgroundResource(R.drawable.tab_indicator);
        mSelectedTab = index;
        getChildTabViewAt(mSelectedTab).setSelected(true);
        getChildTabViewAt(mSelectedTab).setBackgroundResource(R.drawable.tab_indicator_current);
        //Log.d("TabWidget", "setCurrentTab:"+index);
    }

    /**
     * Sets the current tab and focuses the UI on it.
     * This method makes sure that the focused tab matches the selected
     * tab, normally at {@link #setCurrentTab}.  Normally this would not
     * be an issue if we go through the UI, since the UI is responsible
     * for calling TabWidget.onFocusChanged(), but in the case where we
     * are selecting the tab programmatically, we'll need to make sure
     * focus keeps up.
     *
     *  @param index The tab that you want focused (highlighted in orange)
     *  and selected (tab brought to the front of the widget)
     *
     *  @see #setCurrentTab
     */
    public void focusCurrentTab(int index) {
        final int oldTab = mSelectedTab;

        // set the tab
        setCurrentTab(index);

        // change the focus if applicable.
        if (oldTab != index) {
            getChildTabViewAt(index).requestFocus();
        }
    }

    @Override
    public void addView(View child) {
        /*if (child.getLayoutParams() == null) {
            final LinearLayout.LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.FILL_PARENT, 1.0f);
            lp.setMargins(0, 0, 0, 0);
            child.setLayoutParams(lp);
        }*/
        // Ensure you can navigate to the tab with the keyboard, and you can touch it
        child.setFocusable(true);
        child.setClickable(true);
        if(getTabCount() == 0)
        {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)child.getLayoutParams();
            lp.leftMargin = 0;
            child.setLayoutParams(lp);
        }

        super.addView(child);

        //触发onFocusChange
        child.setOnClickListener(mOnClickListener);
    }

    /**
     * Provides a way for {@link TabHost} to be notified that the user clicked on a tab indicator.
     */
    void setTabSelectionListener(OnTabSelectionChanged listener) {
        mSelectionChangedListener = listener;
    }

    private OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v)
        {
            int index = indexOfChild(v);
            if(index < 0)
                return;
            Log.d("TabWidget", "onFocusChange: "+mSelectedTab+"/"+index);
            setCurrentTab(index);
            mSelectionChangedListener.onTabSelectionChanged(index);
        }
    };

    static interface OnTabSelectionChanged {
        /**
         * Informs the TabHost which tab was selected. It also indicates
         * if the tab was clicked/pressed or just focused into.
         *
         * @param tabIndex index of the tab that was selected
         */
        void onTabSelectionChanged(int tabIndex);
    }
    
    protected void dispatchDraw(Canvas paramCanvas)
    {
        int count = getTabCount();
        if(count<1 || mSelectedTab < 0)
            return;
        final long drawingTime = getDrawingTime();
        View mView;
        for(int i=count-1; i>=0; i--)
        {
            if(i != mSelectedTab)
            {
                mView = getChildAt(i);
                if(mView != null)
                {
                    drawChild(paramCanvas, mView, drawingTime);
                }
                
            }
        }
        //覆盖在其它tab上面，即是将它置于最前
        mView = getChildAt(mSelectedTab);
        if(mView != null)
            drawChild(paramCanvas, mView, drawingTime);
    }
    
    public void removeViewAt(int tabId)
    {
        mSelectedTab = 0;
        super.removeViewAt(tabId);
    }

}

