package com.hvzhub.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hvzhub.app.API.API;
import com.hvzhub.app.API.ErrorUtils;
import com.hvzhub.app.API.HvZHubClient;
import com.hvzhub.app.API.model.APIError;
import com.hvzhub.app.API.model.APISuccess;
import com.hvzhub.app.API.model.Chapters.ChapterInfo;
import com.hvzhub.app.API.model.Games.HeatmapTagContainer;
import com.hvzhub.app.API.model.Games.PlayerCount;
import com.hvzhub.app.API.model.Uuid;
import com.hvzhub.app.Prefs.GamePrefs;

import org.w3c.dom.Text;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private OnLogoutListener mListener;
    private TextView humanCount;
    private TextView zombieCount;
    private TextView gameRules;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle(getActivity().getString(R.string.home));
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        humanCount = (TextView) view.findViewById(R.id.humanNum);
        zombieCount = (TextView) view.findViewById(R.id.zombieNum);
        gameRules = (TextView) view.findViewById(R.id.rules);

        String uuid = getActivity().getSharedPreferences(GamePrefs.NAME, Context.MODE_PRIVATE).getString(GamePrefs.PREFS_SESSION_ID, null);
        int gameId = getActivity().getSharedPreferences(GamePrefs.NAME, Context.MODE_PRIVATE).getInt(GamePrefs.PREFS_GAME_ID, -1);

        HvZHubClient client = API.getInstance(getActivity()).getHvZHubClient();
        Call<PlayerCount> call = client.numPlayers(new Uuid(uuid), gameId);
        call.enqueue(new Callback<PlayerCount>() {
            @Override
            public void onResponse(Call<PlayerCount> call, Response<PlayerCount> response) {
                if (response.isSuccessful()) {
                    humanCount.setText(Integer.toString(response.body().humans));
                    zombieCount.setText(Integer.toString(response.body().zombies));
                }
                else{
                    APIError apiError = ErrorUtils.parseError(response);
                    String err;
                    String errorMessage;
                    if (apiError.error == null ) {
                        err = "";
                    } else {
                        err = apiError.error.toLowerCase();
                    }
                    if (err.contains("invalid")) {
                        Toast.makeText(getActivity().getApplicationContext(), "Invalid Session ID. Logging Out...", Toast.LENGTH_SHORT);
                        logout();
                    }
                    else {
                        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());

                        b.setTitle(R.string.unexpected_response)
                            .setMessage(R.string.unexpected_response_hint)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing
                                }
                            })
                            .show();
                    }
                }
            }
            @Override
            public void onFailure(Call<PlayerCount> call, Throwable t) {
                AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
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

        String chapterURL = getActivity().getSharedPreferences(GamePrefs.NAME, Context.MODE_PRIVATE).getString(GamePrefs.PREFS_CHAPTER_URL, null);
        Call<ChapterInfo> call2 = client.getChapterInfo(new Uuid(uuid), chapterURL);
        call2.enqueue(new Callback<ChapterInfo>() {
            @Override
            public void onResponse(Call<ChapterInfo> call, Response<ChapterInfo> response) {
                if (response.isSuccessful()) {
                    gameRules.setText(response.body().rules);
                }
                else{
                    APIError apiError = ErrorUtils.parseError(response);
                    String err = apiError.error.toLowerCase();
                    String errorMessage;
                    if (err.contains("invalid")) {
                        Toast.makeText(getActivity().getApplicationContext(), "Invalid Session ID. Logging Out...", Toast.LENGTH_SHORT);
                        logout();
                    }
                    else {
                        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());

                        b.setTitle(R.string.unexpected_response)
                                .setMessage(R.string.unexpected_response_hint)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Do nothing
                                    }
                                })
                                .show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ChapterInfo> call, Throwable t) {
                AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
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

    public void logout(){
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(GamePrefs.NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        // Show the login screen again
        Intent i = new Intent(getActivity(), LoginActivity.class);
        startActivity(i);
        getActivity().finish();
    }
}
