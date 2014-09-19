package de.ox539.ben.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

class UpdateTask extends AsyncTask<Void, Void, Boolean> {
    static final String TAG = "DownloadTask";
    static final String MENU_URL = "http://www.cafebleu.n-group.de/php/Hammerplan.php";
    MenuActivity activity;
    long startTime;

    UpdateTask(MenuActivity a) {
        activity = a;
        startTime = new Date().getTime();
    }

    long getAge() {
        long now = new Date().getTime();

        return startTime - now;
    }

    @Override
    protected Boolean doInBackground(Void... v) {
        try {
            Document doc = Jsoup.connect(MENU_URL).get();
            Elements rows = doc.select("#tableborder > tbody > tr");
            boolean success = false;

            for (Element row : rows) {
                if (row.select(":root > th").size() > 0) {
                    // header row, welp
                    continue;
                }
                Elements cols = row.select(":root > td");
                if (cols.size() != 2) {
                    Log.w(TAG, "row doesn't have two cols: " + row.outerHtml());
                    continue;
                }
                Element dateCol = cols.get(0);
                Element menuCol = cols.get(1);
                String[] dateAndDay = dateCol.text().split("\\s");
                if (dateAndDay.length < 2) {
                    Log.w(TAG, "date has not enough lines: " + dateCol.text());
                    continue;
                }
                String dateStr = dateAndDay[0];

                Elements meals = menuCol.select(":root > table > tbody > tr > td > .txt");
                if (meals.size() < 2) {
                    Log.w(TAG, "meals column has not enough rows: " + meals.outerHtml());
                    continue;
                }
                DailyMenu menu = new DailyMenu();
                try {
                    menu.date = MenuActivity.DD_MM_YYYY_FORMAT.parse(dateStr);
                } catch (ParseException e) {
                    Log.w(TAG, "couldn't parse date: " + dateAndDay[0], e);
                    continue;
                }
                menu.meal1 = meals.get(0).text();
                menu.meal2 = meals.get(1).text();

                activity.db.insert(menu);
                success = true; // at least something read
            }

            return success;
        } catch (IOException e) {
            Log.e(TAG, "network error", e);
            return false;
        }
    }

    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(Boolean b) {
        if (b) {
            activity.updateSucceeded();
        } else {
            activity.updateFailed();
        }
    }

}
