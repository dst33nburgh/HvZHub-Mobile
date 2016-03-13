package com.hvzhub.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.hvzhub.app.API.API;
import com.hvzhub.app.API.HvZHubClient;
import com.hvzhub.app.API.NetworkUtils;
import com.hvzhub.app.API.model.Chapters.Chapter;
import com.hvzhub.app.API.model.Chapters.ChapterListContainer;
import com.hvzhub.app.API.model.Status;
import com.hvzhub.app.API.model.Uuid;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChapterSelectionActivity extends AppCompatActivity {

    ListView listView;
    ArrayAdapter<Chapter> adapter;
    List<Chapter> chapterList;

    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_selection);

        listView = (ListView) findViewById(R.id.list_view);
        chapterList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chapterList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: Remove this
                SharedPreferences.Editor prefs = getSharedPreferences(API.PREFS_API, Context.MODE_PRIVATE).edit();
                prefs.putString(API.PREFS_CHAPTER_URL, "u_of_test");
                prefs.putInt(API.PREFS_GAME_ID, 3);
                prefs.apply();

                // TODO: Remove this
                Intent i = new Intent(ChapterSelectionActivity.this, GameActivity.class);
                startActivity(i);
                finish();

                // TODO: Fix this method
                //joinChapter(chapterList.get(position));
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progress);

        // Show the close button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white);
        }

        getChapterList();
    }

    private void getChapterList() {
        if (!NetworkUtils.networkIsAvailable(this)) {
            AlertDialog.Builder b = new AlertDialog.Builder(ChapterSelectionActivity.this);
            b.setTitle(getString(R.string.network_not_available))
                    .setMessage(getString(R.string.network_not_available_hint))
                    .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                             getChapterList();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        } else {
            showProgress(true);
            HvZHubClient client = API.getInstance(getApplicationContext()).getHvZHubClient();

            String uuid = getSharedPreferences(API.PREFS_API, MODE_PRIVATE).getString(API.PREFS_SESSION_ID, null);
            Call<ChapterListContainer> call = client.chapters(new Uuid(uuid));
            call.enqueue(new Callback<ChapterListContainer>() {
                @Override
                public void onResponse(Call<ChapterListContainer> call, Response<ChapterListContainer> response) {
                    showProgress(false);
                    if (response.isSuccessful()) {
                        chapterList.clear();
                        chapterList.addAll(response.body().chapters);
                    } else {
                        AlertDialog.Builder b = new AlertDialog.Builder(ChapterSelectionActivity.this);
                        b.setTitle(getString(R.string.unexpected_response))
                                .setMessage(getString(R.string.unexpected_response_hint))
                                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        getChapterList();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .show();
                    }
                }

                @Override
                public void onFailure(Call<ChapterListContainer> call, Throwable t) {
                    showProgress(false);

                    AlertDialog.Builder b = new AlertDialog.Builder(ChapterSelectionActivity.this);
                    b.setTitle(getString(R.string.generic_connection_error))
                            .setMessage(getString(R.string.generic_connection_error_hint))
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getChapterList();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .show();
                }
            });

        }
    }

    private void joinChapter(Chapter chapter) {
        showProgress(true);
        HvZHubClient client = API.getInstance(getApplicationContext()).getHvZHubClient();
        String uuid = getSharedPreferences(API.PREFS_API, MODE_PRIVATE).getString(API.PREFS_SESSION_ID, null);
        Call<Status> call = client.join(chapter.getUrl(), new Uuid(uuid));
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                showProgress(false);
                // TODO: Do something
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                showProgress(false);
                // TODO: Do something
            }
        });
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            listView.setVisibility(show ? View.GONE : View.VISIBLE);
            listView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    listView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            listView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                String chapterUrl = getSharedPreferences(API.PREFS_API, MODE_PRIVATE).getString(API.PREFS_CHAPTER_URL, null);
                // If no chapter was selected, log them out and take them back to the login screen
                if (chapterUrl == null) {
                    // Logout and finish() this activity
                    API.getInstance(this).logout(this, true);
                } else {
                    // Else, just close this activity
                    finish();
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}