package com.bignerdranch.android.criminalintent;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PhotoZoomFragment extends DialogFragment {
    private static final String ARG_PHOTO = "photo";

    @BindView(R.id.dialog_photo_zoom)
    ImageView mPhotoView;

    private Unbinder unbinder;

    public static PhotoZoomFragment newInstance(File photo) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PHOTO, photo);

        PhotoZoomFragment fragment = new PhotoZoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_photo, null);
        unbinder = ButterKnife.bind(this, view);
        File photoFile = (File) getArguments().getSerializable(ARG_PHOTO);
        if (photoFile == null || !photoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {

//            try {
//
//                //ExifInterface exif = new ExifInterface(mPhtoFile.getName());
//                //int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
//
//            } catch (IOException e){
//
//            }
            Bitmap bitmap = PictureUtils.getScaledBitmap(photoFile.getPath(), getActivity());
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            mPhotoView.setImageBitmap(rotatedBitmap);
        }
        return new AlertDialog.Builder(getContext()).setView(view).create();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
