package com.example.haibulance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportsRecyclerConfig {
    private Context mContext;
    private ReportsAdapter reportsAdapter;
    public void setmConfig(RecyclerView recyclerView, Context context, List<Report> reports, List<String> keys){
        mContext = context;
        reportsAdapter = new ReportsAdapter(reports, keys);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(reportsAdapter);
    }

    class ReportItemView extends RecyclerView.ViewHolder{
        private TextView desc;
        private TextView date;
        private TextView locName;
        private TextView picked;

        private String key;

        public ReportItemView(ViewGroup parent){
            super(LayoutInflater.from(mContext).inflate(R.layout.reports_list_item, parent, false));
            desc = itemView.findViewById(R.id.item_report_name);
            date = itemView.findViewById(R.id.item_report_date);
            locName = itemView.findViewById(R.id.item_loc_name);
            picked = itemView.findViewById(R.id.item_report_picked);

        }
        public void bind(Report report, String key){
            desc.setText(report.ToString());
            date.setText(report.getTime());
            locName.setText(report.getLocationName());
            picked.setText(report.getStatus());
            this.key = key;
        }
    }

    class ReportsAdapter extends RecyclerView.Adapter<ReportItemView>{
        private List<Report> mReportList;
        private List<String> mKeys;

        public ReportsAdapter(List<Report> mReportList, List<String> mKeys) {
            this.mReportList = mReportList;
            this.mKeys = mKeys;
        }

        @NonNull
        @Override
        public ReportItemView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ReportItemView(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportItemView holder, int position) {
            holder.bind(mReportList.get(position), mKeys.get(position));
        }

        @Override
        public int getItemCount() {
            return mReportList.size();
        }
    }

}
