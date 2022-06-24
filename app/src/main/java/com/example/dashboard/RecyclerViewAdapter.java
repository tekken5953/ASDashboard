/**
 * 에어시그널 태블릿 대쉬보드 (사용자용)
 * 개발자 LeeJaeYoung (jy5953@airsignal.kr)
 * 개발시작 2022-06-20
 */

package com.example.dashboard;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private ArrayList<RecyclerViewItem> mData;

    // 생성자에서 데이터 리스트 객체를 전달받음.
    RecyclerViewAdapter(ArrayList<RecyclerViewItem> list) {
        mData = list;
    }

    // onCreateViewHolder() - 아이템 뷰를 위한 뷰홀더 객체 생성하여 리턴.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.dashboard_listitem, parent, false);
        ViewHolder vh = new ViewHolder(view);

        return vh;
    }

    private OnItemClickListener mListener = null;

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    // OnItemClickListener 리스너 객체 참조를 어댑터에 전달하는 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    // onBindViewHolder() - position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시.
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        RecyclerViewItem item = mData.get(position);

        holder.title.setText(item.getTitle());
        holder.number.setText(item.getNumber());
        holder.unit.setText(item.getUnit());
        holder.status.setText(item.getStatus());

        //양호도 별 텍스트 컬러 설정
        if (holder.status.getText().toString().equals("좋음") ||holder.status.getText().toString().equals("Good")) {
            holder.status.setTextColor(Color.parseColor("#5CC2E4"));
            holder.number.setTextColor(Color.parseColor("#5CC2E4"));
        } else if (holder.status.getText().toString().equals("나쁨") || holder.status.getText().toString().equals("Bad")) {
            holder.status.setTextColor(Color.parseColor("#FBC93D"));
            holder.number.setTextColor(Color.parseColor("#FBC93D"));
        } else if (holder.status.getText().toString().equals("매우나쁨") || holder.status.getText().toString().equals("Very Bad")) {
            holder.status.setTextColor(Color.parseColor("#FB4F4F"));
            holder.number.setTextColor(Color.parseColor("#FB4F4F"));
        } else if (holder.status.getText().toString().equals("보통") || holder.status.getText().toString().equals("Normal")){
            holder.status.setTextColor(Color.parseColor("#1ccf7f"));
            holder.number.setTextColor(Color.parseColor("#1ccf7f"));
        } else {
            holder.status.setTextColor(Color.parseColor("#A0A0A0"));
            holder.number.setTextColor(Color.parseColor("#A0A0A0"));
        }
    }

    // getItemCount() - 전체 데이터 갯수 리턴.
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스.
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, number, unit, status;

        ViewHolder(final View itemView) {
            super(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    if (position != RecyclerView.NO_POSITION) {
                        if (mListener != null) {
                            mListener.onItemClick(v, position);
                        }
                    }
                }
            });

            // 뷰 객체에 대한 참조. (hold strong reference)
            title = itemView.findViewById(R.id.listItemTitle);
            number = itemView.findViewById(R.id.listItemNumber);
            unit = itemView.findViewById(R.id.listItemUnit);
            status = itemView.findViewById(R.id.listItemStatus);
        }
    }
}
