package com.example.webforest.snakegames;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    Intent playGame;
    Intent rating;
    DBHelper dbHelper;
    SQLiteDatabase database;
    String user = "Неизвестный санта";
    ListView list;
    List<String> arrList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playGame = new Intent(this, GameActivity.class);
        setContentView(R.layout.activity_main_menu);
        String score = "";

        Bundle argumentsGameActivity = getIntent().getExtras();

        if (argumentsGameActivity != null)
            score = argumentsGameActivity.get("Result").toString();

        dbHelper = new DBHelper(this);
        database = dbHelper.getReadableDatabase();
        Button buttonPlayGame = findViewById(R.id.startGame);
        list = findViewById(R.id.list);

//        EditText userName = findViewById(R.id.user);

//        user = "Неизвестный санта";
//
//        if (userName != null)
//            user = userName.toString();

        buttonPlayGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                playGame.putExtra("User", user);
                startActivity(playGame);

            }
        });

        TextView resultText = findViewById(R.id.result);

        if (score != "")
            resultText.setText("Жители отметившие новый год: " + score);

        setAdapter();

//        Cursor cr = database.query(DBHelper.TABLE_CONTACTS, null, null, null, null, null, null);
//
//        if(cr.moveToFirst())
//        {
//            int  idIndex = cr.getColumnIndex(DBHelper.KEY_ID);
//            int nameIndex = cr.getColumnIndex(DBHelper.KEY_NAME);
//            int scoreIndex = cr.getColumnIndex(DBHelper.KEY_SCORE);
//        }

    }

    private List<String> setArrList() {
        List<String> tempList = new ArrayList<String>();

        Cursor dbRead = database.query(DBHelper.TABLE_CONTACTS,
                null, null,null,null, null, "score desc");
        if(dbRead.moveToFirst()) {

            int idNum = dbRead.getColumnIndex("_id");
            int nameNum = dbRead.getColumnIndex("name");
            int scoreNum = dbRead.getColumnIndex("score");
            int dateNum = dbRead.getColumnIndex("date");

            do{
                tempList.add(dbRead.getString(nameNum) + dbRead.getInt(idNum) + " | "
                        + dbRead.getInt(scoreNum) +" | " + dbRead.getInt(dateNum) + " сек");
            }while(dbRead.moveToNext());

        } else {
            Log.e("Empty", "Empty");
        }

        dbRead.close();

        return tempList;
    }

    private void setAdapter() {
        arrList = setArrList();
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, arrList);
        list.setAdapter(arrayAdapter);
    }
}
