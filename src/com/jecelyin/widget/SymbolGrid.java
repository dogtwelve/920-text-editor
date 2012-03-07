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

import java.util.ArrayList;
import java.util.HashMap;

import com.jecelyin.editor.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class SymbolGrid extends LinearLayout
{
    private ArrayList<HashMap<String, Object>> mButtons;
    private OnSymbolClickListener mListener;
    private ImageView closeButton;
    private GridView mGridView;
    private int mTop, mRight, mBottom, mLeft;

    public SymbolGrid(Context context)
    {
        super(context);
    }
    
    public SymbolGrid(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        //inflate(context, R.layout.symbol_grid, this);
        init();
    }
    
    private void init()
    {
        LinearLayout.LayoutParams layoutParams;
        DisplayMetrics dm = getResources().getDisplayMetrics();

        int dip6 = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9, dm);
        int dip10 = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, dm);
        //自身属性
        /*layoutParams = new LayoutParams((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260, dm), LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        setBackgroundColor(Color.parseColor("#b0000000"));
        setOrientation(LinearLayout.VERTICAL);
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);*/
        //关闭按钮
        closeButton = new ImageView(getContext());
        closeButton.setImageResource(R.drawable.dialog_close);
        layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.RIGHT;
        layoutParams.setMargins(dip6, dip6, dip6, dip6);
        closeButton.setLayoutParams(layoutParams);
        addView(closeButton);
        //符号表格
        mGridView = new GridView(getContext());
        layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mGridView.setLayoutParams(layoutParams);
        mGridView.setHorizontalSpacing(dip10);
        mGridView.setVerticalSpacing(dip10);
        mGridView.setNumColumns(6);
        mGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appendToolbarButton();
        addView(mGridView);
        setPadding(dip10, dip10, dip10, dip10);
        invalidate();
    }

    public static interface OnSymbolClickListener
    {
        void OnClick(String symbol);
    }
    
    public void setClickListener(OnSymbolClickListener mOnSymbolClickListener)
    {
        mListener = mOnSymbolClickListener;
    }

    private void appendToolbarButton()
    {
        mButtons = new ArrayList<HashMap<String, Object>>();
        mButtons.add(buildToolbarButton(R.drawable.tool_opn_curly1, "{", "tool_opn_curly1"));
        mButtons.add(buildToolbarButton(R.drawable.tool_cls_curly1, "}", "tool_cls_curly"));
        mButtons.add(buildToolbarButton(R.drawable.tool_less1, "<", "tool_less"));
        mButtons.add(buildToolbarButton(R.drawable.tool_more1, ">", "tool_more"));
        mButtons.add(buildToolbarButton(R.drawable.tool_semi1, ";", "tool_semi"));
        mButtons.add(buildToolbarButton(R.drawable.tool_situation1, "\"", "tool_situation"));

        mButtons.add(buildToolbarButton(R.drawable.tool_sl_brack1, "(", "tool_sl_brack"));
        mButtons.add(buildToolbarButton(R.drawable.tool_sr_brack1, ")", "tool_sr_brack"));
        mButtons.add(buildToolbarButton(R.drawable.tool_slash1, "/", "tool_slash"));
        mButtons.add(buildToolbarButton(R.drawable.tool_escape1, "\\", "tool_escape"));
        mButtons.add(buildToolbarButton(R.drawable.tool_single_quotes1, "'", "tool_single_quotes"));
        mButtons.add(buildToolbarButton(R.drawable.tool_percent1, "%", "tool_percent"));
        mButtons.add(buildToolbarButton(R.drawable.tool_opn_brack1, "[", "tool_opn_brack"));
        mButtons.add(buildToolbarButton(R.drawable.tool_cls_brack1, "]", "tool_cls_brack"));
        
        mButtons.add(buildToolbarButton(R.drawable.tool_line1, "|", "tool_line"));
        mButtons.add(buildToolbarButton(R.drawable.tool_hash1, "#", "tool_hash"));
        mButtons.add(buildToolbarButton(R.drawable.tool_equals1, "=", "tool_equals"));
        mButtons.add(buildToolbarButton(R.drawable.tool_dollar1, "$", "tool_dollar"));
        mButtons.add(buildToolbarButton(R.drawable.tool_colon1, ":", "tool_colon"));

        mButtons.add(buildToolbarButton(R.drawable.tool_comma1, ",", "tool_comma"));
        mButtons.add(buildToolbarButton(R.drawable.tool_and1, "&", "tool_and"));
        mButtons.add(buildToolbarButton(R.drawable.tool_question, "?", "tool_question"));
        
        mButtons.add(buildToolbarButton(R.drawable.tool_tab1, "\t", "tool_tab"));
        mButtons.add(buildToolbarButton(R.drawable.tool_enter1, "\n", "tool_enter"));

        closeButton.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v)
             {
                 SymbolGrid.this.setVisibility(View.GONE);
             }
        });

        mGridView.setAdapter(new SimpleAdapter(getContext(), mButtons, R.layout.symbol_item, new String[]{ "res" }, new int[]{ R.id.symbol_iv }));
        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                HashMap<String, Object> map = (HashMap<String, Object>) mButtons.get(position);
                String symbol = (String) map.get("symbol");
                mListener.OnClick(symbol);
            }
        });
        
        setOnTouchListener(new OnTouchListener() {
            private int lastX, lastY; // 记录移动的最后的位置
            
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // 获取Action
                int ea = event.getAction();

                switch(ea)
                {
                    case MotionEvent.ACTION_DOWN: // 按下
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE: // 移动
                        // 移动中动态设置位置
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        mLeft = v.getLeft() + dx;
                        mTop = v.getTop() + dy;
                        mRight = v.getRight() + dx;
                        mBottom = v.getBottom() + dy;
                        v.layout(mLeft, mTop, mRight, mBottom);
                        // 将当前的位置再次设置
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP: // 脱离
                        break;
                }
                return true;
            }
        });
    }

    private HashMap<String, Object> buildToolbarButton(int bg, final String symbol, String id)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("symbol", symbol);
        map.put("res", bg);
        return map;
    }
    
    @Override
    public void setVisibility(int visibility)
    {
        //不能直接GONE，不然位置会变
        if(visibility == View.GONE)
            visibility = View.INVISIBLE;
        super.setVisibility(visibility);
    }
    
}
