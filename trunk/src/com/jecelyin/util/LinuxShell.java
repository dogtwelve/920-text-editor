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

package com.jecelyin.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mozilla.universalchardet.UniversalDetector;

import android.util.Log;

public class LinuxShell
{
    public static String getCmdPath(String path)
    {
        try {
            UniversalDetector detector;
            path = path.replace(" ", "\\ ");
            detector = new UniversalDetector();
            byte[] buf;
            buf = path.getBytes();
            detector.handleData(buf, 0, buf.length);
            detector.dataEnd();
            String encoding = detector.getCharset();
            detector.reset();
            detector.destroy();
            Log.v("CMD PATH", "encoding: "+encoding+" path:"+path);
            if (encoding == null)
            {
                // 默认为utf-8
                encoding = "UTF-8";
            } else if ("GB18030".equals(encoding))
            {
                // 转换下,不然无法正确解码
                encoding = "GBK";
            }
            return "UTF-8".equals(encoding) ? path : new String(path.getBytes(encoding), "utf-8");
        }catch (Exception e) {
            return path;
        }
    }
    
    /**
     * 返回执行完成的结果
     * @param cmd 命令内容
     * @return
     */
    public static BufferedReader execute(String cmd)
    {
        BufferedReader reader = null; //errReader = null;
        try
        {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            //os.writeBytes("mount -oremount,rw /dev/block/mtdblock3 /system\n");
            //os.writeBytes("busybox cp /data/data/com.koushikdutta.superuser/su /system/bin/su\n");
            os.writeBytes(cmd+"\n");
            os.writeBytes("exit\n");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String err = (new BufferedReader(new InputStreamReader(process.getErrorStream()))).readLine();
            os.flush();

            if(process.waitFor() != 0 || (!"".equals(err) && null != err))
            {
                Log.e("920TERoot", err);
                return null;
            }
            return reader;
        }catch (IOException e)
        {
            e.printStackTrace();
        }catch (InterruptedException e)
        {
            e.printStackTrace();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static boolean isRoot()
    {
        boolean retval = false;
        Process suProcess;
        
        try
        {
          suProcess = Runtime.getRuntime().exec("su");
          
          DataOutputStream os = 
              new DataOutputStream(suProcess.getOutputStream());
          DataInputStream osRes = 
              new DataInputStream(suProcess.getInputStream());
          
          if (null != os && null != osRes)
          {
            // Getting the id of the current user to check if this is root
            os.writeBytes("id\n");
            os.flush();

            String currUid = osRes.readLine();
            boolean exitSu = false;
            if (null == currUid)
            {
              retval = false;
              exitSu = false;
              Log.d("ROOT", "Can't get root access or denied by user");
            }
            else if (true == currUid.contains("uid=0"))
            {
              retval = true;
              exitSu = true;
              Log.d("ROOT", "Root access granted");
            }
            else
            {
              retval = false;
              exitSu = true;
              Log.d("ROOT", "Root access rejected: " + currUid);
            }

            if (exitSu)
            {
              os.writeBytes("exit\n");
              os.flush();
            }
          }
        }
        catch (Exception e)
        {
          // Can't get root !
          // Probably broken pipe exception on trying to write to output
          // stream after su failed, meaning that the device is not rooted
          
          retval = false;
          Log.d("ROOT", "Root access rejected [" +
                e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;
    }
    
/*    public static Process exec(String cmd)
    {
        try
        {
            return Runtime.getRuntime().exec(cmd);
        }catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }*/
}


