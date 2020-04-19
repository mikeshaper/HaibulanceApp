package com.example.haibulance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * this class is used to set recycler view (used in analysis activity)
 */
public class RecyclerView_Config {
    private Context mContext;
    private UsersAdapter mUsersAdapter;
    public void setmConfig(RecyclerView recyclerView, Context context, List<User> users, List<String> keys){
        mContext = context;
        mUsersAdapter = new UsersAdapter(users, keys);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(mUsersAdapter);
    }

    class UserItemView extends RecyclerView.ViewHolder{
        private TextView mName;
        private TextView mEmail;
        private TextView tv1;
        private TextView tv2;

        private String key;

        public UserItemView(ViewGroup parent){
            super(LayoutInflater.from(mContext).
                    inflate(R.layout.users_list_item, parent, false));
            mName = itemView.findViewById(R.id.user_name);
            mEmail = itemView.findViewById(R.id.user_email);
            tv1 = itemView.findViewById(R.id.tv1);
            tv2 = itemView.findViewById(R.id.tv2);
        }
        public void bind(User user, String key){
            mName.setText(user.getName());
            mEmail.setText(user.getEmail());
            tv1.setText(user.getPassword());
            tv2.setText(user.getPassword());
            this.key = key;
        }

    }
    class UsersAdapter extends RecyclerView.Adapter<UserItemView>{
        private List<User> mUserList;
        private List<String> mKeys;

        public UsersAdapter(List<User> mUserList, List<String> mKeys) {
            this.mUserList = mUserList;
            this.mKeys = mKeys;
        }

        @NonNull
        @Override
        public UserItemView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserItemView(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull UserItemView holder, int position) {
            holder.bind(mUserList.get(position), mKeys.get(position));
        }

        @Override
        public int getItemCount() {
            return mUserList.size();
        }
    }

}
