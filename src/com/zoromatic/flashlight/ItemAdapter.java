package com.zoromatic.flashlight;

import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class ItemAdapter extends BaseAdapter {
    Context context;
    List<RowItem> rowItems;
     
    public ItemAdapter(Context context, List<RowItem> items) {
        this.context = context;
        this.rowItems = items;
    }
     
    /*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        //TextView txtTitle;
        TextView txtDesc;        
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
         
        LayoutInflater mInflater = (LayoutInflater) 
            context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_row, null);
            holder = new ViewHolder();
            holder.txtDesc = (TextView) convertView.findViewById(R.id.listlabel);
            holder.imageView = (ImageView) convertView.findViewById(R.id.listicon);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }
         
        RowItem rowItem = (RowItem) getItem(position);
         
        holder.txtDesc.setText(rowItem.getDesc());
        holder.imageView.setImageResource(rowItem.getImageId());        
         
        return convertView;
    }
 
    @Override
    public int getCount() {     
        return rowItems.size();
    }
 
    @Override
    public Object getItem(int position) {
        return rowItems.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return rowItems.indexOf(getItem(position));
    }
}