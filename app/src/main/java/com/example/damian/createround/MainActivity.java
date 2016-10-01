package com.example.damian.createround;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private int PICK_IMAGE_REQUEST = 1;

    private Button bSelectPicture, bAddCircle, bAccept;
    private CircleImageView circleImageView;
    private ImageView imageView;
    private Bitmap selectBitmap;
    private VelocityTracker vTracker = null;
    private float startingDistanceBetweenFingers;
    private Canvas canvas;
    private float Xaxis, Yaxis, radius = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        bSelectPicture = (Button) findViewById(R.id.b_select_picture);
        bAddCircle = (Button) findViewById(R.id.b_add_circle);
        bAccept = (Button) findViewById(R.id.b_accept);
        circleImageView = (CircleImageView) findViewById(R.id.profile_image);
        imageView = (ImageView) findViewById(R.id.i_image);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK)) {

            Uri imageUri = data.getData(); // zapisanie adresu Uri zdjęcia
            InputStream imputStream;

            try {
                imputStream = getBaseContext().getContentResolver().openInputStream(imageUri);
                selectBitmap = BitmapFactory.decodeStream(imputStream);  // konwersja adresu na selectBitmap
                selectBitmap = Bitmap.createScaledBitmap(selectBitmap, 1024, 768, false);
                imageView.setImageBitmap(this.selectBitmap);  // dodanie zdjęcia do imageView

//                bitmapmutable = selectBitmap.copy(Bitmap.Config.ARGB_8888, true);
//                circleImageView.setImageBitmap(bitmapmutable);

                imageView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        onTouchEventImageView(event);
                        return true;
                    }
                });

                Xaxis = selectBitmap.getWidth() / 2;
                Yaxis = selectBitmap.getHeight() / 2;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClick(R.id.b_select_picture)
    public void selectPicture() {
        Toast.makeText(getBaseContext(), "Open gallery", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*"); // wybierz zdjęcie ze wszysktkich rozszezeń
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @OnClick(R.id.b_add_circle)
    public void addCircle() {

        bitmapWithCircle();
    }

    @OnClick(R.id.b_accept)
    public void accept() {

        Bitmap bitmap = Bitmap.createBitmap((int) radius * 2, (int) radius * 2, Bitmap.Config.ARGB_8888);

        int XaxisStart = (int) (Xaxis - radius);
//        int XaxisEnd = (int) (Xaxis + radius);
        int YaxisStart = (int) (Yaxis - radius);
//        int YaxisEnd = (int) (Yaxis + radius);

        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                bitmap.setPixel(i, j, selectBitmap.getPixel(XaxisStart, YaxisStart));
                YaxisStart++;
            }
            YaxisStart = (int) (Yaxis - radius);
            XaxisStart++;
        }
        circleImageView.setImageBitmap(bitmap);
    }

    private void bitmapWithCircle() {

        Bitmap bitmap = selectBitmap.copy(Bitmap.Config.ARGB_8888, true);

        canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(Xaxis, Yaxis, radius, paint);
        imageView.setImageBitmap(bitmap);
        imageView.invalidate();
    }

    public boolean onTouchEventImageView(MotionEvent event) {

        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (event.getPointerCount() == 1) {

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (vTracker == null) {
                        vTracker = VelocityTracker.obtain();
                    } else {
                        vTracker.clear();
                    }
                    vTracker.addMovement(event);
                    break;
                case MotionEvent.ACTION_MOVE:

                    vTracker.addMovement(event);
                    vTracker.computeCurrentVelocity(1000);

                    Xaxis = Xaxis + vTracker.getXVelocity() / 50;
                    Yaxis = Yaxis + vTracker.getYVelocity() / 50;

                    exceptionAxisAndRadius();
//                    Log.i("Xaxis = ", String.valueOf(Xaxis));
//                    Log.i("Yaxis = ", String.valueOf(Yaxis));
//                    Log.i("Radius = ", String.valueOf(radius));

                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
            }
        } else if (event.getPointerCount() == 2) {
            switch (action) {
                case MotionEvent.ACTION_POINTER_DOWN: // przygotowanie do gestu ściskania/rozciągania
                    startingDistanceBetweenFingers = distanceBetweenTwoFingers(event); // zapamiętania początkowej odległości mięszy palcami
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_MOVE:
                    float newDistance = distanceBetweenTwoFingers(event);
                    if (newDistance != startingDistanceBetweenFingers) { // palce się oddalają

                        radius = radius + (newDistance - startingDistanceBetweenFingers) / 50;
                        exceptionAxisAndRadius();
                    }
                    break;
            }
        }

        bitmapWithCircle();
        return true;
    }

    private float distanceBetweenTwoFingers(MotionEvent e) {

        float x = e.getX(0) - e.getX(1);
        float y = e.getY(0) - e.getY(1);

        return (float) Math.sqrt(x * x + y * y);
    }

    private void exceptionAxisAndRadius() {
        if (Xaxis + radius > selectBitmap.getWidth()) {
            Xaxis = selectBitmap.getWidth() - radius;
        }
        if (Xaxis - radius < 0) {
            Xaxis = radius;
        }
        if (Yaxis + radius > selectBitmap.getHeight()) {
            Yaxis = selectBitmap.getHeight() - radius;
        }
        if (Yaxis - radius < 0) {
            Yaxis = radius;
        }
        if (radius > selectBitmap.getWidth() / 2) {
            radius = selectBitmap.getWidth() / 2;
        }
        if (radius > selectBitmap.getHeight() / 2) {
            radius = selectBitmap.getHeight() / 2;
        }
    }
}
