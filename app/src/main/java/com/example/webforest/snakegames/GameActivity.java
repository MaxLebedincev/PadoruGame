package com.example.webforest.snakegames;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

public class GameActivity extends Activity {

    Intent MainMenu;
    Canvas canvas;
    SnakeView snakeView;

    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;

    Bitmap[] masBody;

    private static final int SWIPE_MIN_DISTANCE = 130;
    private static final int SWIPE_MAX_DISTANCE = 300;
    private static final int SWIPE_MIN_VELOCITY = 200;
    private GestureDetectorCompat lSwipeDetector;

    int directionOfTravel=0;
    //0 = up, 1 = right, 2 = down, 3= left


    int screenWidth;
    int screenHeight;
    int topGap;

    long lastFrameTime;
    int fps;
    int score = 2;

    int [] snakeX;
    int [] snakeY;
    int snakeLength;
    int appleX;
    int appleY;

    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    boolean dead = false;
    DBHelper dbHelper;
    String user = "Неизвестный санта";

    long startTimer = System.currentTimeMillis();

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {


        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            //Log.e("Swipe", "Swipe");
            int oldDirect = directionOfTravel;

            if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_MIN_VELOCITY
                    && Math.abs(e1.getY() - e2.getY()) < SWIPE_MAX_DISTANCE) {
                directionOfTravel = 1;
            } else if (e2.getX() - e1.getX() < -1 * SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_MIN_VELOCITY
                    && Math.abs(e1.getY() - e2.getY()) < SWIPE_MAX_DISTANCE) {
                directionOfTravel = 3;
            } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_MIN_VELOCITY) {
                directionOfTravel = 2;
            } else if (e2.getY() - e1.getY() < -1 * SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_MIN_VELOCITY) {
                directionOfTravel = 0;
            }

            if(Math.abs(oldDirect - directionOfTravel) == 2) {
                directionOfTravel = oldDirect;
            }
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainMenu = new Intent(GameActivity.this,MainActivity.class);
        lSwipeDetector = new GestureDetectorCompat(this, new MyGestureListener());
        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);

//        Bundle argumentsGameActivity = getIntent().getExtras();

//        if (argumentsGameActivity != null)
//            user = argumentsGameActivity.get("Result").toString();

        dbHelper = new DBHelper(this);
    }

    class SnakeView extends SurfaceView implements Runnable {
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSnake;
        Paint paint;



        public SnakeView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            //Even my 9 year old play tester couldn't
            //get a snake this long
            snakeX = new int[200];
            snakeY = new int[200];

            //our starting snake
            getSnake();
            //get an apple to munch
            getApple();
        }

        public void getSnake(){
            snakeLength = 3;
            snakeX[0] = numBlocksWide/2;
            snakeY[0] = numBlocksHigh /2;

            snakeX[1] = snakeX[0]-1;
            snakeY[1] = snakeY[0];

            snakeX[1] = snakeX[1]-1;
            snakeY[1] = snakeY[0];
        }

        public void getApple(){
            Random random = new Random();
            appleX = random.nextInt(numBlocksWide-1)+1;
            appleY = random.nextInt(numBlocksHigh-1)+1;
        }

        @Override
        public void run() {
            while (playingSnake) {
                updateGame();
                if (!dead)
                {
                    drawGame();
                    controlFPS();
                }
                else
                {


                    SQLiteDatabase database = dbHelper.getWritableDatabase();

                    ContentValues contentValues = new ContentValues();

                    contentValues.put(DBHelper.KEY_NAME, user);
                    contentValues.put(DBHelper.KEY_SCORE, score);
                    contentValues.put(DBHelper.KEY_DATE, (System.currentTimeMillis() - startTimer)/1000 );


                    database.insert(DBHelper.TABLE_CONTACTS, null, contentValues);
                }
            }

        }

        public void updateGame() {

            //Did the player get the apple
            if(snakeX[0] == appleX && snakeY[0] == appleY){
                snakeLength++;
                getApple();
                score++;
            }

            for(int i=snakeLength; i >0 ; i--){
                snakeX[i] = snakeX[i-1];
                snakeY[i] = snakeY[i-1];
            }


            switch (directionOfTravel){
                case 0://up
                    snakeY[0]  --;
                    break;

                case 1://right
                    snakeX[0] ++;
                    break;

                case 2://down
                    snakeY[0] ++;
                    break;

                case 3://left
                    snakeX[0] --;
                    break;
            }

            if(snakeX[0] == -1)dead=true;
            if(snakeX[0] >= numBlocksWide)dead=true;
            if(snakeY[0] == -1)dead=true;
            if(snakeY[0] == numBlocksHigh)dead=true;

            for (int i = snakeLength-1; i > 0; i--) {
                if ((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i])) {
                    dead = true;
                }
            }


            if(dead){

                MainMenu.putExtra("Result", score);
                startActivity(MainMenu);
                //score = 0;
                //getSnake();

            }

        }

        public void drawGame() {

            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();

                canvas.drawColor(Color.rgb(163, 217, 208));
                paint.setColor(Color.rgb(15, 89, 6));
                paint.setTextSize(topGap/2);
                canvas.drawText("Жители: " + score, 10, topGap-6, paint);

                paint.setStrokeWidth(3);//4 pixel border
                canvas.drawLine(1,topGap,screenWidth-1,topGap,paint);
                canvas.drawLine(screenWidth-1,topGap,screenWidth-1,topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(screenWidth-1,topGap+(numBlocksHigh*blockSize),1,topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(1,topGap, 1,topGap+(numBlocksHigh*blockSize), paint);


                canvas.drawBitmap(headBitmap, snakeX[0]*blockSize, (snakeY[0]*blockSize)+topGap, paint);

                for(int i = 1; i < snakeLength-1;i++){
                    Random random = new Random();
                    bodyBitmap = Bitmap.createScaledBitmap(masBody[i-1], blockSize, blockSize, false);
                    canvas.drawBitmap(bodyBitmap, snakeX[i]*blockSize, (snakeY[i]*blockSize)+topGap, paint);
                }
                //draw the tail
                canvas.drawBitmap(tailBitmap, snakeX[snakeLength-1]*blockSize, (snakeY[snakeLength-1]*blockSize)+topGap, paint);

                //draw the apple
                canvas.drawBitmap(appleBitmap, appleX*blockSize, (appleY*blockSize)+topGap, paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }

        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 100 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {

                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    //Print an error message to the console
                    Log.e("error", "failed to load sound files");
                }

            }

            lastFrameTime = System.currentTimeMillis();
        }


        public void pause() {
            playingSnake = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
            }

        }

        public void resume() {
            playingSnake = true;
            ourThread = new Thread(this);
            ourThread.start();
        }


        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            lSwipeDetector.onTouchEvent(motionEvent);
            return true;
        }


    }

    @Override
    protected void onStop() {
        super.onStop();

        while (true) {
            snakeView.pause();
            break;
        }

        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            snakeView.pause();



            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }

    public void configureDisplay(){

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        topGap = screenHeight/14;

        blockSize = screenWidth/20; //40

        numBlocksWide = 20; //40
        numBlocksHigh = ((screenHeight - topGap ))/blockSize;

        headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.padoru);

        //---------------------------------------

        Bitmap[] masTempBody = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.padoru1),
                BitmapFactory.decodeResource(getResources(), R.drawable.padoru2),
                BitmapFactory.decodeResource(getResources(), R.drawable.padoru3),
                BitmapFactory.decodeResource(getResources(), R.drawable.padoru4)
        };

        masBody = new Bitmap[200];
        for(int j = 0; j < 200; j++)
        {
            Random random = new Random();
            masBody[j] = Bitmap.createScaledBitmap(masTempBody[random.nextInt(4-1)], blockSize, blockSize, false);
        }
        //---------------------------------------


        tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sled);
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.house);

        headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize, blockSize, false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);

    }


}
