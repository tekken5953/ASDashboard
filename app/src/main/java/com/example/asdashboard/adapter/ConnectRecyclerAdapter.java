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

import com.example.asdashboard.model.ConnectRecyclerItem;
import com.example.asdashboard.R;

import java.util.ArrayList;

public class ConnectRecyclerAdapter extends RecyclerView.Adapter<ConnectRecyclerAdapter.ViewHolder> {
    private ArrayList<ConnectRecyclerItem> mData;

    // 생성자에서 데이터 리스트 객체를 전달받음.
    public ConnectRecyclerAdapter(ArrayList<ConnectRecyclerItem> list) {
        mData = list;
    }

    // onCreateViewHolder() - 아이템 뷰를 위한 뷰홀더 객체 생성하여 리턴.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.listitem_connectable_rv, parent, false);

        return new ViewHolder(view);
    }

    private OnItemClickListener mListener = null;

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    // OnItemClickListener 리스너 객체 참조를 어댑터에 전달하는 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    // onBindViewHolder() - position 에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시.
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ConnectRecyclerItem item = mData.get(position);

        holder.device_name.setText(item.getDevice_name());
        holder.device_img.setImageDrawable(item.getDevice_img());
        holder.device_address.setText(item.getDevice_address());

    }

    // getItemCount() - 전체 데이터 갯수 리턴.
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스.
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView device_img;
        TextView device_name, device_address, connect;

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

            // 뷰 객체에 대한 참조. (hold strong reference)
            device_img = itemView.findViewById(R.id.listItemConnDeviceImageIv);
            device_name = itemView.findViewById(R.id.listItemConnDeviceTitleTv);
            device_address = itemView.findViewById(R.id.listItemConnDeviceAddressTv);
            connect = itemView.findViewById(R.id.listItemConnDeviceConnTv);

        }
    }
}
