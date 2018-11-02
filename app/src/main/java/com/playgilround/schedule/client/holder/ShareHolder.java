package com.playgilround.schedule.client.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.playgilround.schedule.client.R;

/**
 * 18-11-02
 * 공유 요청 스케줄 관련 ViewHolder
 */
public class ShareHolder extends RecyclerView.ViewHolder {

    public TextView tvTitle; //스케줄 이름
    public TextView tvName; //스케줄 요청자
    public TextView tvTime; //스케줄 생성 시간

    public ShareHolder(View itemView) {
        super(itemView);

        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvName = itemView.findViewById(R.id.tvName);
    }
}
