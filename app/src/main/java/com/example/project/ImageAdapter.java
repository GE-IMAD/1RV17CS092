package com.example.project;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.MyViewHolder> implements Filterable
{
    private Context context;
    private List<ImageData> imageList;
    private List<ImageData> imageListFiltered;
    private ImageAdapterListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder
    {
        public TextView imagename, taglist, mode;
        public ImageView thumbnail;

        public MyViewHolder(View view) {
            super(view);
            imagename = view.findViewById(R.id.title);
            taglist = view.findViewById(R.id.tags);
            thumbnail = view.findViewById(R.id.list_image);
            mode = view.findViewById(R.id.mode);

            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    listener.onImageDataSelected(imageListFiltered.get(getAdapterPosition()));
                }
            });
        }
    }


    public ImageAdapter(Context context, List<ImageData> imageList, ImageAdapterListener listener)
    {
        this.context = context;
        this.listener = listener;
        this.imageList = imageList;
        this.imageListFiltered = imageList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listview, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position)
    {
        final ImageData img = imageListFiltered.get(position);
        holder.imagename.setText(img.getImagename());
        holder.taglist.setText(img.getTaglist());
        if(img.getMode()==Constants.LOCAL_MODE)
            holder.mode.setText(R.string.mode_local);
        else
            holder.mode.setText(R.string.mode_cloud);

        if(img.getMode()==Constants.LOCAL_MODE)
        {
            Glide.with(context)
                    .load(img.getImagepath())
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.thumbnail);
        }
        else
        {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl(img.getImagepath());

            GlideApp.with(context)
                    .load(storageRef)
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.thumbnail);
        }
    }

    @Override
    public int getItemCount()
    {
        return imageListFiltered.size();
    }

    @Override
    public Filter getFilter()
    {
        return new Filter()
        {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence)
            {
                String charString = charSequence.toString();
                if (charString.isEmpty())
                {
                    imageListFiltered = imageList;
                }
                else
                    {
                    List<ImageData> filteredList = new ArrayList<>();
                    for (ImageData row : imageList) {

                        if (row.getTaglist().toLowerCase().contains(charSequence.toString().toLowerCase()))
                        {
                            filteredList.add(row);
                        }
                    }

                    imageListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = imageListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults)
            {
                imageListFiltered = (ArrayList<ImageData>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public interface ImageAdapterListener
    {
        void onImageDataSelected(ImageData imageData);
    }
}
