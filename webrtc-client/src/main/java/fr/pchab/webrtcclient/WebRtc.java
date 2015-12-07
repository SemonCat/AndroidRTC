package fr.pchab.webrtcclient;

import android.content.Context;
import android.graphics.Point;
import android.view.WindowManager;

/**
 * Created by semoncat on 2015/12/7.
 */
public class WebRtc {
    private static WebRtcClient mInstance;

    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";

    public static void init(Context mContext, String host) {
        WindowManager windowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        Point displaySize = new Point();
        windowManager.getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        mInstance = new WebRtcClient(mContext, host, params);
    }

    public static WebRtcClient getInstance() {
        if (mInstance == null) {
            throw new IllegalArgumentException("Please init first");
        }

        return mInstance;
    }


}
