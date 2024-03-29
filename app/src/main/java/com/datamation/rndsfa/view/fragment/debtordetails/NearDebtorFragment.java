package com.datamation.rndsfa.view.fragment.debtordetails;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.datamation.rndsfa.R;
import com.datamation.rndsfa.viewmodel.adapter.NearCustomerAdapter;
import com.datamation.rndsfa.viewmodel.controller.CustomerController;
import com.datamation.rndsfa.viewmodel.controller.NearCustomerController;
import com.datamation.rndsfa.view.dialog.CustomProgressDialog;
import com.datamation.rndsfa.viewmodel.helpers.SharedPref;
import com.datamation.rndsfa.model.NearDebtor;
import com.datamation.rndsfa.viewmodel.utils.GPSTracker;

import java.util.ArrayList;

public class NearDebtorFragment extends Fragment {

    View view;
    ListView lvCustomers;
    SharedPref mSharedPref;
    ArrayList<NearDebtor> nearDebList;
    NearCustomerAdapter nearDebAdapter;
    private NearDebtor nDebtor;
    GPSTracker gpsTracker;
    double lati = 0.0;
    double longi = 0.0;
    private String debCode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_near_debtor, container, false);

        lvCustomers = (ListView)view.findViewById(R.id.near_cus_lv);
        mSharedPref = new SharedPref(getActivity());
        debCode = mSharedPref.getSelectedDebCode();

        lvCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view2, int position, long id) {

                int i = android.provider.Settings.Global.getInt(getActivity().getContentResolver(), android.provider.Settings.Global.AUTO_TIME, 0);
                /* If option is selected */
                if (i > 0) {
                    nDebtor = nearDebList.get(position);

                    updateDialog("Update Co-ordinates", "Do you want to update co-ordinates?", nDebtor);
                }
            }
        });

        gpsTracker = new GPSTracker(getActivity());

        if (gpsTracker.canGetLocation())
        {
        //    gpsTracker = new GPSTracker(getActivity());
//            if(!mSharedPref.getGlobalVal("Latitude").equals("") && mSharedPref.getGlobalVal("Longitude").equals(""))
//            {
                lati = Double.parseDouble(mSharedPref.getGlobalVal("Latitude"));
                longi = Double.parseDouble(mSharedPref.getGlobalVal("Longitude"));
                new getNearGPSCustomers().execute();
//            }
//            else
//            {
//                //startActivityForResult(new Intent(Settings.ACTION_LOCALE_SETTINGS), 0);
//            }
        }
        else
        {
            gpsTracker.showSettingsAlert();
        }

        return view;
    }

    private class getNearGPSCustomers extends AsyncTask<String, Integer, Boolean> {
        int totalRecords=0;
        CustomProgressDialog pdialog;
        private String repcode;

        public getNearGPSCustomers(){
            this.pdialog = new CustomProgressDialog(getActivity());
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdialog  = new CustomProgressDialog(getActivity());
            pdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            pdialog.setMessage("Authenticating...");
            pdialog.show();
        }

        @Override
        protected Boolean doInBackground(String... arg0) {

            try
            {
                nearDebList = new NearCustomerController(getActivity()).getAllGPSNearCustomer(lati, longi);

                return true;

            }
            catch (Exception e)
            {
                e.printStackTrace();

                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);

            if (result)
            {
                nearDebAdapter = new NearCustomerAdapter(getActivity(), nearDebList);
                lvCustomers.setAdapter(nearDebAdapter);
            }
            pdialog.setMessage("Finalizing data");
            pdialog.setMessage("Download Completed..");
            if (result) {
                if (pdialog.isShowing()) {
                    pdialog.dismiss();
                }
            } else {
                if (pdialog.isShowing()) {
                    pdialog.dismiss();
                }

            }
        }
    }

    public void updateDialog(String title, String msg, final NearDebtor nDebtor)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage(msg);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setIcon(R.drawable.info11);

        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                updateDebtorCoordinates(debCode, nDebtor);
                Log.d("NEAR_DEB_FRAG","CO_ORDINATES: " + nDebtor.getFNEARDEBTOR_LONGI() + ", " + nDebtor.getFNEARDEBTOR_LATI());
                dialog.cancel();
            }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
        alertD.getWindow().setLayout(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void updateDebtorCoordinates(String debCode, NearDebtor nDebtor)
    {
        if (new CustomerController(getActivity()).updateCustomerLocationByNeDebtor(debCode, nDebtor) >0)
        {
            Toast.makeText(getActivity(), "Customer location updated successfully!!!" , Toast.LENGTH_LONG).show();
            mSharedPref.setGPSUpdated("Y");
        }
        else
        {
            Toast.makeText(getActivity(), "Customer location update failed!!!" , Toast.LENGTH_LONG).show();
        }
        Log.d("NEAR_DEB_FRAG","UPDATE_DEB: " + debCode + ", " + nDebtor.getFNEARDEBTOR_LATI() + ", " + nDebtor.getFNEARDEBTOR_LONGI());
    }
}
