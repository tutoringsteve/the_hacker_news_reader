package com.myappcompany.steve.newsreader;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {

    static ArrayList<String> favTitles;
    static ArrayList<Integer> favIds;
    static ArrayAdapter favAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        final ListView favStoriesListView = findViewById(R.id.favStoriesListView);

        favTitles = new ArrayList<>();
        favIds = new ArrayList<>();
        try {
            Cursor c = MainActivity.favDatabase.rawQuery("SELECT * FROM favStories", null);

            int idIndex = c.getColumnIndex("id");
            int titleIndex = c.getColumnIndex("title");

            c.moveToFirst();
            while (!c.isAfterLast()) {
                favIds.add(c.getInt(idIndex));
                favTitles.add(c.getString(titleIndex));
                c.moveToNext();
            }

            c.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Error switching to fav:", "Error loading the favorites DB" + e.toString() + e.getMessage());
        }

        favAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, favTitles);

        favStoriesListView.setAdapter(favAdapter);

        favStoriesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                intent.putExtra("id", favIds.get(position));
                intent.putExtra("source", "favorites");
                startActivity(intent);
            }
        });

        favStoriesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(FavoritesActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Are you sure?")
                        .setMessage("Are you sure you want to delete this from your favorites?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                int id = favIds.get(position);
                                favTitles.remove(position);
                                favIds.remove(position);

                                MainActivity.favDatabase.execSQL("DELETE FROM favStories WHERE id = " + id);

                                favAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            }
        });
    }

}
