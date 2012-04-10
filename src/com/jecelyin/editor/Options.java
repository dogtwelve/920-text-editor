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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.jecelyin.colorschemes.ColorScheme;
import com.jecelyin.util.ColorPicker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.widget.Toast;

public class Options extends PreferenceActivity
{
    private int category;
    private SharedPreferences mSP;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        category = getIntent().getIntExtra("category", R.xml.options);
        addPreferencesFromResource(category);
        mSP = getPreferenceManager().getSharedPreferences();

        switch(category)
        {
            case R.xml.view:
                initView();
                break;
            case R.xml.highlight:
                initHighlight();
                break;
            case R.xml.help:
                initHelp();
                break;
            case R.xml.options:
                init();
                break;
                
            case R.xml.other:
            	initOther();
            	break;
        }
    }

    private void initHelp()
    {

        findPreference("about").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                Intent intent = new Intent(Options.this, About.class);
                startActivity(intent);
                return true;
            }
        });
        findPreference("help").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                Help.showHelp(Options.this);
                return true;
            }
        });
        findPreference("feedback").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                Uri uri;
                try
                {
                    uri = Uri.parse("http://www.jecelyin.com/920report.php?ver=" + URLEncoder.encode(JecEditor.version, "utf-8"));
                }catch (UnsupportedEncodingException e)
                {
                    uri = Uri.parse("http://www.jecelyin.com/920report.php?var=badver");
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                // Intent intent = new Intent(Options.this, Donate.class);
                startActivity(intent);
                return true;
            }
        });

        findPreference("project").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://code.google.com/p/920-text-editor/"));
                startActivity(intent);
                return true;
            }
        });
    }

    private void initHighlight()
    {
        setHighlightEvent("hlc_font", ColorScheme.color_font);
        setHighlightEvent("hlc_backgroup", ColorScheme.color_backgroup);
        setHighlightEvent("hlc_string", ColorScheme.color_string);
        setHighlightEvent("hlc_keyword", ColorScheme.color_keyword);
        setHighlightEvent("hlc_comment", ColorScheme.color_comment);
        setHighlightEvent("hlc_tag", ColorScheme.color_tag);
        setHighlightEvent("hlc_attr_name", ColorScheme.color_attr_name);
        setHighlightEvent("hlc_function", ColorScheme.color_function);

        PreferenceCategory cate = (PreferenceCategory) findPreference("custom_highlight_color");
        cate.setEnabled(mSP.getBoolean("use_custom_hl_color", false));
        CheckBoxPreference uchc = (CheckBoxPreference) findPreference("use_custom_hl_color");
        uchc.setOnPreferenceChangeListener(mOnHighlightChange);
        
        ListPreference csPref = (ListPreference) findPreference("hl_colorscheme");
        String[] csNames = ColorScheme.getSchemeNames();
        if(csNames == null)
            csNames = new String[]{"Default"};
        csPref.setEntries(csNames);
        csPref.setEntryValues(csNames);

    }

    private void setHighlightEvent(final String key, final String def)
    {
        Preference pref = (Preference) findPreference(key);
        pref.setSummary(mSP.getString(key, def));

        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                ColorPicker cp = new ColorPicker(Options.this, new ColorListener(), preference.getKey(), preference.getTitle().toString(), Color.parseColor(preference
                        .getSharedPreferences().getString(key, def)));
                cp.show();
                return true;
            }
        });
    }

    private class ColorListener implements ColorPicker.OnColorChangedListener
    {
        @Override
        public void onColorChanged(String key, String color)
        {
            Preference pref = (Preference) findPreference(key);
            pref.setSummary(color);
            pref.getEditor().putString(key, color).commit();
        }

    }

    private void initView()
    {
        ListPreference fontPf = (ListPreference) findPreference("font");
        String[] fonts = new String[]{ "Normal", "Monospace", "Sans Serif", "Serif" };
        fontPf.setEntries(fonts);
        fontPf.setEntryValues(fonts);
        fontPf.setDefaultValue("Monospace");

        ListPreference fontSizePf = (ListPreference) findPreference("font_size");
        String[] font_size = new String[]{ "10", "12", "13", "14", "16", "18", "20", "22", "24", "26", "28", "32" };
        fontSizePf.setEntries(font_size);
        fontSizePf.setEntryValues(font_size);
        fontSizePf.setDefaultValue("14");
    }

    private void setOptionsPreference(final String key, final int id)
    {
        Preference pref = (Preference) findPreference(key);
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                Intent intent = new Intent(Options.this, Options.class);
                intent.putExtra("category", id);
                startActivity(intent);
                return true;
            }
        });
    }

    private void init()
    {
        setOptionsPreference("opt_view", R.xml.view);
        setOptionsPreference("opt_highlight", R.xml.highlight);
        setOptionsPreference("opt_search", R.xml.search);
        setOptionsPreference("opt_other", R.xml.other);
        setOptionsPreference("opt_help", R.xml.help);
        
        findPreference("donate").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                startActivity(Donate.getWebIntent());
                return true;
            }
        });

        findPreference("clear_history").setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                SharedPreferences sp = getSharedPreferences(JecEditor.PREF_HISTORY, MODE_PRIVATE);
                sp.edit().clear().commit();
                Toast.makeText(getApplicationContext(), R.string.clear_history_ok, Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }
    
    private void initOther()
    {
        ListPreference bbbPref = (ListPreference) findPreference("back_button");
        
        String[] bbbEntry = this.getResources().getStringArray(R.array.back_button_behavior);
        String[] bbbVaule = new String[]
        		{
        			String.valueOf(JecEditor.BACK_BUTTON_BEHAV_EXIT_APP),
        			String.valueOf(JecEditor.BACK_BUTTON_BEHAV_CLOSE_TAB),
        			String.valueOf(JecEditor.BACK_BUTTON_BEHAV_EXIT_TAB),
        			String.valueOf(JecEditor.BACK_BUTTON_BEHAV_UNDO),
        			String.valueOf(JecEditor.BACK_BUTTON_BEHAV_DO_NOTHING)
        		};
        
        bbbPref.setEntries(bbbEntry);
        bbbPref.setEntryValues(bbbVaule);
        bbbPref.setDefaultValue(bbbVaule[0]);
    }

    public static Typeface getFont(String font)
    {
        if("Monospace".equals(font))
            return Typeface.MONOSPACE;
        else if("Sans Serif".equals(font))
            return Typeface.SANS_SERIF;
        else if("Serif".equals(font))
            return Typeface.SERIF;
        return Typeface.DEFAULT;
    }

    private OnPreferenceChangeListener mOnHighlightChange = new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference pref, Object val)
        {
            PreferenceCategory cate = (PreferenceCategory) findPreference("custom_highlight_color");
            cate.setEnabled(val.toString().equals("true") ? true : false);
            return true;
        }
    };

}
