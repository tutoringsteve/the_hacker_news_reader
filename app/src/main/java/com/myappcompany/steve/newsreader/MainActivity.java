package com.myappcompany.steve.newsreader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ArrayList<Integer> topStoryIDs = new ArrayList<>();
    ArrayList<String> topStoryTitles = new ArrayList<>();
    static SQLiteDatabase myDatabase;
    static SQLiteDatabase favDatabase;
    ArrayAdapter arrayAdapter;

    int  maxStories = 10;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.favorites:
                Intent intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDatabase = this.openOrCreateDatabase("topStories", MODE_PRIVATE, null);
        favDatabase = this.openOrCreateDatabase("favStories", MODE_PRIVATE, null);
        //Clear current database
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS topStories (id INTEGER, title VARCHAR, url VARCHAR)");
        favDatabase.execSQL("CREATE TABLE IF NOT EXISTS favStories (id INTEGER, title VARCHAR, url VARCHAR, UNIQUE (id, title, url))");

        //Grab and process the json for the top stories
        //@ https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty

        //For the first int maxStories of these run the url
        //"https://hacker-news.firebaseio.com/v0/item/"+ String.valueOf(itemID) + ".json"

        //Process this json
        //store the information in a database with the following columns
        // id | title | url, see below for more : https://github.com/HackerNews/API
        //Field	        Description
        //-------------|--------------------------------------------------------------------------
        //id	        The item's unique id.
        //deleted	    true if the item is deleted.
        //type	        The type of item. One of "job", "story", "comment", "poll", or "pollopt".
        //by	        The username of the item's author.
        //time	        Creation date of the item, in Unix Time.
        //text	        The comment, story or poll text. HTML.
        //dead	        true if the item is dead.
        //parent	    The comment's parent: either another comment or the relevant story.
        //poll	        The pollopt's associated poll.
        //kids	        The ids of the item's comments, in ranked display order.
        //url	        The URL of the story.
        //score	        The story's score, or the votes for a pollopt.
        //title	        The title of the story, poll or job. HTML.
        //parts	        A list of related pollopts, in display order.
        //descendants	In the case of stories or polls, the total comment count.

        //Populate a ListView with the "title" fields.

        //When you click on an item in the ListView, have it open a WebView with the url

        //populate topStoryIDs
        new GetTopStoryIDsTask().execute("https://hacker-news.firebaseio.com/v0/topstories.json");

        Log.i("story ids", topStoryIDs.toString());

        ListView topStoriesListView = findViewById(R.id.topStoriesListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, topStoryTitles);
        topStoriesListView.setAdapter(arrayAdapter);

        topStoriesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                intent.putExtra("id", topStoryIDs.get(position));
                intent.putExtra("source", "top");
                startActivity(intent);
            }
        });

        topStoriesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Are you sure?")
                        .setMessage("Are you sure you want to add this to your favorites?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                int id = topStoryIDs.get(position);
                                //take the data from the topStories DB and add them to the favStories DB
                                Cursor c = MainActivity.myDatabase.rawQuery("SELECT * FROM topStories WHERE id = " + id, null);

                                int titleIndex = c.getColumnIndex("title");
                                int urlIndex = c.getColumnIndex("url");

                                String url = "";
                                String title = "";

                                c.moveToFirst();
                                if(!c.isAfterLast()) {
                                    title = c.getString(titleIndex);
                                    url = c.getString(urlIndex);
                                }
                                Log.i("Added to fav: ", "id: " + id + " title: " + title + " url: " +url);

                                ContentValues cv = new ContentValues();
                                cv.put("id", id);
                                cv.put("title", title);
                                cv.put("url", url);
                                try {
                                    favDatabase.insertOrThrow("favStories", null, cv);
                                } catch (SQLiteConstraintException e) {
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(), "Article already added to favorites.", Toast.LENGTH_SHORT).show();
                                }
                                /*
                                favDatabase.execSQL("INSERT INTO OR IGNORE favStories (id, title, url) VALUES ("
                                        + id + ", '"
                                        + title + "', '"
                                        + url + "')" );
                                */
                                c.close();

                                //If the favorites Activity has already been opened once
                                //We can no longer rely on the onCreate method
                                //And must then update the live ArrayList and Adapter
                                if(FavoritesActivity.favAdapter != null && FavoritesActivity.favTitles != null) {
                                    if(!FavoritesActivity.favTitles.contains(title)){
                                        FavoritesActivity.favTitles.add(title);
                                        FavoritesActivity.favIds.add(id);
                                        FavoritesActivity.favAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            }
        });

        myDatabase.delete("topStories", null, null);
    }

    public class GetTopStoryIDsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpsURLConnection connection;

            try{
                url = new URL(urls[0]);
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();
                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while(data != -1) {
                    result += (char) data;
                    data = reader.read();
                }

                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        @Override
        protected void onPostExecute(String json) {
            super.onPostExecute(json);

            try {
                //The String should be a keyless JSON array [ 22478508, 22476930, 22472779, 22477875, ..., 22455935 ]
                JSONArray jsonArray = new JSONArray(json);

                for (int i = 0; i < Math.min(maxStories, jsonArray.length()); i++) {
                    topStoryIDs.add(jsonArray.getInt(i));
                    new GetStoryJSONTask().execute(jsonArray.getInt(i));
                    Log.i("top story id added: ", String.valueOf(jsonArray.getInt(i)));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Json error: ", "Error in GetTopStoryIDsTask " + e.toString() + e.getMessage());
            }
        }
    }

    public class GetStoryJSONTask extends AsyncTask<Integer, Void, String> {

        @Override
        protected String doInBackground(Integer... itemIDs) {

            String result = "";
            URL url;
            HttpsURLConnection connection;

            try{
                int itemId = itemIDs[0];
                url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ itemId + ".json");
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();
                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while(data != -1) {
                    result += (char) data;
                    data = reader.read();
                }

                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        @Override
        protected void onPostExecute(String json) {
            super.onPostExecute(json);

            try {
                /*{
                  "by" : "dhouston",
                  "descendants" : 71,
                  "id" : 8863,
                  "kids" : [ 8952, 9224, 8917, 8884, 8887, 8943, 8869, 8958, 9005, 9671, 8940, 9067, 8908, 9055, 8865, 8881, 8872, 8873, 8955, 10403, 8903, 8928, 9125, 8998, 8901, 8902, 8907, 8894, 8878, 8870, 8980, 8934, 8876 ],
                  "score" : 111,
                  "time" : 1175714200,
                  "title" : "My YC app: Dropbox - Throw away your USB drive",
                  "type" : "story",
                  "url" : "http://www.getdropbox.com/u/2/screencast.html"
                }*/

                JSONObject jsonObject = new JSONObject(json);

                String title = jsonObject.getString("title");
                String url = jsonObject.getString("url");
                int id = jsonObject.getInt("id");

                Log.i("story data: " , "id: " + id + " title : " + title + " url: " + url);

                try {
                    myDatabase.execSQL("INSERT INTO topStories (id, title, url) VALUES ("
                            + id + ", '"
                            + title.replaceAll("'","''") + "', '"
                            + url.replaceAll("'","''") + "')" );
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Database add error: ", "Error adding id: " + id + " title: " + title + " url: " + url + " error details: " + e.toString() + e.getMessage());
                }
                topStoryTitles.add(title);
                arrayAdapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Json error: ", "Error in GetTopStoryIDsTask " + e.toString() + e.getMessage());
            }
        }
    }
}
