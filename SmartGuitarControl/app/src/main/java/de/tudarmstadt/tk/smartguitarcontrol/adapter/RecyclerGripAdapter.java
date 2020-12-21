package de.tudarmstadt.tk.smartguitarcontrol.adapter;

import android.util.Pair;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;

import de.tudarmstadt.tk.smartguitarcontrol.R;
import de.tudarmstadt.tk.smartguitarcontrol.database.Grip;
import de.tudarmstadt.tk.smartguitarcontrol.database.Position;
import de.tudarmstadt.tk.smartguitarcontrol.database.TimestampConverter;
import de.tudarmstadt.tk.smartguitarcontrol.views.TabBoard;

public class RecyclerGripAdapter extends RecyclerView.Adapter<RecyclerGripAdapter.GripViewHolder> {

    public interface customClickListener{
        void onItemClick(int position);
    }

    private ArrayList<Pair<Grip,Position[]>> pairs;
    private final customClickListener listener;

    public RecyclerGripAdapter(customClickListener listener){
        pairs = new ArrayList<>();
        this.listener = listener;
    }


    @NonNull
    @Override
    public GripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grip_layout,parent,false);
            return new GripViewHolder(view,listener);

    }

    @Override
    public void onBindViewHolder(@NonNull GripViewHolder holder, int position) {
        holder.bind(pairs.get(position).first,pairs.get(position).second);
    }

    public Grip getGripViaPosition(int position){
        return  pairs.get(position).first;
    }

    public Pair<Grip,Position[]> getPair(int id){
        return pairs.get(id);
    }

    public Position[] getPositionsViaID(int id){
        return pairs.get(id).second;
    }

    public long[] getAllGripIDS(){
        long[] result = new long[pairs.size()];
        for(int i=0;i<result.length;i++){
            result[i] = pairs.get(i).first.getId();
        }
        return result;
    }

    public ArrayList<Pair<Grip,Position[]>> getAllPairs(){
        return pairs;
    }

    public void removeItem(int id){
        pairs.remove(id);
    }

    public void clear(){
        pairs.clear();
    }

    @Override
    public int getItemCount() {
        return pairs.size();
    }

    public void addGrip(Grip grip){
        Pair<Grip,Position[]> tmpPair = new Pair<>(grip,null);
        pairs.add(tmpPair);
    }

    public void addPair(Grip grip, Position[] positions){
        Pair<Grip,Position[]> tmpPair = new Pair<>(grip,positions);
        pairs.add(tmpPair);
    }

    public void addPairCollection(Collection<? extends Pair<Grip,Position[]>> collection){
        pairs.addAll(collection);
    }

    public static class GripViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {

        TextView GripName;
        TextView GripDatum;
        TextView GripPositionCnt;
        TabBoard tabBoard;
        customClickListener listener;

        GripViewHolder(@NonNull View itemView, customClickListener listener) {
            super(itemView);
            GripName = itemView.findViewById(R.id.grip_name);
            GripDatum = itemView.findViewById(R.id.grip_datum);
            GripPositionCnt = itemView.findViewById(R.id.grip_cnt_positions);
            tabBoard = itemView.findViewById(R.id.tabBoard);
            this.listener = listener;
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        void bind(final Grip grip, final Position[] positions){
            this.GripName.setText(grip.getName());
            if(grip.getDate()!=null){
                this.GripDatum.setText(TimestampConverter.dateToTimestamp(grip.getDate()));
            }
            if(positions != null){
                tabBoard.setStringsToHit(grip.getHitString());
                for(Position tmpPos:positions){
                    tabBoard.handleSingleData(tmpPos);
                }
            }else{
                tabBoard.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(getAdapterPosition());
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            //this check is not necessary, because the listener does not get set when null
            contextMenu.add(this.getAdapterPosition(),150,0,R.string.GENERIC_delete);
        }
    }

}
