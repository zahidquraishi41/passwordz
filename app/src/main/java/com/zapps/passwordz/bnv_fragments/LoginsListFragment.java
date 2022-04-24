package com.zapps.passwordz.bnv_fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.zapps.passwordz.AddOrEditLoginsActivity;
import com.zapps.passwordz.LoginsListActivity;
import com.zapps.passwordz.R;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.FirebaseHelper;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.model.LoginsModel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

// displays list after grouping all logins by website
public class LoginsListFragment extends Fragment {
    public static final String TAG = "ZQ-LoginsListFragment";
    private ProgressBar progressBar;
    private Context context;
    private TextView tvMessage;
    private LinearLayoutManager linearLayoutManager;
    private Parcelable scrollState;
    private Adapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logins_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvMessage = view.findViewById(R.id.tv_message);
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        FloatingActionButton fabAddLogins = view.findViewById(R.id.fab_add_logins);

        linearLayoutManager = new LinearLayoutManager(context);
        adapter = new Adapter();

        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        recyclerView.setHasFixedSize(true);
        Helper.hideOnScroll(recyclerView, ((Activity) context).findViewById(R.id.bnv_main), fabAddLogins);
        recyclerView.setLayoutManager(linearLayoutManager);
        fabAddLogins.setOnClickListener(view1 -> startActivity(new Intent(context, AddOrEditLoginsActivity.class)));
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            scrollState = null;
            refreshAdapter();
        });
        progressBar.setVisibility(View.VISIBLE);

        return view;
    }

    private HashMap<String, Integer> counter(LoginsModel... loginsModels) {
        HashMap<String, Integer> map = new HashMap<>();
        for (LoginsModel model : loginsModels) {
            Integer count = map.getOrDefault(model.getWebsite(), 0);
            count += 1;
            map.put(model.getWebsite(), count);
        }
        return map;
    }

    // downloads new data and set it to adapter
    public void refreshAdapter() {
        FirebaseHelper.getAllLogins(context, new FirebaseHelper.DataRetrieveListener() {
            @Override
            public void onSuccess(@NonNull LoginsModel... loginsModels) {
                // if no logins is added then displaying message
                if (loginsModels.length == 0) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(R.string.no_logins_found);
                } else tvMessage.setVisibility(View.GONE);

                // counting the number of accounts in each website
                HashMap<String, Integer> map = counter(loginsModels);

                // converting the count to viewModels for displaying
                ArrayList<ViewModel> list = new ArrayList<>();
                for (String key : map.keySet()) {
                    ViewModel customModel = new ViewModel(key, map.getOrDefault(key, 0));
                    list.add(customModel);
                }
                Collections.sort(list);
                adapter.refresh(list);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(@NonNull String error) {
                progressBar.setVisibility(View.GONE);
                CToast.error(context, error);
            }
        });
    }

    private static class ViewModel implements Comparable<ViewModel> {
        private final String website;
        private Integer accountCount;

        public ViewModel(String website, Integer accountCount) {
            this.website = website;
            this.accountCount = accountCount;
        }

        public String getWebsite() {
            return website;
        }

        public int getAccountCount() {
            return accountCount;
        }

        public void setAccountCount(Integer accountCount) {
            this.accountCount = accountCount;
        }

        @Override
        public int compareTo(ViewModel viewModel) {
            return getWebsite().toLowerCase().compareTo(viewModel.getWebsite().toLowerCase());
        }

        public boolean equals(ViewModel viewModel) {
            if (viewModel == null) return false;
            return getWebsite().equals(viewModel.getWebsite())
                    && getAccountCount() == viewModel.getAccountCount();
        }

        /**
         * Returns first ViewModel whose 'website' equals to current model's website.
         */
        public ViewModel in(ArrayList<ViewModel> viewModels) {
            for (ViewModel viewModel : viewModels)
                if (viewModel.getWebsite().equals(getWebsite())) return viewModel;
            return null;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvSubtitle;
        private final ImageView ivFavIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            ivFavIcon = itemView.findViewById(R.id.iv_icon);
        }

        public void setData(ViewModel data) {
            tvTitle.setText(data.getWebsite());
            tvSubtitle.setText(MessageFormat.format("{0} Accounts", data.getAccountCount()));
            Picasso.get().load(Helper.getFavIconUrl(data.getWebsite())).placeholder(R.drawable.ic_internet).error(R.drawable.ic_internet).into(ivFavIcon);
            itemView.setOnClickListener(view1 -> {
                Intent intent = new Intent(itemView.getContext(), LoginsListActivity.class);
                intent.putExtra(LoginsListActivity.PARAM_WEBSITE, data.getWebsite());
                itemView.getContext().startActivity(intent);
            });
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final ArrayList<ViewModel> list;

        public Adapter() {
            list = new ArrayList<>();
        }

        public boolean requireRefresh(ArrayList<ViewModel> updatedList) {
            if (list.size() != updatedList.size()) return true;
            for (ViewModel updatedModel : updatedList) {
                boolean found = false;
                for (ViewModel oldModel : list) {
                    if (updatedModel.getWebsite().equals(oldModel.getWebsite())) {
                        found = true;
                        if (updatedModel.getAccountCount() == oldModel.getAccountCount())
                            break;
                        else return true;
                    }
                }
                if (!found) return true;
            }
            return false;
        }

        public void refresh(ArrayList<ViewModel> updatedList) {
            if (!requireRefresh(updatedList)) return;
            list.clear();
            list.addAll(updatedList);
            Collections.sort(list);
            notifyDataSetChanged();
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.itemView.clearAnimation();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_website_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setData(list.get(position));
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fall_down);
            holder.itemView.startAnimation(animation);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onStart() {
        super.onStart();
        linearLayoutManager.onRestoreInstanceState(scrollState);
    }

    @Override
    public void onPause() {
        super.onPause();
        scrollState = linearLayoutManager.onSaveInstanceState();
    }
}
