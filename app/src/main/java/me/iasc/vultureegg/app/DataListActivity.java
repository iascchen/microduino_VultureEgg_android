/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/3/19.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.iasc.vultureegg.app;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import me.iasc.vultureegg.app.db.DataBaseHelper;
import me.iasc.vultureegg.app.db.RecordDAO;
import me.iasc.vultureegg.app.db.RecordModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DataListActivity extends ListActivity {
    private static final String TAG = DataListActivity.class.getSimpleName();
    private Tracker mTracker;

    private List<RecordModel> entityList;

    private DataListAdapter mDataListAdapter;

    public RecordDAO recordDAO = null;
    public static final int ROW_LIMIT = 50;
    public static final String DUMP_FILENAME = "/download/vulture_data.csv";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VultureEggApplication application = (VultureEggApplication) getApplication();
        mTracker = application.getDefaultTracker();

        getActionBar().setTitle(getString(R.string.title_data));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        recordDAO = new RecordDAO(this);
        entityList = recordDAO.getDatas(ROW_LIMIT);
        Log.v(TAG, "entity count : " + entityList.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_datas, menu);
        menu.findItem(R.id.menu_dump).setVisible(true);
        menu.findItem(R.id.menu_reset).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_dump:
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                        .setAction("DataDump").build());

                dumpAllDatas();
                break;
            case R.id.menu_reset:
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                        .setAction("DataReset").build());

                clearAllDatas();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initializes list view adapter.
        mDataListAdapter = new DataListAdapter();
        setListAdapter(mDataListAdapter);

        queryMyDatas();

        mTracker.setScreenName("DataListActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataListAdapter.clear();
    }

    private void queryMyDatas() {
        mDataListAdapter.clear();
        entityList = recordDAO.getDatas(ROW_LIMIT);

        for (RecordModel _d : entityList) {
            mDataListAdapter.addData(_d);
        }
        mDataListAdapter.notifyDataSetChanged();

        invalidateOptionsMenu();
    }

    private void clearAllDatas() {
        recordDAO.deleteAll();
        queryMyDatas();
    }

    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String formatTime(String timelong) {
        String ret = null;
        Long l = Long.valueOf(timelong);
        Date date = new Date(l);
        return DATE_FORMAT.format(date);
    }

    // Adapter for holding entitys found through scanning.
    private class DataListAdapter extends BaseAdapter {
        private ArrayList<RecordModel> mDatas;
        private LayoutInflater mInflator;

        public DataListAdapter() {
            super();
            mDatas = new ArrayList<RecordModel>();
            mInflator = DataListActivity.this.getLayoutInflater();
        }

        public void addData(RecordModel entity) {
            if (!mDatas.contains(entity)) {
                mDatas.add(entity);
            }
        }

        public void clear() {
            mDatas.clear();
        }

        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public Object getItem(int i) {
            return mDatas.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_data_item, null);
                viewHolder = new ViewHolder();
                viewHolder.dataTime = (TextView) view.findViewById(R.id.data_time);
                viewHolder.deviceDevId = (TextView) view.findViewById(R.id.device_devid);
                viewHolder.dataValue = (TextView) view.findViewById(R.id.data_value);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            RecordModel entity = mDatas.get(i);
            final String entityTime = entity.getTime();
            final String entityDevId = entity.getDeviceId();
            final String entityValue = entity.getValue();

            if (entityTime != null && entityTime.length() > 0)
                viewHolder.dataTime.setText(formatTime(entityTime));

            viewHolder.dataValue.setText(entityValue);

            if (entityDevId != null && entityDevId.length() > 0)
                viewHolder.deviceDevId.setText(entityDevId);
            else
                viewHolder.deviceDevId.setText(R.string.deviceId);

            return view;
        }
    }

    static class ViewHolder {
        TextView dataTime, deviceDevId, dataValue;
    }

    // Dump Data to file

    private void dumpAllDatas() {
        FileOutputStream output = null;
        boolean suc = false;

        try {
            output = openFile();
            write(output, getCsvHeader());

            List<RecordModel> items = recordDAO.getDatas(RecordDAO.UNLIMIT);
            for (RecordModel _i : items) {
                write(output, _i);
            }
            suc = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null)
                    output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Toast.makeText(getApplicationContext(), "Dump Completed : " + suc, Toast.LENGTH_LONG).show();
    }

    public String getCsvHeader() {
        StringBuilder sb = new StringBuilder();

        sb.append(DataBaseHelper.C_DATA_TIME).append(RecordModel.DUMP_SEPRATOR)
                .append(DataBaseHelper.C_DEV_DEVID);

        for (int i = 0; i < RecordModel.DATA_MAP_KEYS.length; i++) {
            sb.append(RecordModel.DUMP_SEPRATOR).append(RecordModel.DATA_MAP_KEYS[i]);
        }
        sb.append("\n");

        return sb.toString();
    }

    public void write(FileOutputStream output, RecordModel dm) throws IOException {
        if (output != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(dm.getTime()).append(RecordModel.DUMP_SEPRATOR)
                    .append(dm.getDeviceId())
                    .append(dm.getValue()).append("\n");
            write(output, sb.toString());
        }
    }

    public void write(FileOutputStream output, String str) throws IOException {
        Log.v("Dump :", str);
        output.write(str.getBytes());
    }

    public FileOutputStream openFile() throws FileNotFoundException {
        String mediaState = Environment.getExternalStorageState();
        if (mediaState.equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return new FileOutputStream(new File(Environment.getExternalStorageDirectory() + DUMP_FILENAME));
        }
        return null;
    }
}