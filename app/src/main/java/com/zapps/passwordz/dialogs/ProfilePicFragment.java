package com.zapps.passwordz.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.squareup.picasso.Picasso;
import com.zapps.passwordz.R;
import com.zapps.passwordz.helper.CToast;
import com.zapps.passwordz.helper.Helper;

public class ProfilePicFragment extends BottomSheetDialogFragment implements AdapterView.OnItemClickListener {
    private int[] profileImages;
    private Context context;
    private final ProfilePicChangeListener listener;
    public interface ProfilePicChangeListener {
        void onProfileChanged(int imgId);
    }

    public ProfilePicFragment(ProfilePicChangeListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_pic, container, false);
        profileImages = new int[]{R.drawable.ic_bee, R.drawable.ic_cat, R.drawable.ic_hen,
                R.drawable.ic_koala, R.drawable.ic_lion, R.drawable.ic_owl, R.drawable.ic_penguin,
                R.drawable.ic_toad, R.drawable.ic_toucan, R.drawable.ic_fox};
        GridView gridView = view.findViewById(R.id.grid_view);
        gridView.setAdapter(new Adapter(profileImages, context));
        gridView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            CToast.error(context, Helper.MESSAGE_FIREBASE_USER_NULL);
            return;
        }
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
        builder.setPhotoUri(Helper.drawableToUri(context, profileImages[i]));
        user.updateProfile(builder.build());
        listener.onProfileChanged(profileImages[i]);
        dismiss();
    }

    private static class Adapter extends BaseAdapter {
        private final int[] images;
        private final Context context;
        private LayoutInflater inflater;

        public Adapter(int[] images, Context context) {
            this.images = images;
            this.context = context;
        }

        @Override
        public int getCount() {
            return images.length;
        }

        @Override
        public Object getItem(int i) {
            return images[i];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (inflater == null)
                inflater = LayoutInflater.from(context);
            if (view == null)
                view = inflater.inflate(R.layout.row_profile_pic, viewGroup, false);
            ImageView imageView = view.findViewById(R.id.iv_profile_icon);
            Picasso.get().load(images[i]).into(imageView);
            return view;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }
}
