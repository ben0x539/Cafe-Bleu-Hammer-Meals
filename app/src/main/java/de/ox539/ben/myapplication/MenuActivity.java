package de.ox539.ben.myapplication;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;


public class MenuActivity extends Activity {
    static final String TAG = "MenuActivity";
    static final SimpleDateFormat YYYY_MM_DD_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    static final SimpleDateFormat DD_MM_YYYY_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    MenuDbAdapter db;
    UpdateTask updateTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.label_view);
        updateTask = null;

        try {
            db = new MenuDbAdapter(this).open();
        } catch (SQLException e) {
            Log.e(TAG, "sql error", e);
        }

        updateMenu();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_update:
                startUpdate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startUpdate() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            DialogFragment newFragment = new NoNetDialog();
            newFragment.show(getFragmentManager(), "no_net");
            return;
        }

        if (updateTask != null) {
            if (updateTask.getAge() > 30 * 1000)
                return;
            else
                updateTask.cancel(false);
        }
        setContentView(R.layout.label_view);
        View v = findViewById(R.id.label);
        if (v != null) {
            TextView t = (TextView) v;
            t.setText(R.string.label_updating);
        }

        updateTask = new UpdateTask(this);
        updateTask.execute();
    }

    void updateMenu() {
        List<DailyMenu> menus = db.getCurrentMenus();
        ViewGroup menuList = null;
        int dateFormatFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY;
        for (DailyMenu menu : menus) {
            if (menuList == null) {
                setContentView(R.layout.table_view);
                menuList = (ViewGroup) findViewById(R.id.table);
            }
            ViewGroup row = (ViewGroup) getLayoutInflater().inflate(R.layout.menu_entry, menuList, false);
            TextView date = (TextView) row.findViewById(R.id.col_date);
            TextView meal1 = (TextView) row.findViewById(R.id.col_meal1);
            TextView meal2 = (TextView) row.findViewById(R.id.col_meal2);
            date.setText(DateUtils.formatDateTime(this, menu.date.getTime(), dateFormatFlags));
            meal1.setText(menu.meal1);
            meal2.setText(menu.meal2);
            menuList.addView(row);
        }
        if (menuList == null) {
            setContentView(R.layout.label_view);
            View v = findViewById(R.id.label);
            if (v != null) {
                TextView t = (TextView) v;
                t.setText(R.string.label_no_data);
            }
        }
    }

    public void updateFailed() {
        updateTask = null;
        updateMenu();
        DialogFragment newFragment = new UpdateFailureDialog();
        newFragment.show(getFragmentManager(), "update_failed");
    }

    public void updateSucceeded() {
        updateTask = null;
        updateMenu();
    }
}
