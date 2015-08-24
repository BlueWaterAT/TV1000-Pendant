package com.bwat.pendant;

import android.widget.SeekBar;

/**
 * @author Kareem ElFaramawi
 */
public class SpringSeekBar implements SeekBar.OnSeekBarChangeListener {
	private SeekBar seek;

	private int min, max, spring;

	public static final double THUMB_SCALE = 4.0;

	public SpringSeekBar(SeekBar seekBar, int min, int max, int spring) {
		this.seek = seekBar;
		this.min = min;
		this.max = max;
		this.spring = spring;

		this.seek.setMax(max - min);
		this.seek.setOnSeekBarChangeListener(this);
		seek.setThumb(null);
		setProgress(spring);

		//Scale seekbar thumb size
//		ViewTreeObserver vto = seek.getViewTreeObserver();
//		vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//			public boolean onPreDraw() {
//				Drawable thumb = seek.getThumb();
//				int w = (int) (thumb.getIntrinsicWidth() * THUMB_SCALE), h = (int) (thumb.getIntrinsicHeight() * THUMB_SCALE);
//				seek.setMinimumWidth(w);
//				seek.setMinimumHeight(h);
//				thumb.setBounds(-w/2, -h/2, w ,h);
//				seek.setThumb(thumb);
////				int h = (int) (seek.getMeasuredHeight() * 1.5); // 8 * 1.5 = 12
////				int w = h;
////				Bitmap bmpOrg = ((BitmapDrawable) thumb).getBitmap();
////				Bitmap bmpScaled = Bitmap.createScaledBitmap(bmpOrg, w, h, true);
////				Drawable newThumb = new BitmapDrawable(seek.getResources(), bmpScaled);
////				newThumb.setBounds(0, 0, newThumb.getIntrinsicWidth(), newThumb.getIntrinsicHeight());
////				seek.setThumb(newThumb);
//
//				seek.getViewTreeObserver().removeOnPreDrawListener(this);
//
//				return true;
//			}
//		});
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		setProgress(spring);
	}

	public int getProgress() {
		return seek.getProgress() + min;
	}

	public void setProgress(int progress) {
		progress = MathUtils.clamp_i(progress, min, max);
		seek.setProgress(progress - min);
	}
}
