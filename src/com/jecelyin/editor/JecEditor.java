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

package com.jecelyin.editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jecelyin.editor.R;
import com.jecelyin.highlight.Highlight;
import com.jecelyin.util.ColorPicker;
import com.jecelyin.util.FileBrowser;
import com.jecelyin.util.FileUtil;
import com.jecelyin.util.LinuxShell;
import com.jecelyin.util.TimeUtil;
import com.jecelyin.widget.JecEditText;
import com.jecelyin.widget.JecMenu;
import com.jecelyin.widget.JecMenu.OnMenuItemSelectedListener;
import com.jecelyin.widget.SymbolGrid;
import com.jecelyin.widget.SymbolGrid.OnSymbolClickListener;
import com.jecelyin.widget.TabHost;
import com.jecelyin.widget.TabHost.OnTabChangeListener;
import com.jecelyin.widget.TabHost.OnTabCloseListener;

import android.app.Activity;
import android.app.AlertDialog;
/*import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;*/
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class JecEditor extends Activity
{
    public final static int FILE_BROWSER_OPEN_CODE = 0; // 打开
    public final static int FILE_BROWSER_SAVEAS_CODE = 1; // 另存为
    private final static String TAG = "JecEditor";
    public final static String PREF_HISTORY = "history"; // 保存打开文件记录
    private final static String PREF_LAST_FILE = "last_files"; // 最后打开的文件
    private final static String SYNTAX_SIGN = "16";
    public static String version = "";
    public static String TEMP_PATH = "";
    private JecEditText mEditText;
    // SL4A
    private static final String EXTRA_SCRIPT_PATH = "com.googlecode.android_scripting.extra.SCRIPT_PATH";
    private static final String EXTRA_SCRIPT_CONTENT = "com.googlecode.android_scripting.extra.SCRIPT_CONTENT";
    private static final String ACTION_EDIT_SCRIPT = "com.googlecode.android_scripting.action.EDIT_SCRIPT";
    
    public int MAX_HIGHLIGHT_FILESIZE = 400;
    //private int org_textcontent_md5 = 0;
    private boolean back_button_exit = true; // 按返回键退出程序
    private boolean autosave = false; // 是否自动保存
    // end

    public static boolean isLoading = false; // 是否正在加载文件
    private static boolean fullScreen = false; // 是否已经全屏状态
    private static boolean hideToolbar = false; // 是否已经隐藏工具栏
    public static boolean isRoot = false;
    private static boolean mHideSoftKeyboard = false;

    // button
    private ImageButton undoBtn;
    private ImageButton redoBtn;
    private ImageButton previewBtn;
    private LinearLayout findLayout;
    private LinearLayout replaceLayout;
    private Button replaceShowButton;
    private EditText findEditText;
    private EditText replaceEditText;
    private AsyncSearch mAsyncSearch;
    private SymbolGrid mSymbolGrid;
    private SharedPreferences mPref;
    private TabHost mTabHost;

    // 打开文件浏览器后的回调操作
    private Runnable fileBrowserCallbackRunnable = new Runnable() {

        @Override
        public void run()
        {

        }
    };
    private HorizontalScrollView toolbar;
    private Drawable undo_can_drawable;
    private Drawable undo_no_drawable;
    private Drawable redo_can_drawable;
    private Drawable redo_no_drawable;
    private ImageButton last_edit_back;
    private ImageButton last_edit_forward;
    private Drawable last_edit_back_d;
    private Drawable last_edit_back_s;
    private Drawable last_edit_forward_d;
    private Drawable last_edit_forward_s;
    private JecMenu mMenu;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
        }catch (Exception e)
        {

        }
        
        mTabHost = (TabHost) findViewById(R.id.tabs);
        mTabHost.initTabHost(this);
        mTabHost.addTab("");
        mEditText = mTabHost.getCurrentEditText();
        findLayout = (LinearLayout) findViewById(R.id.findlinearLayout);
        replaceLayout = (LinearLayout) findViewById(R.id.replace_linearLayout);
        replaceShowButton = (Button) findViewById(R.id.show_replace_button);
        findEditText = (EditText) findViewById(R.id.find_editText);
        replaceEditText = (EditText) findViewById(R.id.replace_editText);
        previewBtn = (ImageButton) findViewById(R.id.preview);
        toolbar = (HorizontalScrollView) findViewById(R.id.toolbar);
        last_edit_back = (ImageButton) findViewById(R.id.last_edit_back);
        last_edit_forward = (ImageButton) findViewById(R.id.last_edit_forward);
        undo_can_drawable = getResources().getDrawable(R.drawable.undo_sel2);
        undo_no_drawable = getResources().getDrawable(R.drawable.undo_no2);
        redo_can_drawable = getResources().getDrawable(R.drawable.redo_sel2);
        redo_no_drawable = getResources().getDrawable(R.drawable.redo_no2);
        // last edit button
        last_edit_back_d = getResources().getDrawable(R.drawable.back_edit_location_d2);
        last_edit_back_s = getResources().getDrawable(R.drawable.back_edit_location_s2);
        last_edit_forward_d = getResources().getDrawable(R.drawable.forward_edit_location_d2);
        last_edit_forward_s = getResources().getDrawable(R.drawable.forward_edit_location_s2);
        // 设置横屏时不全屏编辑
        findEditText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        replaceEditText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        // 一些android 3.0设备没有菜单按钮， 要特殊处理
        /**
         *  Android 4.0, 4.0.1, 4.0.2    14  ICE_CREAM_SANDWICH
         *  Android 3.2     13  HONEYCOMB_MR2   
         *  Android 3.1.x   12  HONEYCOMB_MR1
         *  Android 3.0.x   11
         */
        mMenu = new JecMenu(JecEditor.this);
        mMenu.setOnMenuItemSelectedListener(mOnMenuItemSelectedListener);
        //尽量在平板电脑上才显示菜单按钮
        boolean showMenu = android.os.Build.VERSION.SDK_INT > 10;
        ImageButton menuButton = (ImageButton) findViewById(R.id.menu);
        if(showMenu)
        {
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v)
                {
                    closeOptionsMenu();
                    openOptionsMenu();
                    mMenu.show();
                }
            });
        }
        // end
        mPref = PreferenceManager.getDefaultSharedPreferences(this);

        // 确保顺序没错
        mAsyncSearch = new AsyncSearch();
        
        //需要先处理高亮大小
        init_highlight();
        
        // 最后编辑按钮事件
        mTabHost.setOnTextChangedListener(new JecEditText.OnTextChangedListener() {
            
            @Override
            public void onTextChanged(JecEditText editText)
            {
                onEditLocationChanged(editText);
                if(editText.canUndo())
                {
                    undoBtn.setImageDrawable(undo_can_drawable);
                }else
                {
                    undoBtn.setImageDrawable(undo_no_drawable);
                }
                if(editText.canRedo())
                {
                    redoBtn.setImageDrawable(redo_can_drawable);
                }else
                {
                    redoBtn.setImageDrawable(redo_no_drawable);
                }
            }
        });

        //标签切换事件
        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
            
            @Override
            public void onTabChanged(int tabId)
            {
                mEditText = mTabHost.getCurrentEditText();
                String name = Highlight.getNameByExt(mEditText.getCurrentFileExt());
                switchPreviewButton(name);
            }
        });
        
        mTabHost.setOnTabCloseListener(new OnTabCloseListener() {
            
            @Override
            public boolean onTabClose(final int tabId)
            {
                confirm_save(new Runnable() {
                    @Override
                    public void run()
                    {
                        mTabHost.closeTab(tabId);
                    }
                });
                return false;
            }
        });
        
        last_edit_back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v)
            {
                if(mEditText.isCanBackEditLocation())
                {
                    mEditText.gotoBackEditLocation();
                    onEditLocationChanged(mEditText);
                }else
                {
                    Toast.makeText(JecEditor.this, R.string.not_need_back, Toast.LENGTH_LONG).show();
                }
            }
        });
        last_edit_forward.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v)
            {
                if(mEditText.isCanForwardEditLocation())
                {
                    mEditText.gotoForwardEditLocation();
                    onEditLocationChanged(mEditText);
                }else
                {
                    Toast.makeText(JecEditor.this, R.string.not_need_forward, Toast.LENGTH_LONG).show();
                }
            }
        });

        // 添加工具栏按钮
        mSymbolGrid = (SymbolGrid) findViewById(R.id.symbolGrid1);// new
                                                                  // SymbolGrid(this);
        mSymbolGrid.setClickListener(new OnSymbolClickListener() {

            @Override
            public void OnClick(String symbol)
            {
                insert_text(symbol);
            }
        });

        /*
         * RelativeLayout mainLayout =
         * (RelativeLayout)findViewById(R.id.mainLayout);
         * mainLayout.addView(mSymbolGrid);
         * mainLayout.bringChildToFront(mSymbolGrid);
         */
        // 设置符号图标点击事件
        ImageButton symbolButton = (ImageButton) findViewById(R.id.symbol);
        symbolButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v)
            {
                mSymbolGrid.setVisibility(View.VISIBLE);
            }
        });
        // bind event
        bindEvent();
        // 获取root权限
        try
        {
            if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
            {
                TEMP_PATH = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/.920TextEditor";
            }else
            {
                TEMP_PATH = getFilesDir().getAbsolutePath() + "/.920TextEditor";
            }

            File temp = new File(TEMP_PATH);
            if(!temp.isDirectory() && !temp.mkdir())
            {
                alert(R.string.can_not_create_temp_path);
                // return;
            }
            // 解压语法文件
            String synfilestr = TEMP_PATH + "/version";
            File synsignfile = new File(synfilestr);
            if(!synsignfile.isFile())
            {
                if(!unpackSyntax())
                {
                    alert(R.string.can_not_create_synfile);
                    // return;
                }else
                {
                    FileUtil.writeFile(synfilestr, SYNTAX_SIGN, "utf-8", false);
                }
            }else
            {
                if(!SYNTAX_SIGN.equals(Highlight.readFile(synfilestr, "utf-8")))
                {
                    if(!unpackSyntax())
                    {
                        alert(R.string.can_not_create_synfile);
                        // return;
                    }else
                    {
                        FileUtil.writeFile(synfilestr, SYNTAX_SIGN, "utf-8", false);
                    }
                }
            }

            Highlight.init();

        }catch (Exception e)
        {
            printException(e);
        }
        //显示新版本更新日志
        String prefVer=mPref.getString("version", "-1");
        if(!version.equals(prefVer))
        {
            Help.showChangesLog(this);
            mPref.edit().putString("version", version).commit();
        }
        
        if(isLoading == false)
        {
            // 处理来自其它程序通过Intent来打开文件
            Intent mIntent = getIntent();
            if (mIntent != null 
                    && (Intent.ACTION_VIEW.equals(mIntent.getAction()) 
                    || Intent.ACTION_EDIT.equals(mIntent.getAction())
            )) {
                Uri mUri = mIntent.getData();
                String open_path = mUri != null ? mUri.getPath() : "";
                if(!"".equals(open_path) && open_path != null)
                {
                    readFileToEditText(open_path);
                }
            }else if (mIntent != null && Intent.ACTION_SEND.equals(mIntent.getAction()) && mIntent.getExtras() != null) {
                    Bundle extras = mIntent.getExtras();
                    CharSequence text = extras.getCharSequence(Intent.EXTRA_TEXT);
                    if (text != null) {
                        mEditText.setText2(text.toString());
                    }

            } else if (mIntent != null && ACTION_EDIT_SCRIPT.equals(mIntent.getAction()) && mIntent.getExtras() != null) {
                Bundle extras = mIntent.getExtras();
                String path = extras.getString(EXTRA_SCRIPT_PATH);
                CharSequence contents = extras.getCharSequence(EXTRA_SCRIPT_CONTENT);
                if (contents != null) {
                    mEditText.setText2(contents);
                } else {
                    if (path != null) {
                        readFileToEditText(path);
                    }
                }
            }else
            {
                // 打开上次打开的文件
                if(mPref.getBoolean("open_last_file", false))
                {
                    //在onLoaded处理最后位置
                    
                    SharedPreferences sp = getSharedPreferences(PREF_LAST_FILE, MODE_PRIVATE);
                    Map<String, ?> map = sp.getAll();
                    if(map.size() > 0)
                    {
                        for (Entry<String, ?> entry : map.entrySet())
                        {
                            Object val = entry.getValue();
                            if (val instanceof String) {
                                readFileToEditText((String)val);
                            }
                        }
                    }
                }
            }
        }
    }


    protected void onEditLocationChanged(JecEditText editText)
    {
        if(editText.isCanBackEditLocation())
        {
            last_edit_back.setImageDrawable(last_edit_back_s);
        }else
        {
            last_edit_back.setImageDrawable(last_edit_back_d);
        }
        if(editText.isCanForwardEditLocation())
        {
            last_edit_forward.setImageDrawable(last_edit_forward_s);
        }else
        {
            last_edit_forward.setImageDrawable(last_edit_forward_d);
        }
    }


    class ColorListener implements ColorPicker.OnColorChangedListener
    {
        @Override
        public void onColorChanged(String key, String color)
        {
            insert_text(color);
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        load_options(); //旋转时会调用 onResume但是不会调用 onCreate
        // 按HOME键后，再点击程序图标恢复程序，不会执行onRestoreInstanceState，所以这里要处理一下
        isLoading = false;
        /*if(!"".equals(mEditText.getPath()))
        {
            setTitle((new File(mEditText.getPath())).getName() + "(" + mEditText.getPath() + ")");
        }*/

    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState)
    {
        isLoading = true;
        super.onSaveInstanceState(savedInstanceState);
        // 自动保存当前文档
        if(autosave && mEditText.isTextChanged())
        {
            save();
            //Toast.makeText(this, R.string.has_autosave, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        try
        {
            isLoading = false;
            super.onRestoreInstanceState(savedInstanceState);
        }catch (Exception e)
        {
            printException(e);
        }
    }
    
    protected void onStop() {
        saveHistory();
        // 自动保存当前文档
        if(autosave && mEditText.isTextChanged())
        {
            save();
            //Toast.makeText(this, R.string.has_autosave, Toast.LENGTH_LONG).show();
        }
        super.onStop();
    }
    
    public JecEditText getEditText()
    {
        return mTabHost.getCurrentEditText();
    }

    public static void printException(Exception e)
    {
        Log.d(TAG, e.getMessage());
    }

    private void load_options()
    {
        isRoot = mPref.getBoolean("get_root", false);
        if(isRoot)
        {
            if(!LinuxShell.isRoot())
            {
                isRoot = false;
                Toast.makeText(this, "Root Fail", Toast.LENGTH_LONG).show();
            }
        }
        
        JecEditText.TOUCH_ZOOM_ENABLED = mPref.getBoolean("touch_zoom", true);
        mHideSoftKeyboard = mPref.getBoolean("hide_soft_Keyboard", false);
        if(mHideSoftKeyboard)
        {
            showIME(false);
        }
        // 设置
        String screen_orientation = mPref.getString("screen_orientation", "auto");
        if("portrait".equals(screen_orientation))
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else if("landscape".equals(screen_orientation))
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        autosave = mPref.getBoolean("autosave", false);
        // 搜索设置
        mAsyncSearch.setIgnoreCase(mPref.getBoolean("search_ignore_case", true));
        mAsyncSearch.setRegExp(mPref.getBoolean("search_regex", false));
        back_button_exit = mPref.getBoolean("back_button_exit", true);
        
        init_highlight();
    }
    
    private void init_highlight()
    {
        Highlight.setEnabled(mPref.getBoolean("enable_highlight", true));
        // kb
        int limitSize;
        try
        {
            limitSize = Integer.valueOf(mPref.getString("highlight_limit", Integer.toString(MAX_HIGHLIGHT_FILESIZE)));
        }catch (Exception e)
        {
            limitSize = MAX_HIGHLIGHT_FILESIZE;
            printException(e);
        }
        Highlight.setLimitFileSize(limitSize);
    }

    public void showIME(boolean show)
    {
        mHideSoftKeyboard = !show;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if(getResources().getConfiguration().hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES)
        {
            show = false;
        }
        if(show)
        { // 显示键盘，即输入法
            int type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            mEditText.setInputType(type);
            if(imm != null)
            {
                imm.showSoftInput(mEditText, 0);
            }
        }else
        { // 隐藏键盘
            mEditText.setRawInputType(0);
            if(imm != null)
            {
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            }
        }
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        int ctrlKeyCode = 8 | 0x1000;
        int keycode = event.getKeyCode();
        // CTRL + KEYDOWN
        int meta = (int)event.getMetaState();
        boolean ctrl = (meta & ctrlKeyCode) != 0 ;
        if(ctrl)
        {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_S )
            {
                save();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                if(isLoading)
                {
                    break;
                }else if(mSymbolGrid.isShown())
                {
                    mSymbolGrid.setVisibility(View.GONE);
                }else if(findLayout.getVisibility() == View.VISIBLE)
                {
                    findLayout.setVisibility(View.GONE);
                    replaceLayout.setVisibility(View.GONE);
                }else if(back_button_exit)
                {
                    confirm_save(new Runnable() {

                        @Override
                        public void run()
                        {
                            JecEditor.this.finish();
                        }
                    });
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if(!hideToolbar)
                {
                    toolbar.setVisibility(View.GONE);
                    hideToolbar = true;
                    Toast.makeText(this, R.string.volume_up_toolbar_msg, Toast.LENGTH_LONG).show();
                }else if(!fullScreen)
                {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    fullScreen = true;
                    Toast.makeText(this, R.string.volume_up_fullscreen_msg, Toast.LENGTH_LONG).show();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if(hideToolbar)
                {
                    toolbar.setVisibility(View.VISIBLE);
                    hideToolbar = false;
                }else if(fullScreen)
                {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    fullScreen = false;
                }
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_SEARCH: // 查找按钮
                if(findLayout.getVisibility() == View.GONE)
                {
                    findLayout.setVisibility(View.VISIBLE);
                }
                find("next");
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void bindEvent()
    {
        ImageButton btnOpen = (ImageButton) findViewById(R.id.btn_open);
        btnOpen.setOnClickListener(onBtnOpenClicked);
        ImageButton btnSave = (ImageButton) findViewById(R.id.btn_save);
        btnSave.setOnClickListener(onBtnSaveClicked);
        bindUndoButtonClickEvent();
        bindRedoButtonClickEvent();

        replaceShowButton.setOnClickListener(replaceShowClickListener);
        // 搜索相关
        ImageButton findNext = (ImageButton) findViewById(R.id.find_next_imageButton);
        ImageButton findBack = (ImageButton) findViewById(R.id.find_back_imageButton);
        findNext.setOnClickListener(findButtonClickListener);
        findBack.setOnClickListener(findButtonClickListener);
        // replace
        Button replaceButton = (Button) findViewById(R.id.replace_button);
        Button replaceAllButton = (Button) findViewById(R.id.replace_all_button);
        replaceButton.setOnClickListener(replaceClickListener);
        replaceAllButton.setOnClickListener(replaceClickListener);

        previewBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v)
            {
                if("".equals(mEditText.getPath()) || mEditText.isTextChanged())
                {
                    Toast.makeText(JecEditor.this, R.string.preview_msg, Toast.LENGTH_LONG).show();
                    return;
                }
                try
                {
                    Uri uri = Uri.fromFile(new File(mEditText.getPath()));
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "text/html");
                    startActivity(intent);
                }catch (Exception e)
                {
                    
                }
                
            }
        });
        ImageButton colorButton = (ImageButton) findViewById(R.id.color);
        colorButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ColorPicker cp = new ColorPicker(JecEditor.this, new ColorListener(), "edittext", JecEditor.this.getString(R.string.insert_color), Color.GREEN);
                cp.show();
            }
        });
    }

    /**
     * 查找
     * 
     * @param direction
     *            next or back
     */
    public void find(String direction)
    {
        String keyword = findEditText.getText().toString();
        if("".equals(keyword))
        {
            return;
        }
        if("back".equals(direction))
        {
            mAsyncSearch.search(keyword, false, JecEditor.this);
        }else
        {
            mAsyncSearch.search(keyword, true, JecEditor.this);
        }
    }

    private OnClickListener findButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v)
        {
            switch(v.getId())
            {
                case R.id.find_next_imageButton:
                    find("next");
                    break;
                case R.id.find_back_imageButton:
                    find("back");
                    break;
            }
        }
    };

    private OnClickListener replaceClickListener = new OnClickListener() {

        @Override
        public void onClick(View v)
        {
            String searchText = findEditText.getText().toString();
            String replaceText = replaceEditText.getText().toString();
            if("".equals(searchText))
            {
                return;
            }
            switch(v.getId())
            {
                case R.id.replace_button:
                    mAsyncSearch.replace(replaceText);
                    break;
                case R.id.replace_all_button:
                    mAsyncSearch.replaceAll(searchText, replaceText, JecEditor.this);
                    break;
            }
        }
    };

    private OnClickListener replaceShowClickListener = new OnClickListener() {

        @Override
        public void onClick(View v)
        {
            replaceLayout.setVisibility(View.VISIBLE);
            v.setVisibility(View.GONE);
            replaceEditText.requestFocus();
        }
    };

    private void bindUndoButtonClickEvent()
    {
        undoBtn = (ImageButton) findViewById(R.id.undo);
        // undoBtn.
        undoBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View paramView)
            {
                mEditText.unDo();
            }
        });
    }

    private void bindRedoButtonClickEvent()
    {
        redoBtn = (ImageButton) findViewById(R.id.redo);
        redoBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View paramView)
            {
                mEditText.reDo();
            }
        });

    }


    public void scrollToTop()
    {
        mEditText.scrollTo(0, 0);
    }

    /**
     * 警告并退出程序
     */
    public void alert(int msg)
    {
        new AlertDialog.Builder(this).setMessage(msg).setPositiveButton(R.string.yes, // 保存
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        JecEditor.this.finish();
                    }
                }).show();
    }

    /**
     * 是否要进行保存
     * 
     * @return 返回true则需要保存
     */
    public void confirm_save(final Runnable mRunnable)
    {
        if(!mEditText.isTextChanged())
        {// 内容没有改变
            mRunnable.run();
            return;
        }
        new AlertDialog.Builder(this).setTitle(R.string.save_changes).setMessage(R.string.confirm_save).setPositiveButton(R.string.yes, // 保存
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if("".equals(mEditText.getPath()))
                        {
                            openFileBrowser(FILE_BROWSER_SAVEAS_CODE, getString(R.string.new_filename), mRunnable);
                            return;
                        }
                        save();
                        dialog.dismiss();
                        mRunnable.run();
                    }
                }).setNeutralButton(R.string.no, // 放弃
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        mRunnable.run();
                    }
                }).setNegativeButton(R.string.cancel, // 取消
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                }).show();

    }

    private void save()
    {
        if("".equals(mEditText.getPath()) || isLoading)
            return;
        String content = mEditText.getString();
        boolean ok = FileUtil.writeFile(mEditText.getPath(), content, mEditText.getEncoding(), isRoot);
        if(ok)
        {
            mEditText.setTextFinger();
            Toast.makeText(JecEditor.this, R.string.save_succ, Toast.LENGTH_LONG).show();
        }else
        {
            Toast.makeText(JecEditor.this, R.string.save_failed, Toast.LENGTH_LONG).show();
        }
    }

    private OnClickListener onBtnSaveClicked = new OnClickListener() {

        @Override
        public void onClick(View v)
        {
            if("".equals(mEditText.getPath()))
            {
                openFileBrowser(FILE_BROWSER_SAVEAS_CODE, "Untitled.txt");
                return;
            }
            save();
        }
    };

    private OnClickListener onBtnOpenClicked = new OnClickListener() {

        @Override
        public void onClick(View v)
        {
            openFileBrowser(FILE_BROWSER_OPEN_CODE, "");
        }
    };
    

    private void openFileBrowser(int mode, String filename)
    {
        openFileBrowser(mode, filename, new Runnable() {
            @Override
            public void run()
            {
            }
        });
    }

    /**
     * 打开文件浏览器
     * 
     * @param mode
     *            0打开， 1保存模式
     * @param filename
     * @param mRunnable
     */
    private void openFileBrowser(int mode, String filename, Runnable mRunnable)
    {
        fileBrowserCallbackRunnable = mRunnable;
        Intent intent = new Intent();
        intent.putExtra("filename", filename);
        intent.putExtra("mode", mode);
        intent.putExtra("isRoot", isRoot);
        intent.setClass(JecEditor.this, FileBrowser.class);
        startActivityForResult(intent, mode);
    }

    /**
     * startActivityForResult回调函数
     * 
     * @param requestCode
     *            这里的requestCode就是前面启动新Activity时的带过去的requestCode
     * @param resultCode
     *            resultCode则关联上了setResult中的resultCode
     * @param data
     *            返回的Intent参数
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(RESULT_OK != resultCode)
        {
            return;
        }
        
        final String path;
        switch(requestCode)
        {
            case FILE_BROWSER_OPEN_CODE: // 打开
                path = data.getStringExtra("file");
                int lineBreak = data.getIntExtra("linebreak", 0);
                int encoding = data.getIntExtra("encoding", 0);
                String charset;
                if(encoding < 1)
                {
                    charset = "";
                } else {
                    charset = EncodingList.list[encoding];
                }
                readFileToEditText(path, charset, lineBreak);
                break;
            case FILE_BROWSER_SAVEAS_CODE:
                isLoading = false;
                path = data.getStringExtra("file");
                final File file = new File(path);
                if(file.exists())
                {
                    new AlertDialog.Builder(this).setMessage(getText(R.string.overwrite_confirm)).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mEditText.setPath(path);
                            setTitle(file.getName());
                            save();
                        }
                    }).setNegativeButton(android.R.string.no, null).show();
                }else
                {
                    mEditText.setPath(path);
                    setTitle(file.getName());
                    save();
                }

                break;
        }
        fileBrowserCallbackRunnable.run();
    }

    public void onLoaded()
    {
        String msg = getString(R.string.encoding) + ": " + mEditText.getEncoding();
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        mEditText.resetUndoStatus();
        String filename = new File(mEditText.getPath()).getName();
        setTitle(filename);
        String name = Highlight.getNameByExt(mEditText.getCurrentFileExt());
        switchPreviewButton(name);
        //记住最后位置
        SharedPreferences sp = getSharedPreferences(PREF_HISTORY, MODE_PRIVATE);
        String[] selinfo = sp.getString(mEditText.getPath(), "").split(",");
        if(selinfo.length >= 3)
        {
            mEditText.setSelection(Integer.valueOf(selinfo[0]), Integer.valueOf(selinfo[1]));
        }
        //注意顺序
        saveHistory();
    }

    public void setTitle(String title)
    {
        super.setTitle(title);
        mTabHost.setTitle(title);
    }


/*    public void removeHighlight()
    {
        Editable text = text_content.getText();
        text.clearSpans();
        // 重新设置文本，不然会产生无法滚动和光标不闪烁或光标不可见的问题
        text_content.setText(text);
        text_content.invalidate();
    }*/

    /**
     * 解压语法配置文件
     * 
     * @return
     */
    public boolean unpackSyntax()
    {
        try
        {
            InputStream is = getAssets().open("syntax.zip");
            ZipInputStream zin = new ZipInputStream(is);
            ZipEntry ze = null;
            String name;
            File file;
            while ((ze = zin.getNextEntry()) != null)
            {
                name = ze.getName();
                // Log.v("Decompress", "Unzipping " + name);

                if(ze.isDirectory())
                {
                    file = new File(TEMP_PATH + File.separator + name);
                    if(!file.exists())
                    {
                        if(!file.mkdir())
                        {
                            return false;
                        }
                    }
                }else
                {
                    FileOutputStream fout = new FileOutputStream(TEMP_PATH + File.separator + name);
                    byte[] buf = new byte[1024 * 4];
                    int len;
                    while ((len = zin.read(buf)) > 0)
                    {
                        fout.write(buf, 0, len);
                    }
                    buf = null;
                    zin.closeEntry();
                    fout.close();
                }

            }
            zin.close();
        }catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void switchPreviewButton(String type)
    {
        if(type.toUpperCase().startsWith("HTML"))
        {
            previewBtn.setVisibility(View.VISIBLE);
        }else
        {
            previewBtn.setVisibility(View.GONE);
        }
    }

    public void setEncoding(String encoding)
    {
        try
        {
            byte[] bytes = mEditText.getString().getBytes(mEditText.getEncoding());
            mEditText.setText2(new String(bytes, encoding));
            mEditText.setEncoding(encoding);
            //doHighlight(mEditText.getCurrentFileExt());
        }catch (UnsupportedEncodingException e)
        {
            printException(e);
        }
    }
    
    public void readFileToEditText(String path)
    {
        SharedPreferences sp = getSharedPreferences(PREF_HISTORY, MODE_PRIVATE);
        String[] selinfo = sp.getString(path, "").split(",");
        int linebreak=0;
        String encoding = "";
        if(selinfo.length >= 5)
        {
            linebreak = Integer.valueOf(selinfo[3]);
            encoding = selinfo[4];
        }
        readFileToEditText(path, encoding, linebreak);
    }

    public void readFileToEditText(String path, String encoding, int lineBreak)
    {
        if("".equals(path))
            return;
        // text_content.setText("");
        // text_content.resetUndoStatus();
        //current_path_tmp = path;
        //current_ext_tmp = FileUtil.getExt(path);
        mTabHost.addTab(path);
        new AsyncReadFile(JecEditor.this, path, encoding, lineBreak);
        // String content = FileUtil.Read(path, encoding);
        // text_content.setText(content);
        // saveHistory();
    }

    public void insert_text(String text)
    {
        if(mEditText == null)
            return;
        int start = mEditText.getSelectionStart();
        int end = mEditText.getSelectionEnd();
        /*
         * Editable mEditable = text_content.getText(); mEditable.insert(start,
         * text); int j = text_content.getSelectionStart() - 1;
         * text_content.setSelection(j);
         */
        mEditText.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());

    }

    /**
     * EditText菜单
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v.getId() == mEditText.getId())
        {
            MenuHandler handler = new MenuHandler();
            // 跳转到指定行
            menu.add(0, R.id.go_to_begin, 0, R.string.go_to_begin).setOnMenuItemClickListener(handler);
            // 跳转到指定行
            menu.add(0, R.id.go_to_end, 0, R.string.go_to_end).setOnMenuItemClickListener(handler);
            // 跳转到指定行
            menu.add(0, R.id.goto_line, 0, R.string.goto_line).setOnMenuItemClickListener(handler);
            // 转为小写
            menu.add(0, R.id.to_lower, 0, R.string.to_lower).setOnMenuItemClickListener(handler);
            // 转为大写
            menu.add(0, R.id.to_upper, 0, R.string.to_upper).setOnMenuItemClickListener(handler);
            // 插入时间
            menu.add(0, R.id.insert_datetime, 0, getString(R.string.insert_datetime)+TimeUtil.getDate()).setOnMenuItemClickListener(handler);
            if(mHideSoftKeyboard)
            {
                // 显示输入法
                menu.add(0, R.id.show_ime, 0, R.string.show_ime).setOnMenuItemClickListener(handler);
            } else {
                // 隐藏输入法
                menu.add(0, R.id.hide_ime, 0, R.string.hide_ime).setOnMenuItemClickListener(handler);
            }
            

        }
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener
    {
        public boolean onMenuItemClick(MenuItem item)
        {
            int itemId = item.getItemId();
            switch(itemId)
            {
                case R.id.show_ime:
                    showIME(true);
                    break;
                case R.id.hide_ime:
                    showIME(false);
                    break;
                case R.id.to_lower:
                case R.id.to_upper:
                    int start = mEditText.getSelectionStart();
                    int end = mEditText.getSelectionEnd();
                    if(start == end)
                        break;
                    try
                    {
                        Editable mText = mEditText.getText();
                        char[] dest = new char[end - start];
                        mText.getChars(start, end, dest, 0);
                        if(itemId == R.id.to_lower)
                        {
                            mText.replace(start, end, (new String(dest)).toLowerCase());
                        }else
                        {
                            mText.replace(start, end, (new String(dest)).toUpperCase());
                        }
                    }catch (Exception e)
                    {
                        printException(e);
                    }
                    break;
                case R.id.goto_line:
                    final EditText lineEditText = new EditText(JecEditor.this);
                    lineEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    AlertDialog.Builder builder = new AlertDialog.Builder(JecEditor.this);
                    builder.setTitle(R.string.goto_line).setView(lineEditText).setNegativeButton(android.R.string.cancel, null);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            try
                            {
                                CharSequence lineCharSequence = lineEditText.getText();
                                int line = Integer.valueOf(lineCharSequence.toString());
                                if(!mEditText.gotoLine(line))
                                {
                                    Toast.makeText(JecEditor.this, R.string.can_not_gotoline, Toast.LENGTH_LONG).show();
                                }else
                                {
                                    dialog.dismiss();
                                }
                            }catch (Exception e)
                            {
                                printException(e);
                            }
                        }
                    });
                    builder.show();
                case R.id.go_to_begin:
                    mEditText.setSelection(0, 0);
                    break;
                case R.id.go_to_end:
                    int len = mEditText.getText().length();
                    mEditText.setSelection(len, len);
                    break;
                case R.id.insert_datetime:
                    insert_text(TimeUtil.getDate());
                    break;
            }

            return true; // true表示完成当前item的click处理，不再传递到父类处理
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Toast.makeText(this, "onCreateOptionsMenu", Toast.LENGTH_LONG).show();
        //必须有菜单被创建，不然点menu按钮，只弹出一次菜单
        getMenuInflater().inflate(R.menu.main, menu);

        mMenu.show();
        return true;
    }
    
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        //Toast.makeText(this, "onMenuOpened", Toast.LENGTH_LONG).show();
        mMenu.show();
        return false;// 返回为true 则显示系统menu
    }

    private OnMenuItemSelectedListener mOnMenuItemSelectedListener = new OnMenuItemSelectedListener() {
        
        @Override
        public boolean onMenuItemSelected(int id, View v)
        {
            switch(id)
            {
                case R.id.menu_reopen:
                    new HistoryList(JecEditor.this);
                    break;
                case R.id.menu_highlight:
                    new LangList(JecEditor.this);
                    break;
                case R.id.menu_encoding:
                    new EncodingList(JecEditor.this);
                    break;
                case R.id.menu_saveas:
                    openFileBrowser(1, "".equals(mEditText.getPath()) ? "" : (new File(mEditText.getPath())).getName());
                    break;
                case R.id.menu_search_replace:
                    findLayout.setVisibility(View.VISIBLE);
                    replaceShowButton.setVisibility(View.VISIBLE);
                    break;
                case R.id.menu_pipe:
                    final String [] items=new String []{
                            getString(R.string.view)
                            ,getString(R.string.share)
                            //,getString(R.string.run_script)
                         };
                    AlertDialog.Builder builder=new AlertDialog.Builder(JecEditor.this);
                    builder.setTitle(R.string.open_mode);
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent   = new Intent();
                            if(getString(R.string.view).equals(items[which]))
                            {//view
                                String file = mEditText.getPath();
                                if("".equals(file))
                                {
                                    Toast.makeText(JecEditor.this, R.string.preview_msg, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Uri uri = Uri.parse("file://"+file);
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(uri, "*/*");
                            }else if(getString(R.string.share).equals(items[which])) {
                                //send text
                                intent.setAction(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                int selstart = mEditText.getSelectionStart();
                                int selend = mEditText.getSelectionEnd();
                                String text;
                                if(selend != selstart)
                                {
                                    //has selection text
                                    text=mEditText.getText().subSequence(selstart, selend).toString();
                                } else {
                                    text=mEditText.getString();
                                }
                                intent.putExtra(Intent.EXTRA_TEXT, text);
                            }/*else if(getString(R.string.run_script).equals(items[which])) {
                                String file = mEditText.getPath();
                                if("".equals(file))
                                {
                                    Toast.makeText(JecEditor.this, R.string.preview_msg, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                intent.setAction("com.googlecode.android_scripting.action.LAUNCH_BACKGROUND_SCRIPT");
                                intent.setClassName("com.googlecode.android_scripting", "com.googlecode.android_scripting.activity.ScriptingLayerServiceLauncher");
                                intent.putExtra("com.googlecode.android_scripting.extra.SCRIPT_PATH", file);
                            }*/
                            try
                            {
                                startActivity(intent);
                            }catch (Exception e)
                            {
                                Toast.makeText(JecEditor.this, "Exception: "+e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    builder.show();

                    break;
                case R.id.menu_preferences:
                    Intent intent = new Intent(JecEditor.this, Options.class);
                    startActivity(intent);
                    break;
                case R.id.menu_exit:
                    confirm_save(new Runnable() {

                        @Override
                        public void run()
                        {
                            JecEditor.this.finish();
                        }
                    });
                    break;
            }
            return true;
        }
    };

    private void saveHistory()
    {
        SharedPreferences sp;
        Editor editor;
        if(mEditText.getPath() != null && !"".equals(mEditText.getPath()))
        {
            int selstart = mEditText.getSelectionStart();
            int selend = mEditText.getSelectionEnd();

            sp = getSharedPreferences(PREF_HISTORY, MODE_PRIVATE);
            editor = sp.edit();
            editor.putString(mEditText.getPath(), String.format("%d,%d,%d,%d,%s", selstart, selend, System.currentTimeMillis(), mEditText.getLineBreak(), mEditText.getEncoding()));
            editor.commit();
        }
        
        sp = getSharedPreferences(PREF_LAST_FILE, MODE_PRIVATE);
        editor = sp.edit();
        editor.clear();
        //mPref.edit().putString("last_file", mEditText.getPath()).commit();
        ArrayList<String> paths = mTabHost.getAllPath();
        for(String path : paths)
        {
            editor.putString(path, path);
        }
        editor.commit();
    }

}
