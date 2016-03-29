package com.hvzhub.app;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.hvzhub.app.API.API;
import com.hvzhub.app.API.ErrorUtils;
import com.hvzhub.app.API.HvZHubClient;
import com.hvzhub.app.API.NetworkUtils;
import com.hvzhub.app.API.model.APIError;
import com.hvzhub.app.API.model.APISuccess;
import com.hvzhub.app.API.model.Code;
import com.hvzhub.app.API.model.TagPlayerRequest;
import com.hvzhub.app.API.model.Uuid;
import com.hvzhub.app.Prefs.GamePrefs;
import com.hvzhub.app.Prefs.TagLocationPref;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportTagFragment extends Fragment implements DatePickerFragment.OnDateSetListener, TimePickerFragment.OnTimeSetListener {
    EditText submitCode;
    ProgressBar progressBar;
    LinearLayout myCodeContainer;
    LinearLayout errorView;
    TextView TimeInput;
    TextView DateInput;
    TextView LocationInput;
    Calendar tagTime;
    LatLng tagLocation;
    FragmentManager mapManager;
    SharedPreferences.OnSharedPreferenceChangeListener listener;


    private OnLogoutListener mListener;

    public ReportTagFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportTagFragment newInstance() {
        ReportTagFragment fragment = new ReportTagFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle(getActivity().getString(R.string.report_tag));
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_report_tag, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button tagButton = (Button) view.findViewById(R.id.submit_tag);
        tagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryToTag();
            }
        });
        submitCode = (EditText) view.findViewById(R.id.tag_reported);

        DateInput = (TextView) view.findViewById(R.id.date);
        DateInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        TimeInput = (TextView) view.findViewById(R.id.time);
        TimeInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        LocationInput = (TextView) view.findViewById(R.id.location);
        LocationInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start TagMap
                Intent mapstart = new Intent(getActivity(), TagMapActivity.class);
                startActivity(mapstart);
                setLatLng();
            }
        });

        myCodeContainer = (LinearLayout) view.findViewById(R.id.my_code_container);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        errorView = (LinearLayout) view.findViewById(R.id.error_view);

        SharedPreferences prefs = getActivity().getSharedPreferences(TagLocationPref.NAME, 0);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(TagLocationPref.Latitude)){
                    setLatLng();
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    private void tryToTag() {
        int gameId = getActivity().getSharedPreferences(GamePrefs.PREFS_GAME, Context.MODE_PRIVATE).getInt(GamePrefs.PREFS_GAME_ID, -1);
        HvZHubClient client = API.getInstance(getActivity().getApplicationContext()).getHvZHubClient();
        String uuid = getActivity().getSharedPreferences(GamePrefs.PREFS_GAME, Context.MODE_PRIVATE).getString(GamePrefs.PREFS_SESSION_ID, null);
        String tagCode = submitCode.getText().toString();

        Date tagDate = tagTime.getTime();
        TagPlayerRequest tpr = new TagPlayerRequest(
                uuid,
                tagCode,
                tagDate

        );
        Call<APISuccess> call = client.reportTag(gameId, tpr);
        call.enqueue(new Callback<APISuccess>() {
            @Override
            public void onResponse(Call<APISuccess> call, Response<APISuccess> response) {
                if (response.isSuccessful()) {
                    //make the response string

                    if (response.body().success != null) {
                        AlertDialog.Builder b = new AlertDialog.Builder(getActivity().getApplicationContext());
                        b.setTitle("Tag Successful")
                                .setMessage(response.body().success)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Do nothing
                                    }
                                })
                                .show();
                    } else {
                        AlertDialog.Builder b = new AlertDialog.Builder(getActivity().getApplicationContext());
                        b.setTitle(getString(R.string.unexpected_response))
                                .setMessage(getString(R.string.unexpected_response_hint))
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Do nothing
                                    }
                                })
                                .show();
                    }
                } else {
                    //messages for failed tag
                }
            }

            @Override
            public void onFailure(Call<APISuccess> call, Throwable t) {
                AlertDialog.Builder b = new AlertDialog.Builder(getActivity().getApplicationContext());
                b.setTitle(getString(R.string.generic_connection_error))
                        .setMessage(getString(R.string.generic_connection_error_hint))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                            }
                        })
                        .show();
                Log.d("Error", t.getMessage());
            }
        });

    }

    private void showDatePickerDialog() {
        DatePickerFragment dp = DatePickerFragment.newInstance();
        dp.setTargetFragment(this, 0);
        dp.show(getFragmentManager(), "datePicker");
    }

    private void showTimePickerDialog() {
        TimePickerFragment tp = TimePickerFragment.newInstance();
        tp.setTargetFragment(this, 0);
        tp.show(getFragmentManager(), "timePicker");
    }

    private void showContentView(final boolean show) {
        myCodeContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showErrorView(final boolean show) {
        errorView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setError(String msg, String hint) {
        errorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Give feedback when the user presses the button
            }
        });

        TextView errorMsg = (TextView) errorView.findViewById(R.id.error_msg);
        errorMsg.setText(String.format("%s %s", msg, getString(R.string.tap_to_retry)));
        TextView errorHint = (TextView) errorView.findViewById(R.id.error_hint);
        errorHint.setText(hint);

        // Make the LinearLayout react when clicked
        // Source: http://stackoverflow.com/a/28087443
        TypedValue outValue = new TypedValue();
        getActivity().getApplicationContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        errorView.setBackgroundResource(outValue.resourceId);
    }

    private void showProgress(final boolean showProgress) {
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLogoutListener) {
            mListener = (OnLogoutListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must be an instance of OnLogoutListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        // Update the associated calendar
        tagTime.set(year, monthOfYear, dayOfMonth);
        // Update the associated textview
        DateFormat dateFormat = SimpleDateFormat.getDateInstance();
        String strDate = dateFormat.format(tagTime.getTime());
        DateInput.setText(strDate);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        // Update the associated calendar
        tagTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        tagTime.set(Calendar.MINUTE, minute);
        // Update the associated textview
        DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        String strTime = timeFormat.format(tagTime.getTime());
        TimeInput.setText(strTime);
    }

    public void setLatLng(){
        SharedPreferences prefs = getActivity().getSharedPreferences(TagLocationPref.NAME, 0);
        String latVal = prefs.getString("lat", null);
        if(latVal.length() >= 11){
            latVal = latVal.substring(0, 10);
        }
        String longVal = prefs.getString("long", null);
        if(longVal.length() >= 11) {
            longVal = longVal.substring(0, 10);
        }

        LocationInput.setText("(" + latVal + "," + longVal + ")");
    }
}
