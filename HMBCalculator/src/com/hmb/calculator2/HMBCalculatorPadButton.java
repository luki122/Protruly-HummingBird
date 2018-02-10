package com.hmb.calculator2;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by xiaobin on 17-6-16.
 */

public class HMBCalculatorPadButton extends FrameLayout {

    private static final String TAG = "HMBCalculatorPadButton";

    private final int FLAG_LINE_LEFT = 1;
    private final int FLAG_LINE_TOP = 2;
    private final int FLAG_LINE_RIGHT = 4;
    private final int FLAG_LINE_BOTTOM = 8;
    private final int FLAG_LINE_ALL = 16;

    private ImageView imageView;
//    private TextView textView;
    private View line_left;
    private View line_top;
    private View line_right;
    private View line_bottom;

    private String text;
    private int imageSrcId;
    private int flagDrawLine;
    private boolean showLeft = false;
    private boolean isHighlight = false;

    public HMBCalculatorPadButton(Context context) {
        super(context);
    }

    public HMBCalculatorPadButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HMBCalculatorPadButton);
        text = a.getString(R.styleable.HMBCalculatorPadButton_text);
        imageSrcId = a.getResourceId(R.styleable.HMBCalculatorPadButton_imageSrc, 0);
        flagDrawLine = a.getInteger(R.styleable.HMBCalculatorPadButton_drawLine, 0);
        showLeft = a.getBoolean(R.styleable.HMBCalculatorPadButton_showLeft, false);
        isHighlight = a.getBoolean(R.styleable.HMBCalculatorPadButton_isHighlight, false);

        a.recycle();

        View view = LayoutInflater.from(context).inflate(R.layout.hmb_view_calculator_pad_button, this);
        if (isHighlight) {
            view.findViewById(R.id.frame).setBackgroundResource(R.drawable.calculator_button_background_orange);
        }
//        textView = (TextView) view.findViewById(R.id.text);
        if (showLeft) {
            imageView = (ImageView) view.findViewById(R.id.image_left);
//            textView.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        } else {
            imageView = (ImageView) view.findViewById(R.id.image);
        }
        line_left = view.findViewById(R.id.line_left);
        line_top = view.findViewById(R.id.line_top);
        line_right = view.findViewById(R.id.line_right);
        line_bottom = view.findViewById(R.id.line_bottom);

        setInitValue();
    }

    private void setInitValue() {
        if (!TextUtils.isEmpty(text)) {
//            textView.setVisibility(View.VISIBLE);
//            textView.setText(text);
        } else if (imageSrcId != 0) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(imageSrcId);
        }

        if ((flagDrawLine & FLAG_LINE_ALL) == FLAG_LINE_ALL) {
            line_left.setVisibility(View.VISIBLE);
            line_top.setVisibility(View.VISIBLE);
            line_right.setVisibility(View.VISIBLE);
            line_bottom.setVisibility(View.VISIBLE);
        } else {
            if ((flagDrawLine & FLAG_LINE_LEFT) == FLAG_LINE_LEFT) {
                line_left.setVisibility(View.VISIBLE);
            }
            if ((flagDrawLine & FLAG_LINE_TOP) == FLAG_LINE_TOP) {
                line_top.setVisibility(View.VISIBLE);
            }
            if ((flagDrawLine & FLAG_LINE_RIGHT) == FLAG_LINE_RIGHT) {
                line_right.setVisibility(View.VISIBLE);
            }
            if ((flagDrawLine & FLAG_LINE_BOTTOM) == FLAG_LINE_BOTTOM) {
                line_bottom.setVisibility(View.VISIBLE);
            }
        }

    }

}
