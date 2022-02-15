package com.zapps.passwordz;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.zapps.passwordz.dialogs.DeleteConfirmDialog;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.FirebaseHelper;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.model.LoginsModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// lists all logins for a single website
public class LoginsListActivity extends AppCompatActivity implements DeleteConfirmDialog.DeleteItemListener {
    private static final String TAG = "ZQ";
    public static final String PARAM_WEBSITE = "website";
    private String website;
    private Adapter adapter;
    private ProgressBar progressBar;
    private LinearLayoutManager linearLayoutManager;
    private Parcelable scrollState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logins_list);

        website = getIntent().getStringExtra(PARAM_WEBSITE);
        if (website == null || website.isEmpty()) {
            CToast.error(this, "An error occurred!");
            finish();
            return;
        }
        FloatingActionButton fab = findViewById(R.id.fabAddLogins);
        progressBar = findViewById(R.id.progressBar);
        ImageView ivWebsiteIcon = findViewById(R.id.ivWebsiteIcon);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        TextView tvWebsite = findViewById(R.id.tvWebsite);
        linearLayoutManager = new LinearLayoutManager(this);
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy < 0 && !fab.isShown())
                    fab.show();
                else if (dy > 0 && fab.isShown())
                    fab.hide();
            }
        });
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(LoginsListActivity.this, AddOrEditLoginsActivity.class);
            intent.putExtra(AddOrEditLoginsActivity.PARAM_WEBSITE, website);
            startActivity(intent);
        });
        Picasso.get().load(Helper.getFavIconUrl(website)).error(R.drawable.ic_internet).into(ivWebsiteIcon);
        tvWebsite.setText(website);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setHasFixedSize(true);
        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        refreshAdapter();
        progressBar.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            scrollState = null;
            refreshAdapter();
        });
    }

    private void refreshAdapter() {
        FirebaseHelper.getAllLogins(this, new FirebaseHelper.DataRetrieveListener() {
            @Override
            public void onSuccess(@NonNull LoginsModel... loginsModels) {
                ArrayList<ViewModel> list = new ArrayList<>();
                if (loginsModels.length == 0) {
                    finish();
                    return;
                }
                ArrayList<LoginsModel> filteredLogins = new ArrayList<>();
                for (LoginsModel loginsModel : loginsModels)
                    if (loginsModel.getWebsite().equals(website)) filteredLogins.add(loginsModel);
                for (LoginsModel loginsModel : filteredLogins)
                    list.add(new ViewModel(loginsModel.getUsername(), loginsModel.getPassword(), loginsModel.getLastModified(), loginsModel.getPushId()));
                Collections.sort(list);
                adapter.refresh(list);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(@NonNull String error) {
                progressBar.setVisibility(View.GONE);
                CToast.error(LoginsListActivity.this, error);
                finish();
            }
        });
    }

    @Override
    public void onItemDeleted(String confirmCode) {
        int index = -1;
        for (int i = 0; i < adapter.list.size(); i++) {
            if (adapter.list.get(i).getUsername().equals(confirmCode)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            adapter.list.remove(index);
            adapter.notifyItemRemoved(index);
            if (adapter.list.size() == 0) finish();
        }
    }

    private static class ViewModel implements Comparable<ViewModel> {
        private final String username, password, lastModified, pushId;

        public ViewModel(String username, String password, String lastModified, String pushId) {
            this.username = username;
            this.password = password;
            this.lastModified = lastModified;
            this.pushId = pushId;
        }

        public String getPushId() {
            return pushId;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getLastModified() {
            return lastModified;
        }

        public boolean equals(ViewModel viewModel) {
            if (viewModel == null) return false;
            return viewModel.getPushId().equals(getPushId())
                    && viewModel.getUsername().equals(getUsername())
                    && viewModel.getPassword().equals(getPassword())
                    && viewModel.getLastModified().equals(getLastModified());
        }

        @Override
        public int compareTo(ViewModel viewModel) {
            return getUsername().toLowerCase().compareTo(viewModel.getUsername().toLowerCase());
        }

    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUsername, tvLastModified;
        private final ImageView ivAccountOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLastModified = itemView.findViewById(R.id.tvLastModified);
            ivAccountOptions = itemView.findViewById(R.id.ivOptions);
        }

        public void setData(ViewModel data) {
            tvUsername.setText(data.getUsername());
            tvLastModified.setText(data.getLastModified());
            PopupMenu popupMenu = new PopupMenu(itemView.getContext(), ivAccountOptions);
            popupMenu.getMenuInflater().inflate(R.menu.menu_account_options, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.menuCopyUsername)
                    Helper.copyToClipboard(itemView.getContext(), adapter.list.get(getBindingAdapterPosition()).getUsername());
                if (id == R.id.menuCopyPassword)
                    Helper.copyToClipboard(itemView.getContext(), adapter.list.get(getBindingAdapterPosition()).getPassword());
                if (id == R.id.menuDelete) {
                    String confirmationText = getString(R.string.delete_confirmation_text)
                            .replace("USERNAME", "\"" + data.getUsername() + "\"")
                            .replace("WEBSITE", "\"" + website + "\"");
                    DeleteConfirmDialog dialog = new DeleteConfirmDialog(DeleteConfirmDialog.ACTION_DELETE_LOGIN, data.getPushId(), confirmationText, data.getUsername());
                    dialog.show(LoginsListActivity.this.getSupportFragmentManager(), null);
                }
                return true;
            });
            ivAccountOptions.setOnClickListener(view -> popupMenu.show());
            itemView.setOnClickListener(view -> {
                Intent intent = new Intent(itemView.getContext(), AddOrEditLoginsActivity.class);
                intent.putExtra(AddOrEditLoginsActivity.PARAM_PUSH_ID, data.getPushId());
                itemView.getContext().startActivity(intent);
            });
            itemView.setOnLongClickListener(view -> {
                popupMenu.show();
                return true;
            });
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        public static final String PAYLOAD_USERNAME = "username";
        public static final String PAYLOAD_LAST_MODIFIED = "lastModified";
        public static final String PAYLOAD_BOTH = "both";
        private final ArrayList<ViewModel> list;

        public Adapter() {
            this.list = new ArrayList<>();
        }

        public Adapter(ArrayList<ViewModel> list) {
            this.list = list;
        }

        private boolean hasPushId(String pushId, ArrayList<ViewModel> list) {
            for (ViewModel viewModel : list) if (viewModel.getPushId().equals(pushId)) return true;
            return false;
        }

        public void refresh(ArrayList<ViewModel> newList) {
            // when value is removed.
            ArrayList<ViewModel> tempModel = new ArrayList<>(list);
            for (ViewModel viewModel : tempModel) {
                if (!hasPushId(viewModel.getPushId(), newList)) {
                    int index = list.indexOf(viewModel);
                    list.remove(index);
                    notifyItemRemoved(index);
                }
            }

            // when a new item is added
            ArrayList<ViewModel> newModels = new ArrayList<>();
            for (ViewModel viewModel : newList) {
                if (!hasPushId(viewModel.getPushId(), list)) newModels.add(viewModel);
            }
            list.addAll(newModels);
            sort();
            for (ViewModel viewModel : newModels) {
                int index = list.indexOf(viewModel);
                notifyItemInserted(index);
            }

            // updating changed items
            for (int i = 0; i < list.size(); i++) {
                if (!list.get(i).equals(newList.get(i))) {
                    boolean sameUsername = list.get(i).getUsername().equals(newList.get(i).getUsername());
                    boolean sameLastModified = list.get(i).getLastModified().equals(newList.get(i).getLastModified());
                    list.set(i, newList.get(i));
                    if (!sameUsername && !sameLastModified) notifyItemChanged(i, PAYLOAD_BOTH);
                    else if (!sameUsername) notifyItemChanged(i, PAYLOAD_USERNAME);
                    else if (!sameLastModified) notifyItemChanged(i, PAYLOAD_LAST_MODIFIED);
                }
            }
        }

        public void sort() {
            Collections.sort(list);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.itemView.clearAnimation();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty())
                super.onBindViewHolder(holder, position, payloads);
            else for (Object payload : payloads) {
                if (payload.equals(PAYLOAD_BOTH)) {
                    holder.tvUsername.setText(list.get(position).getUsername());
                    holder.tvLastModified.setText(list.get(position).getLastModified());
                } else if (payload.equals(PAYLOAD_USERNAME))
                    holder.tvUsername.setText(list.get(position).getUsername());
                else if (payload.equals(PAYLOAD_LAST_MODIFIED))
                    holder.tvLastModified.setText(list.get(position).getLastModified());
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setData(list.get(position));
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fall_down);
            holder.itemView.startAnimation(animation);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scrollState = linearLayoutManager.onSaveInstanceState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        linearLayoutManager.onRestoreInstanceState(scrollState);
        refreshAdapter();
    }
}