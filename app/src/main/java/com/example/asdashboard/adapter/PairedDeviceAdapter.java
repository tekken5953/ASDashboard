/**
 * 에어시그널 태블릿 대쉬보드 (사용자용)
 * 개발자 LeeJaeYoung (jy5953@airsignal.kr)
 * 개발시작 2022-06-20
 */

package com.example.asdashboard.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdashboard.model.PairedDeviceItem;
import com.example.asdashboard.R;

import java.util.ArrayList;

public class PairedDeviceAdapter extends RecyclerView.Adapter<PairedDeviceAdapter.ViewHolder> {
    private final ArrayList<PairedDeviceItem> mData;

    // 생성자에서 데이터 리스트 객체를 전달받음.
    public PairedDeviceAdapter(ArrayList<PairedDeviceItem> list) {
        mData = list;
    }

    // onCreateViewHolder() - 아이템 뷰를 위한 뷰홀더 객체 생성하여 리턴.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.listitem_recent_device, parent, false);

        return new ViewHolder(view);
    }

    private OnItemClickListener mListener = null;

    private OnItemLongClickListener longClickListener = null;

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View v, int position);
    }

    // OnItemClickListener 리스너 객체 참조를 어댑터에 전달하는 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }


    // onBindViewHolder() - position 에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시.
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        PairedDeviceItem item = mData.get(position);

        holder.paired_icon.setImageDrawable(item.getIcon());
        holder.paired_name.setText(item.getName());
        holder.paired_address.setText(item.getAddress());

        holder.itemView.setAlpha(0.5f);

    }

    // getItemCount() - 전체 데이터 갯수 리턴.
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스.
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView paired_icon;
        TextView paired_name, paired_address;

        ViewHolder(final View itemView) {
            super(itemView);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();

                if (position != RecyclerView.NO_POSITION) {
                    if (mListener != null) {
                        mListener.onItemClick(v, position);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();

                if (position != RecyclerView.NO_POSITION) {
                    if (longClickListener != null) {
                        longClickListener.onItemLongClick(v, position);
                    }
                }
                return false;
            });

            // 뷰 객체에 대한 참조. (hold strong reference)
            paired_icon = itemView.findViewById(R.id.listItemRecentIconIv);
            paired_name = itemView.findViewById(R.id.listItemRecentTitleTv);
            paired_address = itemView.findViewById(R.id.listItemRecentNumberTv);

        }
    }
}