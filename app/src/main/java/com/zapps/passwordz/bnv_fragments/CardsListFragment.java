package com.zapps.passwordz.bnv_fragments;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zapps.passwordz.AddCardActivity;
import com.zapps.passwordz.R;
import com.zapps.passwordz.dialogs.DeleteConfirmDialog;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.FirebaseHelper;
import com.zapps.passwordz.helper.Helper;
import com.zapps.passwordz.model.CardsModel;

import java.util.ArrayList;
import java.util.Collections;

public class CardsListFragment extends Fragment implements DeleteConfirmDialog.DeleteItemListener {
    public static final String TAG = "ZQ-CardsListFragment";
    private Context context;
    private TextView tvMessage;
    private ProgressBar progressBar;
    private LinearLayoutManager linearLayoutManager;
    private Parcelable scrollState;
    private Adapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);
        tvMessage = view.findViewById(R.id.tv_message);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        FloatingActionButton fabAddCard = view.findViewById(R.id.fab_add_cards);

        adapter = new Adapter();
        linearLayoutManager = new LinearLayoutManager(context);

        fabAddCard.setOnClickListener(view1 -> startActivity(new Intent(context, AddCardActivity.class)));
        Helper.autoHideFAB(recyclerView, fabAddCard);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        progressBar.setVisibility(View.VISIBLE);
        return view;
    }

    @Override
    public void onItemDeleted(String confirmCode) {
        int index = -1;
        for (int i = 0; i < adapter.list.size(); i++) {
            if (adapter.list.get(i).getCardNumber().equals(confirmCode)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            adapter.list.remove(index);
            adapter.notifyItemRemoved(index);
            if (adapter.list.size() == 0) {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(R.string.no_cards_found);
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final ArrayList<CardsModel> list;

        public Adapter() {
            list = new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_card_front, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.itemView.clearAnimation();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setData(list.get(position));
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fall_down);
            holder.itemView.startAnimation(animation);
        }

        public CardsModel getItem(int position) {
            return list.get(position);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private boolean contains(CardsModel model) {
            for (CardsModel cardsModel : list)
                if (cardsModel.equals(model)) return true;
            return false;
        }

        public void sort() {
            Collections.sort(list);
        }

        public void refresh(CardsModel[] updatedList) {
            // TODO: test if refresh is working correctly
            // removing deleted card models
            ArrayList<CardsModel> tempModels = new ArrayList<>(list);
            for (CardsModel cardsModel : tempModels)
                if (!cardsModel.isIn(updatedList)) {
                    int index = list.indexOf(cardsModel);
                    list.remove(index);
                    notifyItemRemoved(index);
                }

            // adding new card models
            ArrayList<CardsModel> newCards = new ArrayList<>();
            for (CardsModel newCard : updatedList)
                if (!this.contains(newCard)) {
                    newCards.add(newCard);
                    list.add(newCard);
                }
            sort();
            for (CardsModel newModel : newCards)
                notifyItemInserted(list.indexOf(newModel));
        }
    }

    public void refreshAdapter() {
        FirebaseHelper.getAllCards(context, new FirebaseHelper.CardsRetrieverListener() {
            @Override
            public void onSuccess(@NonNull CardsModel... cardsModels) {
                if (cardsModels.length == 0) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(R.string.no_cards_found);
                } else tvMessage.setVisibility(View.GONE);
                adapter.refresh(cardsModels);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(@NonNull String error) {
                progressBar.setVisibility(View.GONE);
                CToast.error(context, error);
            }
        });
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCardNumber, tvValidThrough, tvNameOnCard, tvCVV, tvCardType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardNumber = itemView.findViewById(R.id.tv_card_number);
            tvValidThrough = itemView.findViewById(R.id.tv_valid_through);
            tvNameOnCard = itemView.findViewById(R.id.tv_name_on_card);
            tvCVV = itemView.findViewById(R.id.tv_cvv);
            tvCardType = itemView.findViewById(R.id.tv_card_type);
            itemView.setOnLongClickListener(view -> {
                CardsModel model = adapter.getItem(getAbsoluteAdapterPosition());
                String confirmationText = getString(R.string.card_delete)
                        .replace("CARD_TYPE", "\"" + model.getCardType() + "\"")
                        .replace("CARD_NUMBER", "\"" + model.getCardNumber() + "\"");
                DeleteConfirmDialog dialog = new DeleteConfirmDialog(
                        DeleteConfirmDialog.ACTION_DELETE_CARD,
                        model.getPushId(),
                        confirmationText,
                        model.getCardNumber(),
                        CardsListFragment.this);
                dialog.show(getChildFragmentManager(), null);
                return true;
            });
        }

        public void setData(CardsModel data) {
            itemView.setBackground(data.cardBackground());
            tvCardNumber.setText(data.getCardNumber());
            tvValidThrough.setText(data.getValidThrough());
            tvNameOnCard.setText(data.getNameOnCard());
            tvCVV.setText(data.getCvv());
            tvCardType.setText(data.getCardType());
            tvCardNumber.setOnClickListener(view -> Helper.copyToClipboard(context, data.getCardNumber().replace(" ", "")));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        scrollState = linearLayoutManager.onSaveInstanceState();
    }

    @Override
    public void onStart() {
        super.onStart();
        linearLayoutManager.onRestoreInstanceState(scrollState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }
}
