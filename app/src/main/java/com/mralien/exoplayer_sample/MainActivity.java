package com.mralien.exoplayer_sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by tk.hoa on 02/01/2019.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ListView lv = findViewById(R.id.list_view);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, Constants.MEDIA_TYPE);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String url = "";
                int type = position;
                switch (position) {
                    case Constants.TYPE_MP3:
                        url = getString(R.string.media_url_mp3);
                        break;
                    case Constants.TYPE_MP4:
                        url = getString(R.string.media_url_mp4);
                        break;
                    case Constants.TYPE_PLAYLIST:
                        break;
                    case Constants.TYPE_DASH:
                        url = getString(R.string.media_url_dash);
                        break;
                    case Constants.TYPE_HLS:
                        url = getString(R.string.media_url_hls);
                        break;
                    case Constants.TYPE_SS:
//                        url = getString(R.string.media_url_ss);
                        break;
                    default:
                        break;
                }
                Bundle bundle = new Bundle();
                bundle.putString(Constants.BUNDLE_KEY_URL, url);
                bundle.putInt(Constants.BUNDLE_KEY_TYPE, type);
                Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

    }
}

