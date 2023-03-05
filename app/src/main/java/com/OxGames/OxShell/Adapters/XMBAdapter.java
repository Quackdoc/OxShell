package com.OxGames.OxShell.Adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.OxGames.OxShell.Data.HomeItem;
import com.OxGames.OxShell.Data.XMBItem;
import com.OxGames.OxShell.Interfaces.XMBAdapterListener;
import com.OxGames.OxShell.R;
import com.OxGames.OxShell.Views.XMBView;

import java.util.ArrayList;
import java.util.List;

public class XMBAdapter extends XMBView.Adapter<XMBAdapter.XMBViewHolder> {
    private Context context;
    private ArrayList<ArrayList<XMBItem>> items;
    private Typeface font;

//    public XMBAdapter(Context context, XMBItem... items) {
//        this.context = context;
//        this.items = items.clone();
//        font = Typeface.createFromAsset(context.getAssets(), "Fonts/exo.regular.otf");
//    }
//    public XMBAdapter(Context context, List<XMBItem> items) {
//        this.context = context;
//        this.items = items.toArray(new XMBItem[0]);
//        font = Typeface.createFromAsset(context.getAssets(), "Fonts/exo.regular.otf");
//    }
    public XMBAdapter(Context context, ArrayList<ArrayList<XMBItem>> items) {
        this.context = context;
        ArrayList<ArrayList<Object>> casted = new ArrayList<>();
        for (ArrayList<XMBItem> column : items)
            casted.add(new ArrayList<>(column));
        setItems(casted);
        font = Typeface.createFromAsset(context.getAssets(), "Fonts/exo.regular.otf");
    }

    @NonNull
    @Override
    public XMBViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = null;
        if (viewType == XMBView.CATEGORY_TYPE)
            view = layoutInflater.inflate(R.layout.xmb_cat, null);
        else if (viewType == XMBView.ITEM_TYPE)
            view = layoutInflater.inflate(R.layout.xmb_item, null);
        else if (viewType == XMBView.INNER_TYPE)
            view = layoutInflater.inflate(R.layout.xmb_inner_item, null);
        return new XMBViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull XMBViewHolder holder, Integer... position) {
        XMBItem item = null;
        if (position[1] < items.get(position[0]).size()) // empty item condition
            item = (XMBItem)getItem(position);
        holder.bindItem(item);
    }
    @Override
    public int getItemCount(boolean withInnerItems) {
        int size = 0;
        for (List<XMBItem> column : items) {
            if (column != null)
                size += column.size();
            if (withInnerItems)
                for (XMBItem item : column)
                    if (item != null)
                        size += item.getInnerItemCount();
        }
        return size;
    }

    @Override
    public int getColumnCount() {
        return items.size();
    }

    @Override
    public int getColumnSize(int columnIndex) {
        return items.get(columnIndex).size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull XMBViewHolder holder) {

    }
    @Override
    public Object getItem(Integer... position) {
        XMBItem current = null;
        if (position != null && position.length > 0) {
            current = items.get(position[0]).get(position[1]);
            for (int i = 2; i < position.length; i++)
                current = current.getInnerItem(position[i]);
        }
        return current;
    }

    @Override
    public ArrayList<ArrayList<Object>> getItems() {
        ArrayList<ArrayList<Object>> casted = new ArrayList<>();
        for (ArrayList<XMBItem> column : items)
            casted.add(new ArrayList<>(column));
        return casted;
    }

    @Override
    public void setItems(ArrayList<ArrayList<Object>> items) {
        this.items = new ArrayList<>();
        for (ArrayList<Object> column : items) {
            ArrayList<XMBItem> casted = new ArrayList<>();
            for (Object item : column)
                casted.add((XMBItem)item);
            this.items.add(casted);
        }
    }

    @Override
    public boolean isColumnHead(Integer... position) {
        XMBItem item = (XMBItem)getItem(position);
        return item.obj == null && !(item instanceof HomeItem);
    }

    @Override
    public boolean hasInnerItems(Integer... position) {
        XMBItem current = (XMBItem)getItem(position);
        //Log.d("XMBView", "Checking if " + current.title + " has inner items? " + current.hasInnerItems());
        return current != null && current.hasInnerItems();
    }
    @Override
    public int getInnerItemCount(Integer... position) {
        XMBItem current = (XMBItem)getItem(position);
        return current != null ? current.getInnerItemCount() : 0;
    }

    public class XMBViewHolder extends XMBView.ViewHolder {
        public XMBViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        public void bindItem(XMBItem item) {
            TextView title = itemView.findViewById(R.id.title);
            title.setText(item != null ? item.title : "Empty");
            title.setSelected(true);
            title.setTypeface(font);
            title.setVisibility(isHideTitleRequested() ? View.GONE : View.VISIBLE);

            //ImageView superIcon = itemView.findViewById(R.id.typeSuperIcon);
            //superIcon.setVisibility(View.GONE);
            //superIcon.setVisibility(((HomeItem)item).type == HomeItem.Type.assoc ? View.VISIBLE : View.GONE);

            ImageView img = itemView.findViewById(R.id.typeIcon);
            ImageView highlight = itemView.findViewById(R.id.iconGlow);
            Drawable icon = item != null ? item.getIcon() : ContextCompat.getDrawable(context, R.drawable.ic_baseline_block_24);
            if (icon == null)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_question_mark_24);
            img.setBackground(icon);
            highlight.setBackground(icon.getConstantState().newDrawable());
            highlight.setVisibility(isHighlighted() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    protected void shiftItemHorizontally(int toBeMovedColIndex, int toBeMovedLocalIndex, int moveToColIndex, int moveToLocalIndex, boolean createColumn) {
        //Log.d("XMBAdapter", "Moving item [" + toBeMovedColIndex + ", " + toBeMovedLocalIndex + "] => [" + moveToColIndex + ", " + moveToLocalIndex + "] Create column: " + createColumn);
        XMBItem toBeMoved = items.get(toBeMovedColIndex).get(toBeMovedLocalIndex);
        if (createColumn) {
            removeSubItem(toBeMovedColIndex, toBeMovedLocalIndex);
            createColumnAt(moveToColIndex, toBeMoved);
        } else {
            addSubItem(moveToColIndex, moveToLocalIndex, toBeMoved);
            removeSubItem(toBeMovedColIndex, toBeMovedLocalIndex);
        }
    }
    @Override
    protected void shiftItemVertically(int startColIndex, int fromLocalIndex, int toLocalIndex) {
        XMBItem toBeMoved = items.get(startColIndex).get(fromLocalIndex);
        removeSubItem(startColIndex, fromLocalIndex);
        addSubItem(startColIndex, toLocalIndex, toBeMoved);
    }
    @Override
    public void addSubItem(int columnIndex, int localIndex, Object toBeAdded) {
        items.get(columnIndex).add(localIndex, (XMBItem)toBeAdded);
        fireSubItemAddedEvent(columnIndex, localIndex);
    }
    @Override
    public void removeSubItem(int columnIndex, int localIndex) {
        items.get(columnIndex).remove(localIndex);
        fireSubItemRemovedEvent(columnIndex, localIndex);
        removeColIfEmpty(columnIndex);
    }
    @Override
    public void createColumnAt(int columnIndex, Object head) {
        ArrayList<XMBItem> newColumn = new ArrayList<>();
        newColumn.add((XMBItem)head);
        items.add(columnIndex, newColumn);
        fireColumnAddedEvent(columnIndex);
        fireSubItemAddedEvent(columnIndex, 0);
    }
    @Override
    public void removeColumnAt(int columnIndex) {
        items.remove(columnIndex);
        fireColumnRemovedEvent(columnIndex);
    }
    private void removeColIfEmpty(int columnIndex) {
        if (items.get(columnIndex).size() <= 0)
            removeColumnAt(columnIndex);
    }
    private void fireColumnAddedEvent(int columnIndex) {
        for (XMBAdapterListener listener : listeners)
            if (listener != null)
                listener.onColumnAdded(columnIndex);
    }
    private void fireColumnRemovedEvent(int columnIndex) {
        for (XMBAdapterListener listener : listeners)
            if (listener != null)
                listener.onColumnRemoved(columnIndex);
    }
    private void fireSubItemAddedEvent(int columnIndex, int localIndex) {
        for (XMBAdapterListener listener : listeners)
            if (listener != null)
                listener.onSubItemAdded(columnIndex, localIndex);
    }
    private void fireSubItemRemovedEvent(int columnIndex, int localIndex) {
        for (XMBAdapterListener listener : listeners)
            if (listener != null)
                listener.onSubItemRemoved(columnIndex, localIndex);
    }
}
