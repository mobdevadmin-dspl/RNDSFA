package com.datamation.rndsfa.viewmodel.upload;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.datamation.rndsfa.viewmodel.controller.DayNPrdHedController;
import com.datamation.rndsfa.viewmodel.helpers.NetworkFunctions;
import com.datamation.rndsfa.viewmodel.helpers.UploadTaskListener;
import com.datamation.rndsfa.model.DayNPrdHed;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class UploadNonProd extends AsyncTask<ArrayList<DayNPrdHed>, Integer, ArrayList<DayNPrdHed>> {

    // Shared Preferences variables
    public static final String SETTINGS = "SETTINGS";
    public static SharedPreferences localSP;
    Context context;
    ProgressDialog dialog;
    UploadTaskListener taskListener;
    NetworkFunctions networkFunctions;
    int totalRecords;

    public UploadNonProd(Context context, UploadTaskListener taskListener) {

        this.context = context;
        this.taskListener = taskListener;

        //localSP = context.getSharedPreferences(SETTINGS, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        localSP = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE + Context.MODE_PRIVATE);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(context);
        dialog.setTitle("Uploading nonproductive records");
        dialog.show();
    }

    @Override
    protected ArrayList<DayNPrdHed> doInBackground(ArrayList<DayNPrdHed>... params) {

        int recordCount = 0;
        publishProgress(recordCount);
        networkFunctions = new NetworkFunctions(context);

        ArrayList<DayNPrdHed> RCSList = params[0];
        totalRecords = RCSList.size();

        final String sp_url = localSP.getString("URL", "").toString();
        String URL = "http://" + sp_url;
        Log.v("## Json ##", URL);

        for (DayNPrdHed c : RCSList) {

            try {
                List<String> List = new ArrayList<String>();
                String sJsonHed = new Gson().toJson(c);
                List.add(sJsonHed);

                Log.d("&", "doInBackground: "+networkFunctions.syncNonProductive());
                boolean bStatus = NetworkFunctions.mHttpManager(networkFunctions.syncNonProductive(),List.toString());

                if (bStatus) {
                    c.setNONPRDHED_IS_SYNCED("1");
                } else {
                    c.setNONPRDHED_IS_SYNCED("0");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            ++recordCount;
            publishProgress(recordCount);
        }

        return RCSList;
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        dialog.setMessage("Uploading.. Non Prod Record " + values[0] + "/" + totalRecords);
    }

    @Override
    protected void onPostExecute(ArrayList<DayNPrdHed> NonPrdList) {

        super.onPostExecute(NonPrdList);
        List<String> list = new ArrayList<>();

        if (NonPrdList.size() > 0) {
            list.add("\nNONPRODUCTIVE");
            list.add("------------------------------------\n");
        }
        int i = 1;
        for (DayNPrdHed c : NonPrdList) {

            new DayNPrdHedController(context).updateIsSynced(c);

            if (c.getNONPRDHED_IS_SYNCED().equals("1")) {
                list.add(i + ". " + c.getNONPRDHED_REFNO()+ " --> Success\n");
            } else {
                list.add(i + ". " + c.getNONPRDHED_REFNO() + " --> Failed\n");
            }
            i++;
        }

        dialog.dismiss();
        taskListener.onTaskCompleted(list);
    }

}
