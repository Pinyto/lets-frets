package de.tudarmstadt.tk.smartguitarcontrol.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.LinkedList;


import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.baseClass;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.utility.utils;

public class TabBoard extends View{

    private int height, width = 0;
    private float heightBit, widthBit = 0;
    private float borderWidth;
    private float partHeight, partWidth = 0;
    private float topRowHeight;
    private float topRowBorder;
    private int strokeWidth;

    private boolean isInit = false;
    private boolean isDataInit = false;
    private boolean isTurned = false;
    private TabBoardPosition[][] positionMatrix = new TabBoardPosition[4][6];
    private boolean[] stringsToHit = new boolean[6];
    private final static String TAG = "TabBoard";

    public TabBoard(Context context) {
        super(context);
    }

    public TabBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TabBoard, 0,0);
        try{
            isTurned = a.getBoolean(R.styleable.TabBoard_turned, false);
        }finally {
            a.recycle();
        }
    }

    public TabBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initData(){
        isDataInit = true;
        for(int i=0;i<stringsToHit.length;i++){
            stringsToHit[i]=false;
        }
        for(int i=0;i<positionMatrix.length;i++){
            for(int j=0;j<positionMatrix[i].length;j++){
                positionMatrix[i][j]=null;
            }
        }
    }

    private void initFretBoard(){
        if(!isDataInit){
            initData();
        }
        isInit = true;

        height = getHeight();
        width = getWidth();

        heightBit = height/17;
        widthBit = width/12;
        borderWidth = Math.min(heightBit,widthBit);
        topRowHeight = 2*heightBit;
        partWidth = 2*widthBit;
        partHeight = 3*heightBit;

        strokeWidth = resolveStrokeWidth();
        topRowBorder = topRowHeight + borderWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!isInit){
            initFretBoard();
        }
        //canvas.drawColor(Color.LTGRAY);
        applyData(canvas);

        super.onDraw(canvas);
        Log.d(TAG, "onDraw: " + prettyPrintMatrix(positionMatrix));
        if(isTurned){
            this.setRotation(270);
        }
    }

    protected void applyData(Canvas canvas){
        Paint tmpPaint = new Paint();
        drawBaseGrid(canvas, tmpPaint);
        tmpPaint.setAntiAlias(true);
        tmpPaint.setStrokeWidth(strokeWidth);

        //Draw Fingers
        LinkedList<TabBoardPosition> queue = new LinkedList<>();
        for(int i=0;i<positionMatrix.length;i++){
            // Queue for grips that need to be combined
            for(int j=0;j<positionMatrix[i].length;j++){
                TabBoardPosition ele = positionMatrix[i][j];
                if(!queue.isEmpty()){
                    if(null != ele){
                        if(ele.getFinger() == queue.getFirst().getFinger() &&
                                ele.getFret()==queue.getFirst().getFret()){
                            //If new Line,
                            queue.add(ele);
                        }else{
                            drawPoints(canvas,tmpPaint,queue);
                            queue.clear();
                            queue.add(ele);
                        }
                    }else{
                        drawPoints(canvas,tmpPaint,queue);
                        queue.clear();
                    }
                } else if (null != ele) {
                    queue.add(ele);
                }
            }
        }
        if(!queue.isEmpty()){
            drawPoints(canvas,tmpPaint,queue);
            queue.clear();
        }

        //Draw top row
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String topRowSwitch = sp.getString("singleGrip_indicatorAtTab","1");

        // Skip draw any information at top if disabled in settings
        if(!topRowSwitch.equals("0")){
            tmpPaint.setTextAlign(Paint.Align.CENTER);
            tmpPaint.setTextSize(topRowHeight * (float) 0.8);
            tmpPaint.setColor(Color.BLACK);
            for(int i=0;i<stringsToHit.length;i++){
                //Moving tab description down a midge
                float y = topRowBorder/2 + heightBit;
                float x = i * partWidth+borderWidth;
                if(stringsToHit[i]){
                    //Check if settings allow additional information
                    if(!topRowSwitch.equals("4")){
                        TabBoardPosition tmpTBP = findFarthestTBP(i);
                        String topDesc;
                        if(null==tmpTBP){
                            topDesc = "0";
                        }else{
                            if(!topRowSwitch.equals("3")){
                                if(topRowSwitch.equals("1")){
                                    // Switched by settings
                                    topDesc = String.valueOf(tmpTBP.getFret()+1);
                                }else{
                                    topDesc = getTextForFinger(tmpTBP.getFinger());
                                }
                            }else{
                                topDesc = "";
                            }
                        }
                        canvas.drawText(topDesc,x,y,tmpPaint);
                    }
                }else{
                    canvas.drawText("X",x,y,tmpPaint);
                }
            }
        }
    }

    private void drawBaseGrid(Canvas canvas, Paint tmpPaint){
        float tmp = heightBit;
        tmpPaint.setStrokeWidth(tmp);
        tmpPaint.setAntiAlias(true);
        tmpPaint.setColor(Color.BLACK);
        canvas.drawLine(borderWidth, topRowBorder+heightBit/2,
                borderWidth+10*widthBit,topRowBorder+heightBit/2,tmpPaint);
        tmpPaint.setStrokeWidth(strokeWidth);
        //drawing vertical
        for(int i = 0;i<6;i++){
            float x = borderWidth+i*partWidth;
            float y_start = topRowBorder;
            float y_end = y_start+12*heightBit+heightBit/2;
            canvas.drawLine(x,y_start,x,y_end,tmpPaint);
        }
        //drawing horizontal
        for(int i = 1;i<5;i++){
            float x_start = borderWidth;
            float x_end = x_start+widthBit*10;
            float y = topRowHeight+heightBit+heightBit/2+partHeight*i;
            canvas.drawLine(x_start,y,x_end,y,tmpPaint);
        }
    }

    private void drawPoints(Canvas canvas, Paint tmpPaint, LinkedList<TabBoardPosition> queue){
        //draws rect with round corners or single circle, make sure queue is not empty.
        float base = topRowHeight+heightBit/2 +borderWidth + partHeight/2;
        int shapeSize = queue.size();
        float r1 = borderWidth* (float) 0.9;
        float r2 = borderWidth* (float) 0.8;

        TabBoardPosition tbp = queue.getFirst();
        if(shapeSize==1){
            float y = base+tbp.getFret()*partHeight;
            float x = tbp.getString() * partWidth+borderWidth;
            tmpPaint.setColor(Color.BLACK);
            canvas.drawCircle(x,y,r1,tmpPaint);
            tmpPaint.setColor(getColorForFinger(tbp.getFinger()));
            canvas.drawCircle(x,y,r2,tmpPaint);
            drawNumberAtPositionIfSet(canvas,tbp.getFinger(),x,y,tmpPaint);
        }else{
            //Draw custom shape
            float left = tbp.getString() * partWidth + borderWidth;
            float right = (tbp.getString()+shapeSize-1) * partWidth + borderWidth;
            float centerY = base + tbp.getFret()*partHeight;

            tmpPaint.setColor(Color.BLACK);
            float mod = r1;
            canvas.drawRoundRect(left-mod,centerY-mod,right+mod,centerY+mod,mod,mod,tmpPaint);
            mod = r2;
            tmpPaint.setColor(getColorForFinger(tbp.getFinger()));
            canvas.drawRoundRect(left-mod,centerY-mod,right+mod,centerY+mod,mod,mod,tmpPaint);
            drawNumberAtPositionIfSet(canvas,tbp.getFinger(),(left+right)/2,centerY,tmpPaint);
        }
    }

    private TabBoardPosition findFarthestTBP(int string_id){
        for(int i = positionMatrix.length-1;i>=0;i--){
            TabBoardPosition ele = positionMatrix[i][string_id];
            if (null != ele) {
                return ele;
            }
        }
        return null;
    }

    private int getColorForFinger(int finger){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        if(sp.getString("singleGrip_indicatorPos","0").equals("2")){
            return Color.BLACK;
        }
        switch (finger){
            case 4:
                return sp.getInt("color_f4",Color.BLUE);
            case 3:
                return sp.getInt("color_f3",Color.BLUE);
            case 2:
                return sp.getInt("color_f2",Color.BLUE);
            case 1:
                return sp.getInt("color_f1",Color.BLUE);
            case 0:
                return sp.getInt("color_f0",Color.BLUE);
            case -1:
                return sp.getInt("color_any",Color.BLUE);
            default:
                return Color.BLUE;
        }
    }

    private String getTextForFinger(int finger){
        switch (finger){
            case -1:
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                return sp.getString("singleGrip_anyFinger","0");
            default:
                return String.valueOf(finger);
        }
    }

    private void drawNumberAtPositionIfSet(Canvas canvas, int fingerID, float x, float y, Paint oldPaint){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        //If drawing number is set, draw them at given coors. Reduce base line by half of the height
        if(sp.getString("singleGrip_indicatorPos","0").equals("1")){
            Paint tmpPaint = new Paint(oldPaint);
            tmpPaint.setColor(Color.BLACK);
            tmpPaint.setTextAlign(Paint.Align.CENTER);
            tmpPaint.setTextSize(topRowHeight * (float) 0.7);
            canvas.drawText(getTextForFinger(fingerID),x,y+(tmpPaint.getTextSize()/3),tmpPaint);
        }
    }

    private int resolveStrokeWidth(){
        float shortSide = Math.min(height,width);
        if (shortSide > 2000){
            return 32;
        } else if (shortSide > 1000){
            return 16;
        } else if (shortSide > 750){
            return 12;
        } else if (shortSide > 500){
            return 8;
        } else if (shortSide > 250){
            return 4;
        } else if (shortSide > 125){
            return 2;
        } else {
            return 1;
        }
    }

    private boolean anyMorePositionsOnString(int string_id){
        for(int i=0;i<positionMatrix.length;i++){
            if(positionMatrix[i][string_id]!=null){
                return true;
            }
        }
        return false;
    }

    private void removeAllPositionsOnString(int x){
        for(TabBoardPosition[] tbpRow:positionMatrix){
            tbpRow[x] = null;
        }
    }

    private int resolveHeight(float posY){
        float top = topRowBorder;
        float rowHeight = partHeight;
        if(posY<top){
            return -1;
        }else if(posY<(top+rowHeight)){
            return 0;
        }else if(posY<(top+rowHeight*2)){
            return 1;
        }else if(posY<(top+rowHeight*3)){
            return 2;
        }else{
            return 3;
        }
    }

    private int resolveWidth(float posX) {
        float base = borderWidth-widthBit;
        if(posX<base+partWidth){
            return 0;
        }else if(posX<base+partWidth*2){
            return 1;
        }else if(posX<base+partWidth*3){
            return 2;
        }else if(posX<base+partWidth*4){
            return 3;
        }else if(posX<base+partWidth*5){
            return 4;
        }else{
            return 5;
        }
    }

    public boolean isClickOnTopRow(float positionY) {
        return positionY < (topRowBorder);
    }

    private void handleTopDelete(int idx){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean autoDelete = sp.getBoolean("singleGrip_removePositions",false);
        if(stringsToHit[idx]){
            stringsToHit[idx] = false;
            if(autoDelete){
                removeAllPositionsOnString(idx);
            }
        }else{
            //Nothing to delete, consider adding user notification
            Log.d(TAG, "handleTopDelete: String at position given not set"+idx);
        }
    }

    private int[] handleClick(int idx, int idy, int fingerID){
        //On return data is x and y swapped (y,x,finger)
        if(idy==-1){
            //top row
            switch (fingerID){
                case -2:
                    handleTopDelete(idx);
                    return null;
                case -1:
                    stringsToHit[idx] = true;
                    return null;
                default:
                    //neither any finger nor delete, should not happen with new popup
                    baseClass.toast(getContext(),getContext().getString(R.string.tabBoard_hint_use_any_finger));
                    return null;
            }
        }else{
            //not top row
            int[] data = {idy,idx,fingerID};
            handleSingleData(data);
            return data;
        }
    }

    public int[][] handleOnClick(float x, float y, int fingerID){
        //Check if click in first part
        //Check on which row
        //Check on which column
        int idy = resolveHeight(y);
        int idx = resolveWidth(x);
        Log.d(TAG, "handleOnClick: positions->"+idy+"/"+idx);
        //Pack result in array to hold conformity with overload methods
        int[][] result = new int[1][3];
        result[0] = handleClick(idx,idy,fingerID);
        return result;
    }

    public int[][] handleOnClick(float x, float y, int fingerID, float secondX){
        //Check if click in first part
        //Check on which row
        //Check on which column
        int idy = resolveHeight(y);
        int idx = resolveWidth(x);
        int offset_x = resolveWidth(secondX);
        Log.d(TAG, "handleOnClick: positions->"+idy+"/ ("+idx+":"+offset_x+")");
        if(idx == offset_x){return handleOnClick(x,y,fingerID);}

        int[] range;
        if(idx>offset_x){
            range = utils.generateRange(offset_x,idx+1);
        }else{
            range = utils.generateRange(idx,offset_x+1);
        }
        int[][] result = new int[range.length][3];
        for(int i=0;i<range.length;i++){
            int curX = range[i];
            result[i] = handleClick(curX, idy, fingerID);
        }
        return result;
    }

    private void handleTBP(TabBoardPosition tbp){
        if(!isDataInit){
            initData();
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        if(tbp.getFinger()==-2){
            //delete
            if(null!=positionMatrix[tbp.getFret()][tbp.getString()]){
                positionMatrix[tbp.getFret()][tbp.getString()] = null;
                if(sp.getBoolean("singleGrip_removeString",true)){
                    if(!anyMorePositionsOnString(tbp.string)){
                        stringsToHit[tbp.getString()]=false;
                    }
                }
            }else{
                //noting to delete
                Log.d(TAG, "handleTBP: Nothing to delete...");
            }
        }else{
            positionMatrix[tbp.getFret()][tbp.getString()] = tbp;

            if(sp.getBoolean("singleGrip_addString",true)){
                stringsToHit[tbp.getString()] = true;
            }
        }
    }

    public void handleSingleData(Position position){
        TabBoardPosition tbp = new TabBoardPosition(position.getPos(),
                position.getString_number(),
                position.getFinger());
        handleTBP(tbp);
    }

    public void handleSingleData(int[] data){
        if(data.length==3){
            if(!(data[0]>-1 && data[0]<5 && data[1]>-1 && data[1]<6 && data[2] > -3 && data[2]<6)){
                Log.w(TAG, "handleSingleData: Data not in range!");
                return;
            }
            TabBoardPosition tbp = new TabBoardPosition(data[0],data[1],data[2]);
            handleTBP(tbp);
        }
    }

    public void setStringsToHit(String toHit){
        // toHit in string binary e.g. "111000"
        if(!isDataInit){
            initData();
        }
        for(int i=0;i<toHit.length();i++){
            stringsToHit[i]=toHit.charAt(i)=='1';
        }
    }

    public String getStringsToHit(){
        String result = "";
        for(boolean tmp : stringsToHit){
            if(tmp){
                result = result.concat("1");
            }else{
                result = result.concat("0");
            }
        }
        return result;
    }

    public void resetInternalData(){
        initData();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public float getPartWidth() {
        return partWidth;
    }

    public String prettyPrintMatrix(TabBoardPosition[][] boardPositions){
        String result = "\n";
        for(TabBoardPosition[] fretArray:boardPositions){
            for(TabBoardPosition tbp:fretArray){
                if(null!=tbp){
                    result = result + tbp.getFinger() + "\t";
                }else{
                    result = result + "X" + "\t";
                }
            }
            result = result + "\n";
        }
        return result;
    }

    private class TabBoardPosition{
        private int fret;//position from bridge, first fret is 0
        private int string;//which string, most-left is 0
        private int finger;//which finger ? 0 thumb, 4 is pinky , -1 is any, (-2) is remove

        TabBoardPosition(int fret, int string, int finger){
            this.fret = fret;
            this.string = string;
            this.finger = finger;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if(null!=obj){
                if(obj instanceof TabBoardPosition){
                    TabBoardPosition c_obj = (TabBoardPosition) obj;
                    //Check all attributes
                    if(this.fret==c_obj.fret && this.string==c_obj.string && this.finger==c_obj.finger){
                        return true;
                    }
                }
            }
            return false;
        }

        @NonNull
        @Override
        public String toString() {
            return "["+this.fret+"|"+this.string+"] =>"+finger;
        }

        public int getFret() {
            return fret;
        }

        public int getString() {
            return string;
        }

        public int getFinger() {
            return finger;
        }

    }

}
