package com.zr.addressselector;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.zr.addressselector.listener.OnAddressSelectedListener;
import com.zr.addressselector.util.ResUtils;


/**
 * @author HZJ
 */
public class JdSelectorDialog extends Dialog {

    public JdAddressSelector getSelector() {
        return mSelector;
    }

    private JdAddressSelector mSelector;
    /***JdSelectDialog关闭的时候是否清空缓存，默认为false***/
    private boolean mClearCacheWhenDismiss = false;

    public JdSelectorDialog(Context context) {
        this(context, R.style.bottom_dialog);
    }

    public JdSelectorDialog(Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    public JdSelectorDialog(Context context, boolean clearCacheWhenDismiss) {
        this(context, R.style.bottom_dialog);
        this.mClearCacheWhenDismiss = clearCacheWhenDismiss;
    }

    private void init(Context context) {
        mSelector = new JdAddressSelector(context);

        setContentView(mSelector.getmSelectorView());

        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = ResUtils.dp2px(context, 256);
        window.setAttributes(params);

        window.setGravity(Gravity.BOTTOM);

        mSelector.setOnCloseClickListener(new JdAddressSelector.OnCloseClickListener() {
            @Override
            public void onCloseClick() {
                dismiss();
            }
        });
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener listener) {
        this.mSelector.setOnAddressSelectedListener(listener);
    }

    public static JdSelectorDialog show(Context context) {
        return show(context, null);
    }

    public static JdSelectorDialog show(Context context, OnAddressSelectedListener listener) {
        JdSelectorDialog dialog = new JdSelectorDialog(context, R.style.bottom_dialog);
        dialog.mSelector.setOnAddressSelectedListener(listener);
        dialog.show();

        return dialog;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mClearCacheWhenDismiss) {
            this.mSelector.clearCacheData();
        }
    }
}
