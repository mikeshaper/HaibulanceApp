package com.example.haibulance;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class EntranceActivity extends AppCompatActivity {

    Handler handler;
    ImageView haibuIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);
        handler = new Handler();
        haibuIcon = findViewById(R.id.haibuIcon);
        animateImage(2000, haibuIcon);
        startRegiActivity(2000);
    }

    void startRegiActivity(long duration){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(EntranceActivity.this, RegisterActivity.class));
                finish();
            }
        }, duration);
    }

    void animateImage(long duration, ImageView img){
        rotateAnimation(duration, img);
        //scaleAnimation(duration, img);
    }

    void rotateAnimation(final long duration, final ImageView img){
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(duration);
        rotate.setInterpolator(new LinearInterpolator());
        img.startAnimation(rotate);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                rotateAnimation(duration, img);
            }
        }, duration);
    }

    void scaleAnimation(long duration, ImageView img){
        scaleDown(duration, img);
    }

    void scaleUp(final long duration, final ImageView img){
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(img, "scaleX", 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(img, "scaleY", 1f);
        scaleUpX.setDuration(duration);
        scaleUpY.setDuration(duration);
        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.play(scaleUpX).with(scaleUpY);
        scaleUp.start();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scaleDown(duration, img);
            }
        }, duration);
    }

    void scaleDown(final long duration, final ImageView img){
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(img, "scaleX", 0.75f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(img, "scaleY", 0.75f);
        scaleDownX.setDuration(duration);
        scaleDownY.setDuration(duration);
        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY);
        scaleDown.start();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scaleUp(duration, img);
            }
        }, duration);
    }
}
