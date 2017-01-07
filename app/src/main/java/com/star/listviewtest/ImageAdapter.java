package com.star.listviewtest;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageAdapter extends ArrayAdapter<String> {

    private LruCache<String, BitmapDrawable> mMemoryCache;

    public ImageAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable bitmapDrawable) {
                return bitmapDrawable.getBitmap().getByteCount();
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        String url = getItem(position);

        View view;

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.image_item, null);
        } else {
            view = convertView;
        }

        ImageView imageView = (ImageView) view.findViewById(R.id.image_view);

        BitmapDrawable bitmapDrawable = getBitmapFromMemoryCache(url);

        if (bitmapDrawable != null) {
            imageView.setImageDrawable(bitmapDrawable);
        } else {
            BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(imageView);
            bitmapWorkerTask.execute(url);
        }

        return view;
    }

    private BitmapDrawable getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    private void addBitmapToMemoryCache(String key, BitmapDrawable bitmapDrawable) {
        mMemoryCache.put(key, bitmapDrawable);
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {

        private ImageView mImageView;

        public BitmapWorkerTask(ImageView imageView) {
            mImageView = imageView;
        }

        @Override
        protected BitmapDrawable doInBackground(String... params) {

            String url = params[0];

            Bitmap bitmap = downloadBitmap(url);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getContext().getResources(), bitmap);

            addBitmapToMemoryCache(url, bitmapDrawable);

            return bitmapDrawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            if (mImageView != null && drawable != null) {
                mImageView.setImageDrawable(drawable);
            }
        }

        private Bitmap downloadBitmap(String imageUrl) {
            Bitmap bitmap = null;

            HttpURLConnection httpURLConnection = null;

            try {
                URL url = new URL(imageUrl);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(5 * 1000);
                httpURLConnection.setReadTimeout(10 * 1000);
                bitmap = BitmapFactory.decodeStream(httpURLConnection.getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }

            return bitmap;
        }
    }

}
