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
 *   along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jecelyin.highlight;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.jecelyin.colorschemes.ColorScheme;
import com.jecelyin.editor.JecEditor;
import com.jecelyin.util.FileUtil;
import com.jecelyin.util.TimerUtil;

public class Highlight
{
    static {
        System.loadLibrary("highlight");
    }
    
    public static final int GROUP_TAG_ID        = 1;
    public static final int GROUP_COMMENT_ID    = 2;
    public static final int GROUP_STRING_ID     = 3;
    public static final int GROUP_KEYWORD_ID    = 4;
    public static final int GROUP_FUNCTION_ID   = 5;
    public static final int GROUP_ATTR_NAME_ID  = 6;

    
    private final static String TAG = "Highlight";
    private static HashMap<String, String[]> langTab;
    private static ArrayList<String[]> nameTab;
    
    /**
     * 
     * @param openedFile 要解析的文件，绝对路径
     * @param ext 扩展名
     * @return 返回[[高亮类型,开始offset, 结束offset],,]
     */
    public static boolean parse(Spannable textSpannable, String ext)
    {
        String[] lang = langTab.get(ext);
        if(lang == null)
        {
            return false;
        }
        String text = textSpannable.toString();
        TimerUtil.start();
        int[] ret = jni_parse(text, JecEditor.TEMP_PATH + File.separator + lang[1]);
        TimerUtil.stop("hg parse");
        if(ret == null)
        {
            return false;
        }
        int len = ret.length;
        if(len < 1 || len % 3.0F != 0)
        {
            return false;
        }
        //色彩模块
        int color_tag            = Color.parseColor(ColorScheme.color_tag);
        int color_string         = Color.parseColor(ColorScheme.color_string);
        int color_keyword        = Color.parseColor(ColorScheme.color_keyword);
        int color_function       = Color.parseColor(ColorScheme.color_function);
        int color_comment        = Color.parseColor(ColorScheme.color_comment);
        int color_attr_name      = Color.parseColor(ColorScheme.color_attr_name);
        
        TimerUtil.start();
        ForegroundColorSpan color;
        int start;
        int end;
        for(int i=0; i<len; i++)
        {
            
            switch(ret[i])
            {
                case GROUP_TAG_ID:
                    color = new ForegroundColorSpan(color_tag);
                    break;
                case GROUP_STRING_ID:
                    color = new ForegroundColorSpan(color_string);
                    break;
                case GROUP_KEYWORD_ID:
                    color = new ForegroundColorSpan(color_keyword);
                    break;
                case GROUP_FUNCTION_ID:
                    color = new ForegroundColorSpan(color_function);
                    break;
                case GROUP_COMMENT_ID:
                    color = new ForegroundColorSpan(color_comment);
                    break;
                case GROUP_ATTR_NAME_ID:
                    color = new ForegroundColorSpan(color_attr_name);
                    break;
                default:
                    Log.v(TAG, "获取颜色group id失败");
                    return false;
            }
            
            start = ret[++i];
            end   = ret[++i];
            textSpannable.setSpan(color, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ret = null;
        TimerUtil.stop("hg 1");
        return true;
    }
    
    /**
     * [{语法名称,其中一个扩展名},,]
     */
    public static ArrayList<String[]> getLangList()
    {
        return nameTab;
    }
    
    public static boolean loadLang()
    {
        String langfile = JecEditor.TEMP_PATH + "/lang.conf";
        File file = new File(langfile);
        if(!file.isFile())
        {
            return false;
        }
        file = null;
        langTab = new HashMap<String, String[]>();
        nameTab = new ArrayList<String[]>();

        try
        {
            byte[] mByte = readFile(langfile);
            String mData = new String(mByte, "utf-8");
            String[] lines = mData.split("\n");
            String[] cols;
            for(String line:lines)
            {
                line = line.trim();
                if(line.startsWith("#"))
                    continue;
                cols = line.split(":");
                String name = cols[0].trim();
                String synfile = cols[1].trim();
                String extsString = cols[2].trim();
                String[] exts = extsString.split("\\s+");
                nameTab.add(new String[] {name, exts[0]});
                for(String ext:exts)
                {
                    langTab.put(ext, new String[]{name, synfile});
                }
            }
            mByte = null;
        }catch (Exception e)
        {
            return false;
        }
        
        return true;
    }
    
    public static String getNameByExt(String ext)
    {
        String[] info = langTab.get(ext);
        if(info == null)
        {
            return "";
        }
        return info[0];
    }
    
    public static byte[] readFile(String file)
    {
        byte[] ret;
        ret = read_file(file);
        return ret;
    }
    
    public static String readFile(String file, String encoding)
    {
        try
        {
            byte[] mByte = readFile(file);
            return new String(mByte, encoding);
        }catch (Exception e)
        {
            try {
                return FileUtil.ReadFile(file, encoding);
            }catch (Exception e2) {
                return "";
            }
        }
    }

    /**
     * 读取文件
     * @param file
     * @return 返回一个char数组
     */
    private native static byte[] read_file(String file);
    
    private native static int[] jni_parse(String text, String syntaxFile);

}
