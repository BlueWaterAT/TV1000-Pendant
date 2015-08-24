package com.bwat.pendant;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * @author Kareem ElFaramawi
 */
public class NumberSpinner extends FrameLayout {
	public interface ChangeListener {
		void valueChanged();
	}

	private EditText spinnerVal;
	private Button incr;
	private Button decr;

	private int min = Integer.MIN_VALUE;
	private int max = Integer.MAX_VALUE;
	private int step = 1;

	private ChangeListener listener = null;

	public NumberSpinner(Context context) {
		super(context);
		initView(context);
	}

	public NumberSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttrs(context, attrs);
		initView(context);
	}

	public NumberSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		parseAttrs(context, attrs);
		initView(context);
	}

	private void parseAttrs(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberSpinner);
		for (int i = 0; i < a.getIndexCount(); i++) {
			int attr = a.getIndex(i);
			switch (attr) {
				case R.styleable.NumberSpinner_spinnerMin:
					min = a.getInt(attr, Integer.MIN_VALUE);
					break;
				case R.styleable.NumberSpinner_spinnerMax:
					max = a.getInt(attr, Integer.MAX_VALUE);
					break;
				case R.styleable.NumberSpinner_spinnerStep:
					step = a.getInt(attr, 1);
					break;
			}
		}
		a.recycle();
	}

	private void initView(Context context) {
		View.inflate(context, R.layout.number_spinner, this);


		spinnerVal = (EditText) findViewById(R.id.spinnerVal);
		incr = (Button) findViewById(R.id.incr);
		decr = (Button) findViewById(R.id.decr);

		incr.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setValue(getValue() + step);
			}
		});

		decr.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setValue(getValue() - step);
			}
		});

		setValue(Math.max(0, min));

		spinnerVal.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (listener != null && spinnerVal.getText().toString().length() > 0) {
					listener.valueChanged();
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
	}

	public void setValue(int val) {
		spinnerVal.setText("" + MathUtils.clamp_i(val, min, max));
	}

	public int getValue() {
		return Integer.parseInt(spinnerVal.getText().toString());
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public void setOnChangeListener(ChangeListener listener) {
		this.listener = listener;
	}
}
