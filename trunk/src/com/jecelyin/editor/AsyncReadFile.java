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
import java.io.FileInputStream;

import org.mozilla.universalchardet.UniversalDetector;
import org.mozilla.universalchardet.UniversalDetector.DetectorException;

import com.jecelyin.highlight.Highlight;
import com.jecelyin.util.LinuxShell;
import com.jecelyin.util.TimerUtil;

import android.util.Log;
import android.widget.Toast;

public class AsyncReadFile
{
    private final static String TAG = "AsyncReadFile";
    private JecEditor mJecEditor;
    private String mData;
    private String path = "";
    private String encoding = "";
    private String errorMsg = "";
    private static boolean isRoot = false;
    
    public AsyncReadFile(JecEditor mJecEditor, String path)
    {
        // 加载文件不算改动，不能有撤销操作
        JecEditor.isLoading = true;
        mJecEditor.text_content.requestFocus();
        this.mJecEditor = mJecEditor;
        this.path = path;
        isRoot = JecEditor.isRoot;
        mData = "";

        read();
    }

    private void read()
    {
        String fileString = path;
        File file = new File(fileString);
        fileString = file.getAbsolutePath();
        try
        {
            String tempFile = JecEditor.TEMP_PATH + "/temp.root.file";
            boolean root = false;
            if(!file.canRead() && isRoot)
            {
                //需要Root权限处理
                LinuxShell.execute("cat " + LinuxShell.getCmdPath(fileString) + " > " + LinuxShell.getCmdPath(tempFile));
                LinuxShell.execute("chmod 777 "+LinuxShell.getCmdPath(tempFile));
                fileString = tempFile;
                root = true;
            }
            encoding = getEncoding(fileString);
            mData = Highlight.readFile(fileString, encoding);

            if(root)
            {
                LinuxShell.execute("rm -rf "+LinuxShell.getCmdPath(tempFile));
            }
            finish(1);
        } catch (Exception e)
        {
            mData = "";
            e.printStackTrace();
            errorMsg = e.getMessage();//R.string.exception;
            finish(0);
        } catch (OutOfMemoryError e) {
            mData = "";
            errorMsg = mJecEditor.getString(R.string.out_of_memory);
            finish(0);
        }
    }

    private void finish(int what)
    {
        
        if(!"".equals(errorMsg) && what == 0)
        {//error
            JecEditor.isLoading = false;
            Toast.makeText(mJecEditor, errorMsg, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            if(mData == null)
                return;
            TimerUtil.start();
            //mJecEditor.text_content.setText("");
            mJecEditor.text_content.setText2(mData);
            TimerUtil.stop(TAG+"1");
            // scroll to top
            mJecEditor.text_content.setSelection(0, 0);
            mJecEditor.text_content.clearFocus();
            //mJecEditor.text_content.invalidate();
            mJecEditor.current_encoding_tmp = encoding;

            mJecEditor.onLoaded();

            JecEditor.isLoading = false;
        } catch (OutOfMemoryError e) {
            Toast.makeText(mJecEditor, R.string.out_of_memory, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(mJecEditor, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private String getEncoding(String path)
    {
        byte[] buf = new byte[4096];
        FileInputStream fis;
        try
        {
            fis = new FileInputStream(path);
        } catch (Exception e)
        {
            e.printStackTrace();
            return "UTF-8";
        }

        UniversalDetector detector;
        try
        {
            detector = new UniversalDetector();
        }catch (DetectorException e1)
        {
            e1.printStackTrace();
            return "UTF-8";
        }

        int nread;
        try
        {
            do
            {
                nread = fis.read(buf);
            }while (nread > 0 && detector.handleData(buf, 0, nread) == 0);

            fis.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        detector.dataEnd();
        String encoding = detector.getCharset();
        detector.reset();
        detector.destroy();
        Log.v(TAG, "encoding: "+encoding);
        if (encoding == null)
        {
            // 默认为utf-8
            encoding = "UTF-8";
        } else if ("GB18030".equals(encoding))
        {
            // 转换下,不然无法正确解码
            encoding = "GBK";
        }
        
        return encoding;
    }
}
