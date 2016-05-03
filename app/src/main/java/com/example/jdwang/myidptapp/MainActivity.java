package com.example.jdwang.myidptapp;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.lang.Math.sqrt;


public class MainActivity extends AppCompatActivity {

    private static final String DEBUG_TAG = "iDPT Demo -> ";

    ViewSwitcher    viewSwitcher;
    ImageView       imageView;
    TextView        textView;
    Point           zeroPoint;
    Bitmap[]        bitmapArray;
    Point[]         positionArray;
    int[]           colorArray;
    int[]           AcceleArray;
    String[]        photoNameArray;
    boolean[]       selectedTagName;
    JSONObject      jsonResponse;
    JSONArray       jsonMainNode;
    String          qpeUrl;
    String          stringp;
    String          J;
    boolean         doConnect       = false;
    boolean         doPhoto         = false;
    boolean         doDot           = false;
    boolean         doCross         = false;
    boolean         onSite          = true;
    boolean         doColorArray    = false;
    boolean         doPositionArray = false;
    boolean         doPhotoArray    = false;
    boolean         timerOn         = false;
    boolean         doAssetDemo     = false;
    boolean         doHRDemo        = false;
    boolean         doCamDemo       = false;
    boolean         doAccelDemo     = false;
    int             dotSize         = 8;
    int             assetTagID;
    private         Handler         mHandler;
    private         Handler         vHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
        imageView    = (ImageView) findViewById(R.id.imageView);
        textView     = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        if (!onSite) {
            qpeUrl = "http://numbersapi.com/random/date?json";
        } else {
            //qpeUrl = "http://192.168.123.124:8080/qpe/getHAIPLocation?version=2&maxAge=50000&humanReadable=true";
            qpeUrl = "http://192.168.123.124:8080/qpe/getTagPosition?version=2&humanReadable=true&maxAge=5000";
        }

        stringp = "smoothedPosition";

        zeroPoint = getZeroPoint();

        mHandler = new Handler();
        mHandler.post(runnable);
        vHandler = new Handler();
        vHandler.post(uiRunnable);

    }


    final Runnable runnable = new Runnable() {
        public void run() {
            // TODO Auto-generated method stub
            // 需要背景作的事
            new DownloadJSONTask().execute(qpeUrl);

          }
    };

    final Runnable uiRunnable = new Runnable() {
        public void run() {
            uiUpdate();
        }
    };

    private void uiUpdate() {
        vHandler.postDelayed(uiRunnable, 500);
        if (timerOn) {
            if (imageView.isShown() && (onSite)) repaint();
            if (textView.isShown()) textView.setText(J);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_onsite:
                if (!onSite) {
                    if (imageView.isShown()) viewSwitcher.showNext();
                    qpeUrl = "http://192.168.123.124:8080/qpe/getTagPosition?version=2&humanReadable=true&maxAge=5000";
                    onSite = !onSite;
                    item.setTitle("Off Site");
                    textView.setText(qpeUrl);
                } else {
                    if (imageView.isShown()) viewSwitcher.showNext();
                    qpeUrl = "http://numbersapi.com/random/date?json";
                    onSite = !onSite;
                    item.setTitle("On Site");
                    textView.setText(qpeUrl);
                }
                //textView.setText("This is a demo of Quuppa HAIP system installed by FullyRich Worldwide.");
                return true;
            case R.id.action_connect:
                if (imageView.isShown()) viewSwitcher.showNext();
                //check network connection
                doConnect = isNetworkAvailable();
                if (doConnect) {
                    selectConnType();
                    timerOn = !timerOn;
                    textView.setText("Connection Status: It's good to go!");
                    runnable.run();
                    uiRunnable.run();
                } else {
                    textView.setText("Connection Status: Oops!");
                }
                return true;
            case R.id.action_display:
                if (textView.isShown()) viewSwitcher.showNext();
                callDisplayOption();
                return true;
            case R.id.action_demo:
                if (textView.isShown()) viewSwitcher.showNext();
                callDemoOption();
                return true;
            case R.id.action_tagname:
                if (textView.isShown()) viewSwitcher.showNext();
                callTagNameDialog();
                return true;
            case R.id.action_exit:
                timerOn = false;
                finish();
                System.exit(0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (mHandler != null) {
            mHandler.removeCallbacks(runnable);
        }
        if (vHandler != null) {
            vHandler.removeCallbacks(uiRunnable);
        }
    }

    public void repaint() {
        Bitmap bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (doCross) {
            paint.setColor(Color.RED);
            canvas.drawLine(zeroPoint.x, 0, zeroPoint.x, imageView.getHeight(), paint);
            canvas.drawLine(0, zeroPoint.y, imageView.getWidth(), zeroPoint.y, paint);
        }

        if (doPhoto && !doPositionArray) {
            for (int i = 0; i < bitmapArray.length; i++) {
                if (!onSite) {
                    if (doPhotoArray) canvas.drawBitmap(bitmapArray[i], 100 + (i * 100), 150 + (i * 120), paint);
                } else {
                    if (selectedTagName[i]) canvas.drawBitmap(bitmapArray[i], positionArray[i].x - 18, positionArray[i].y - 18, paint);
                }
            }

        }

        if (doDot && !doPositionArray) {
            switch ((doAssetDemo) ? 1 : 0) {
                case 0:
                    for (int i = 0; i < colorArray.length; i++) {
                        paint.setColor(colorArray[i]);
                        if (selectedTagName[i]) canvas.drawCircle(positionArray[i].x, positionArray[i].y, 10, paint);
                    }
                    break;
                case 1:
                    for (int i = 0; i < colorArray.length; i++) {
                        paint.setColor(colorArray[i]);
                        if ((i == assetTagID) && nobodyCloseBy()) {
                            dotSize = dotSize + 2;
                            if (dotSize == 32) dotSize = 10;
                            paint.setColor(Color.RED);
                            canvas.drawCircle(positionArray[i].x, positionArray[i].y, dotSize, paint);
                            switch (dotSize) {
                                case 10 :
                                    doBeep();
                                    break;
                                case 20 :
                                    doBeep();
                                    break;
                                default : break;
                            }
                        } else
                        if (selectedTagName[i]) canvas.drawCircle(positionArray[i].x, positionArray[i].y, 10, paint);
                    }
                    break;
            }
        }
        if (doAccelDemo) {
        //calculate the acceleration
        // draw dot based on the acceleration level
        }
        imageView.setImageBitmap(bitmap);
    }

    private Point getZeroPoint() {
        int x, y;

        x = (int) (390 * 720 / 1002) - 40;
        y = (int) (1156 * 720 /1002) + 20;

        return new Point(x, y);

    }

    private void loadColorArray(String jstring) {

        try {
            jsonResponse = new JSONObject(jstring);
            jsonMainNode = jsonResponse.optJSONArray("tags");

            colorArray = new int[jsonMainNode.length()];

            for (int i = 0; i < jsonMainNode.length(); i++) {
                JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                colorArray[i] = Color.parseColor(jsonChildNode.getString("color"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadAcceleArray(String jstring) {
        try {
            jsonResponse = new JSONObject(jstring);
            jsonMainNode = jsonResponse.optJSONArray("tags");

            colorArray = new int[jsonMainNode.length()];

            for (int i = 0; i < jsonMainNode.length(); i++) {
                JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                colorArray[i] = Color.parseColor(jsonChildNode.getString("color"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadPositionArray(String jstring) {

        double tempx, tempy;

        try {
            jsonResponse = new JSONObject(jstring);
            jsonMainNode = jsonResponse.optJSONArray("tags");

            positionArray = new Point[jsonMainNode.length()];

            for (int i = 0; i < jsonMainNode.length(); i++) {
                JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);

                JSONArray jsonPosition = jsonChildNode.getJSONArray(stringp);
                tempx = jsonPosition.getDouble(0) * 100 * 720 / 1002 / 1.28912;
                tempy = jsonPosition.getDouble(1) * 100 * 720 / 1002 / 1.28912 * -1;

                tempx = tempx + zeroPoint.x;
                tempy = tempy + zeroPoint.y;

                positionArray[i] = new Point((int) tempx, (int) tempy);
            }

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadPhotoArray(String jstring) {

        try {
            jsonResponse = new JSONObject(jstring);
            jsonMainNode = jsonResponse.optJSONArray("tags");

            photoNameArray = new String[jsonMainNode.length()];
            selectedTagName = new boolean[jsonMainNode.length()];

            for (int i = 0; i < jsonMainNode.length(); i++) {
                JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                photoNameArray[i] = jsonChildNode.getString("name");
                selectedTagName[i] = true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        bitmapArray = new Bitmap[jsonMainNode.length()];

        try {
            Class       res = R.drawable.class;
            Field       field;
            int         imgId;
            for (int i = 0; i < jsonMainNode.length(); i++) {
                field = res.getField(photoNameArray[i].toLowerCase());
                imgId = field.getInt(null);
                bitmapArray[i] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), imgId), 36, 36, true);
            }
        }
        catch (Exception e) {
            Log.e("iDPT Demo", "Failure to get drawable id.", e);
        }

    }

    private class DownloadJSONTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve JSON data. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d(DEBUG_TAG, "Return from DownloadJSONTask: " + result);
            //repaint();
         }
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream         is = null;
        int                 response;
        String              contentAsString = null;
        HttpURLConnection   conn;

        try {
            URL url = new URL(myurl);
            while (timerOn) {
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(300 /* milliseconds */);
                conn.setConnectTimeout(350 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                response = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response is: " + response);
                while(response!=200) {
                    conn.disconnect();
                    conn = null;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    conn =(HttpURLConnection)url.openConnection();
                    response = conn.getResponseCode();
                }

                is = conn.getInputStream();
                J = contentAsString = readIt(is, is.available());

                if ((!doAccelDemo) && (isJSONValid(J) && onSite)) {
                    if (!doPhotoArray) {
                        doPhotoArray = !doPhotoArray;
                        loadPhotoArray(J);
                    }
                    if (!doColorArray) {
                        doColorArray = !doColorArray;
                        loadColorArray(J);
                    }

                    if (!doPositionArray) {
                        doPositionArray = !doPositionArray;
                        loadPositionArray(J);
                        doPositionArray = !doPositionArray;
                    }
                }

                conn.disconnect();
                conn = null;

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            return contentAsString;
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    private boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected

        if (networkInfo != null && networkInfo.isConnected()) {
           return true;
        }
        return false;
    }

    private void callTagNameDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Tag to be Display");
        builder.setMultiChoiceItems(photoNameArray, selectedTagName, new DialogInterface.OnMultiChoiceClickListener() {
            // indexSelected contains the index of item (of which checkbox checked)
            @Override
            public void onClick(DialogInterface dialog, int indexSelected,
                                boolean isChecked) {
                if (isChecked) {
                    selectedTagName[indexSelected] = true;
                } else {
                    selectedTagName[indexSelected] = false;
                }
            }
        })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on OK
                        //  You can write the code  to save the selected item here

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel

                    }
                });

        AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
        dialog.show();

    }

    private void callDisplayOption () {

 // Strings to Show In Dialog with Radio Buttons
        final CharSequence[] items = {" Photo "," Dot "};

        int selectedItem = -1;
        if (doPhoto) selectedItem = 0;
        if (doDot) selectedItem = 1;
        final int finalSelectedItem = selectedItem;

        // Creating and Building the Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select The Display Model");

        builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        doPhoto = true;
                        doDot = !doPhoto;// Your code when first option seletced
                        doAssetDemo = !doPhoto;
                        break;
                    case 1:
                        doDot = true;
                        doPhoto = !doDot;// Your code when 2nd  option seletced
                        break;
                }
            }
        })

        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //  Your code when user clicked on OK
                //  You can write the code  to save the selected item here

            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //  Your code when user clicked on Cancel
                doPhoto = doDot = false;
                switch (finalSelectedItem) {
                    case 0:
                        doPhoto = true;
                        break;
                    case 1:
                        doDot = true;
                        break;
                    case -1:
                        break;
                }
            }
        });

        AlertDialog setDisplayModeDialog = builder.create();
        setDisplayModeDialog.show();
    }

    private void callDemoOption() {

        // Strings to Show In Dialog with Radio Buttons
        final CharSequence[] items = {" Asset Alert "," Heart Rate Monitor " , " Camera Activate ", "Acceleration Monitor"};

        int selectedItem              = -1;
        if (doAssetDemo) selectedItem = 0;
        if (doHRDemo)    selectedItem = 1;
        if (doCamDemo)   selectedItem = 2;
        if (doAccelDemo) selectedItem = 3;
        final int finalSelectedItem   = selectedItem;

        // Creating and Building the Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select A Demo Mode");

        builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        doAssetDemo = true;
                        doAccelDemo = doCamDemo = doHRDemo = !doAssetDemo;// Your code when first option seletced
                        break;
                    case 1:
                        doHRDemo = true;
                        doAccelDemo = doCamDemo = doAssetDemo = !doHRDemo;// Your code when 2nd  option seletced
                        break;
                    case 2:
                        doCamDemo = true;
                        doAccelDemo = doAssetDemo = doHRDemo = !doCamDemo;// Your code when 2nd  option seletced
                        break;
                    case 3:
                        doAccelDemo = true;
                        doAssetDemo = doHRDemo = doCamDemo = !doAccelDemo;
                        break;
                }
            }
        })

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  make sure at least 2 tags are selected with badge must on
                        if (doAssetDemo) {
                            selectAssetTagDialog();
                            if (assetTagID != -1) selectedTagName[assetTagID] = true;
                            doDot = true;
                            doPhoto = !doDot;
                        }
                        if (doAccelDemo) {
                            timerOn = !timerOn;
                            doDot = true;
                            doPhoto = !doDot;
                            viewSwitcher.showNext();
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            qpeUrl = ConfigDownloadURL();
                            if (mHandler != null) {
                                mHandler.removeCallbacks(runnable);
                            }
                            if (vHandler != null) {
                                vHandler.removeCallbacks(uiRunnable);
                            }
                            timerOn = !timerOn;
                            doConnect = isNetworkAvailable();
                            if (doConnect && timerOn) {
                                runnable.run();
                                uiRunnable.run();
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel
                        doAssetDemo = doHRDemo = doCamDemo = false;
                        switch (finalSelectedItem) {
                            case 0:
                                doAssetDemo = true;
                                break;
                            case 1:
                                doHRDemo = true;
                                break;
                            case 2:
                                doCamDemo = true;
                                break;
                            case 3:
                                doAccelDemo = true;
                                break;
                            case -1:
                                break;
                        }
                    }
                });

        AlertDialog setDemoModeDialog = builder.create();
        setDemoModeDialog.show();
    }

    private boolean nobodyCloseBy() {
        boolean status;
        int j = 0;
        int k = 0;
        double tempx, tempy;

        int pixpermeter = (int) (1/0.0128912);

        for (int i = 0; i < selectedTagName.length; i++) {
            if ((selectedTagName[i]) && (i != assetTagID)) {
                k = k + 1;
                tempx = positionArray[i].x - positionArray[assetTagID].x;
                tempy = positionArray[i].y - positionArray[assetTagID].y;
                if (sqrt((tempx*tempx)+(tempy*tempy)) > pixpermeter) j = j + 1;
            }
        }
        status = !(k > j);
        return status;
    }

    private void selectAssetTagDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select tag to be act as Asset");
        builder.setSingleChoiceItems(photoNameArray, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                assetTagID = which;
            }
        })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on OK
                        //  You can write the code  to save the selected item here

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel
                        assetTagID = -1;

                    }
                });

        AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
        dialog.show();

    }

    private void selectConnType () {

        // Strings to Show In Dialog with Radio Buttons
        final CharSequence[] items = {" smoothedPosition "," position "};

        int selectedItem = 0;
        if (stringp == "smoothedPosition") selectedItem = 0;
        else selectedItem = 1;

        final int finalSelectedItem = selectedItem;

        // Creating and Building the Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select The Position Data Type");

        builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        stringp = "smoothedPosition";
                        break;
                    case 1:
                        stringp = "position";
                        break;
                }
            }
        })

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on OK
                        //  You can write the code  to save the selected item here

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel
                        switch (finalSelectedItem) {
                            case 0:
                                stringp = "smoothedPosition";
                                break;
                            case 1:
                                stringp = "position";
                                break;
                            case -1:
                                stringp = "smoothedPosition";
                                break;
                        }
                    }
                });

        AlertDialog selectConnTypeDialog = builder.create();
        selectConnTypeDialog.show();
    }

    private void doBeep() {
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    private String ConfigDownloadURL() {
        String  url = null;
        if (!onSite) {
            url = "http://numbersapi.com/random/date?json";
        } else {
            url = "http://192.168.123.124:8080/qpe/getTagPosition?version=2&humanReadable=true&maxAge=5000";
        }
        if (doAssetDemo) {
            url = "http://192.168.123.124:8080/qpe/getTagPosition?version=2&humanReadable=true&maxAge=5000";
        }
        if (doHRDemo) {
            url = "http://192.168.123.124:8080/qpe/getTagInfo?version=2&maxAge=50000&humanReadable=true";
        }
        if (doCamDemo) {
            url = "";
        }
        if (doAccelDemo) {
            url = "http://192.168.123.124:8080/qpe/getTagInfo?version=2&maxAge=50000&humanReadable=true";
        }
        return url;
    }

}
