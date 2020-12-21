package de.tudarmstadt.tk.smartguitarcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Arrays;

import static de.tudarmstadt.tk.smartguitarcontrol.activities.RemoteConfigActivity.pyToAndroidColor;

public class FretBoard extends View{
    private final int m_count_frets = 4;
    private final int m_count_strings = 6;
    private int fretHeight;
    private int fretPartWidth;
    private boolean isInit = false;
    private boolean isMatrixInit = false;
    private int[][] data = new int[m_count_frets][m_count_strings];
    private final static String TAG = "FretBoard";
    private Matrix transformation_matrix;
    public int colorBase;

    public FretBoard(Context context) {
        super(context);
    }

    public FretBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FretBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initMatrix(){
        for(int i=0;i<data.length;i++){
            for(int j=0;j<data[0].length;j++){
                data[i][j] = -2;
            }
        }
        isMatrixInit = true;
    }

    private void initFretBoard(){
        int height = getHeight();
        int width = getWidth();
        colorBase = Color.WHITE;

        isInit = true;
        if(!isMatrixInit){
            initMatrix();
        }

        fretHeight = height / m_count_frets;
        fretPartWidth = width / m_count_strings;

        transformation_matrix = new Matrix();
        float[] src = {0f,0f,
                (float) width,0f,
                0f, (float) height,
                (float) width, (float) height};
        float[] dst = {(float) 0.125* width,0f,
                (float) 0.875*width,0f ,
                0f, (float) height,
                (float) width, (float) height};

        transformation_matrix.setPolyToPoly(src,0,dst,0,4);
    }


    @SuppressLint("NewApi")
    @Override
    protected void onDraw(Canvas canvas) {
        if(!isInit){
            initFretBoard();
        }
        canvas.drawColor(Color.LTGRAY);
        applyData(canvas);

            //if(Build.VERSION.SDK_INT>=29) {
        try {
            this.setAnimationMatrix(transformation_matrix);
        }catch (Exception e){
            Log.e(TAG, "onDraw: Transforming exception",e);
        }
            //}
        super.onDraw(canvas);
    }

    protected void applyData(Canvas canvas){
        int m_margin_h = 8;
        int m_margin_w = 8;

        for(int i=0;i<data.length;i++){
            for(int j=0;j<data[i].length;j++){
                canvas.save();
                Rect tmpRect = new Rect(j*fretPartWidth+m_margin_h,
                        i*fretHeight+m_margin_h,
                        j*fretPartWidth+fretPartWidth-m_margin_w,
                        i*fretHeight+fretHeight-m_margin_h);
                Paint tmpPaint = new Paint();
                int debugging = data[i][j];
                Log.v(TAG, "applyData: "+i+"/"+j+" color:"+debugging);
                tmpPaint.setColor(data[i][j]);
                tmpPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                tmpPaint.setAntiAlias(true);
                canvas.drawRect(tmpRect, tmpPaint);
                canvas.restore();
            }
        }
    }

    private int getColor(int intensity){
        int[] colors = {0xff0000,0xbf4000,0x8c7300,0x59a600,0x26d900,0x00ff00};
        if(intensity>=0 && intensity<colors.length){return pyToAndroidColor(colors[intensity]);}
        return colorBase;
    }

    private int getIntensityLevel(double raw){
        int index = 0;
        for(int i = 15; i>9;i--){
            if(raw > i){
                return index;
            }
            index = index + 1;
        }
        return -1;
    }

    public void resetInternalData(){
        // call invalidate  additional
        for(int i=0;i<data.length;i++){
            for(int j=0;j<data[0].length;j++){
                data[i][j] = colorBase;
            }
        }
    }

    public void setDataViaRaw(double[][] newInput){
        int[][] newData = new int[m_count_frets][m_count_strings];
        for(int i=0;i<newInput.length;i++){
            for(int j=0;j<newInput[i].length;j++){
                newData[i][j] = getColor(getIntensityLevel(newInput[i][j]));
            }
        }
        data = newData;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
