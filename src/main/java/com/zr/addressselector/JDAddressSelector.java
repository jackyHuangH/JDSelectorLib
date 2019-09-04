package com.zr.addressselector;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.zr.addressselector.listener.OnAddressSelectedListener;
import com.zr.addressselector.model.JdCity;
import com.zr.addressselector.model.JdCounty;
import com.zr.addressselector.model.JdProvince;
import com.zr.addressselector.model.JdStreet;
import com.zr.addressselector.model.JdVillage;
import com.zr.addressselector.util.ListUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * 省市县镇村5级仿京东地址选择器
 *
 * @author HZJ
 */
public class JdAddressSelector implements AdapterView.OnItemClickListener {
    private static final String TEXT_UNCHECKED = "请选择";
    private static final int INDEX_TAB_PROVINCE = 0;
    private static final int INDEX_TAB_CITY = 1;
    private static final int INDEX_TAB_COUNTY = 2;
    private static final int INDEX_TAB_STREET = 3;
    private static final int INDEX_TAB_VILLAGE = 4;

    private static final int INDEX_INVALID = -1;

    private static final int WHAT_PROVINCES_PROVIDED = 0;
    private static final int WHAT_CITIES_PROVIDED = 1;
    private static final int WHAT_COUNTIES_PROVIDED = 2;
    private static final int WHAT_STREETS_PROVIDED = 3;
    private static final int WHAT_VILLAGES_PROVIDED = 4;

    private final Context mContext;
    private OnAddressSelectedListener mOnSelectedListener;

    private View mSelectorView;
    /***当前选择指针***/
    private View mIndicator;
    /***五个级别TabTextView***/
    private TextView mTvTabProvince;
    private TextView mTvTabCity;
    private TextView mTvTabCounty;
    private TextView mTvTabStreet;
    private TextView mTvTabVillage;

    private ProgressBar mLoadingPb;
    private TextView mTvConfirmClose;

    private ListView mListView;
    private ProvinceAdapter mProvinceAdapter;
    private CityAdapter mCityAdapter;
    private CountyAdapter mCountyAdapter;
    private StreetAdapter mStreetAdapter;
    private VillageAdapter mVillageAdapter;
    /***各级列表展示数据***/
    private List<JdProvince> mProvinceData;
    private List<JdCity> mCityData;
    private List<JdCounty> mCountyData;
    private List<JdStreet> mStreetData;
    private List<JdVillage> mVillageData;

    /**
     * 缓存数据:省-市
     */
    private ArrayMap<String, List<JdCity>> mProvince2city = new ArrayMap<>();
    /**
     * 缓存数据:市-区
     */
    private ArrayMap<String, List<JdCounty>> mCity2county = new ArrayMap<>();
    /**
     * 缓存数据:区-街道
     */
    private ArrayMap<String, List<JdStreet>> mCounty2street = new ArrayMap<>();
    /**
     * 缓存数据:街道-村
     */
    private ArrayMap<String, List<JdVillage>> mStreet2Village = new ArrayMap<>();

    /***标记已选中的列表位置***/
    private int mProvinceSelectIndex = INDEX_INVALID;
    private int mCitySelectIndex = INDEX_INVALID;
    private int mCountySelectIndex = INDEX_INVALID;
    private int mStreetSelectIndex = INDEX_INVALID;
    private int mVillageSelectIndex = INDEX_INVALID;

    /***当前tab所在位置，默认在省0***/
    private int mTabIndex = INDEX_TAB_PROVINCE;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_PROVINCES_PROVIDED:
                    //更新--省--数据
                    mProvinceData = (List<JdProvince>) msg.obj;
                    mProvinceAdapter.notifyDataSetChanged();
                    mListView.setAdapter(mProvinceAdapter);
                    break;
                case WHAT_CITIES_PROVIDED:
                    //更新--市--数据
                    mCityData = (List<JdCity>) msg.obj;
                    mCityAdapter.notifyDataSetChanged();
                    if (ListUtils.notEmpty(mCityData)) {
                        // 以次级内容更新列表
                        mListView.setAdapter(mCityAdapter);
                        // 更新索引为次级
                        mTabIndex = INDEX_TAB_CITY;

                        // 缓存省-市数据
                        String provinceId = mCityData.get(0).parentId;
                        if (!mProvince2city.containsKey(provinceId)) {
                            List<JdCity> cityList = new ArrayList<>();
                            ListUtils.copy(mCityData, cityList);
                            mProvince2city.put(provinceId, cityList);
                        }

                    } else {
                        // 次级无内容，回调
                        callbackInternal();
                    }
                    break;
                case WHAT_COUNTIES_PROVIDED:
                    //更新--区--数据
                    mCountyData = (List<JdCounty>) msg.obj;
                    mCountyAdapter.notifyDataSetChanged();
                    if (ListUtils.notEmpty(mCountyData)) {
                        mListView.setAdapter(mCountyAdapter);
                        mTabIndex = INDEX_TAB_COUNTY;
                        // 缓存市-区数据
                        String cityId = mCountyData.get(0).parentId;
                        if (!mCity2county.containsKey(cityId)) {
                            List<JdCounty> countyList = new ArrayList<>();
                            ListUtils.copy(mCountyData, countyList);
                            mCity2county.put(cityId, countyList);
                        }
                    } else {
                        callbackInternal();
                    }
                    break;
                case WHAT_STREETS_PROVIDED:
                    //更新--街道--数据
                    mStreetData = (List<JdStreet>) msg.obj;
                    mStreetAdapter.notifyDataSetChanged();
                    if (ListUtils.notEmpty(mStreetData)) {
                        mListView.setAdapter(mStreetAdapter);
                        mTabIndex = INDEX_TAB_STREET;
                        // 缓存区-街道数据
                        String countryId = mStreetData.get(0).parentId;
                        if (!mCounty2street.containsKey(countryId)) {
                            List<JdStreet> streetList = new ArrayList<>();
                            ListUtils.copy(mStreetData, streetList);
                            mCounty2street.put(countryId, streetList);
                        }
                    } else {
                        callbackInternal();
                    }
                    break;
                case WHAT_VILLAGES_PROVIDED:
                    //更新--村--数据
                    mVillageData = (List<JdVillage>) msg.obj;
                    mVillageAdapter.notifyDataSetChanged();
                    if (ListUtils.notEmpty(mVillageData)) {
                        // 以次级内容更新列表
                        mListView.setAdapter(mVillageAdapter);
                        // 更新索引为次级
                        mTabIndex = INDEX_TAB_VILLAGE;
                        // 缓存街道-村数据
                        String streetId = mVillageData.get(0).parentId;
                        if (!mStreet2Village.containsKey(streetId)) {
                            List<JdVillage> villageList = new ArrayList<>();
                            ListUtils.copy(mVillageData, villageList);
                            mStreet2Village.put(streetId, villageList);
                        }
                    } else {
                        // 次级无内容，回调
                        callbackInternal();
                    }
                    break;
                default:
                    break;
            }
            updateTabsVisibility();
            updateProgressVisibility();
            updateIndicator();
        }
    };

    public JdAddressSelector(Context context) {
        this.mContext = context;
        initViews();
        initAdapters();
    }

    private void initAdapters() {
        mProvinceAdapter = new ProvinceAdapter();
        mCityAdapter = new CityAdapter();
        mCountyAdapter = new CountyAdapter();
        mStreetAdapter = new StreetAdapter();
        mVillageAdapter = new VillageAdapter();
    }

    private void initViews() {
        mSelectorView = LayoutInflater.from(mContext).inflate(R.layout.address_selector, null);

        this.mLoadingPb = (ProgressBar) mSelectorView.findViewById(R.id.jd_progressBar);

        this.mListView = (ListView) mSelectorView.findViewById(R.id.jd_listView);
        this.mIndicator = mSelectorView.findViewById(R.id.jd_indicator);

        this.mTvTabProvince = (TextView) mSelectorView.findViewById(R.id.textViewProvince);
        this.mTvTabCity = (TextView) mSelectorView.findViewById(R.id.textViewCity);
        this.mTvTabCounty = (TextView) mSelectorView.findViewById(R.id.textViewCounty);
        this.mTvTabStreet = (TextView) mSelectorView.findViewById(R.id.textViewStreet);
        this.mTvTabVillage = (TextView) mSelectorView.findViewById(R.id.textViewVillage);

        this.mTvConfirmClose = (TextView) mSelectorView.findViewById(R.id.jd_close);

        this.mTvTabProvince.setOnClickListener(new OnProvinceTabClickListener());
        this.mTvTabCity.setOnClickListener(new OnCityTabClickListener());
        this.mTvTabCounty.setOnClickListener(new OnCountyTabClickListener());
        this.mTvTabStreet.setOnClickListener(new OnStreetTabClickListener());
        this.mTvTabVillage.setOnClickListener(new OnVillageTabClickListener());

        this.mListView.setOnItemClickListener(this);

        this.mTvConfirmClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //至少省级选择了一个才触发回调
                if (mProvinceSelectIndex != INDEX_INVALID) {
                    callbackInternal();
                }
                if (null != onCloseClickListener) {
                    onCloseClickListener.onCloseClick();
                }
            }
        });
        updateIndicator();
        mLoadingPb.setVisibility(View.VISIBLE);
    }

    public View getmSelectorView() {
        return mSelectorView;
    }

    private void updateIndicator() {
        mSelectorView.post(new Runnable() {
            @Override
            public void run() {
                switch (mTabIndex) {
                    case INDEX_TAB_PROVINCE:
                        buildIndicatorAnimatorTowards(mTvTabProvince).start();
                        break;
                    case INDEX_TAB_CITY:
                        buildIndicatorAnimatorTowards(mTvTabCity).start();
                        break;
                    case INDEX_TAB_COUNTY:
                        buildIndicatorAnimatorTowards(mTvTabCounty).start();
                        break;
                    case INDEX_TAB_STREET:
                        buildIndicatorAnimatorTowards(mTvTabStreet).start();
                        break;
                    case INDEX_TAB_VILLAGE:
                        buildIndicatorAnimatorTowards(mTvTabVillage).start();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private AnimatorSet buildIndicatorAnimatorTowards(TextView tab) {
        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(mIndicator, "X", mIndicator.getX(), tab.getX());

        final ViewGroup.LayoutParams params = mIndicator.getLayoutParams();
        ValueAnimator widthAnimator = ValueAnimator.ofInt(params.width, tab.getMeasuredWidth());
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.width = (int) animation.getAnimatedValue();
                mIndicator.setLayoutParams(params);
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(new FastOutSlowInInterpolator());
        set.playTogether(xAnimator, widthAnimator);

        return set;
    }

    private class OnProvinceTabClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            mTabIndex = INDEX_TAB_PROVINCE;
            mListView.setAdapter(mProvinceAdapter);

            if (mProvinceSelectIndex != INDEX_INVALID) {
                mListView.setSelection(mProvinceSelectIndex);
            }

            updateTabsVisibility();
            updateIndicator();
        }
    }

    private class OnCityTabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mTabIndex = INDEX_TAB_CITY;
            mListView.setAdapter(mCityAdapter);

            if (mCitySelectIndex != INDEX_INVALID) {
                mListView.setSelection(mCitySelectIndex);
            }

            updateTabsVisibility();
            updateIndicator();
        }
    }

    private class OnCountyTabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mTabIndex = INDEX_TAB_COUNTY;
            mListView.setAdapter(mCountyAdapter);

            if (mCountySelectIndex != INDEX_INVALID) {
                mListView.setSelection(mCountySelectIndex);
            }

            updateTabsVisibility();
            updateIndicator();
        }
    }

    private class OnStreetTabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mTabIndex = INDEX_TAB_STREET;
            mListView.setAdapter(mStreetAdapter);

            if (mStreetSelectIndex != INDEX_INVALID) {
                mListView.setSelection(mStreetSelectIndex);
            }

            updateTabsVisibility();
            updateIndicator();
        }
    }

    private class OnVillageTabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mTabIndex = INDEX_TAB_VILLAGE;
            mListView.setAdapter(mVillageAdapter);

            if (mVillageSelectIndex != INDEX_INVALID) {
                mListView.setSelection(mVillageSelectIndex);
            }

            updateTabsVisibility();
            updateIndicator();
        }
    }

    private void updateTabsVisibility() {
        mTvTabProvince.setVisibility(ListUtils.notEmpty(mProvinceData) ? View.VISIBLE : View.GONE);
        mTvTabCity.setVisibility(ListUtils.notEmpty(mCityData) ? View.VISIBLE : View.GONE);
        mTvTabCounty.setVisibility(ListUtils.notEmpty(mCountyData) ? View.VISIBLE : View.GONE);
        mTvTabStreet.setVisibility(ListUtils.notEmpty(mStreetData) ? View.VISIBLE : View.GONE);
        mTvTabVillage.setVisibility(ListUtils.notEmpty(mVillageData) ? View.VISIBLE : View.GONE);

        mTvTabProvince.setEnabled(mTabIndex != INDEX_TAB_PROVINCE);
        mTvTabCity.setEnabled(mTabIndex != INDEX_TAB_CITY);
        mTvTabCounty.setEnabled(mTabIndex != INDEX_TAB_COUNTY);
        mTvTabStreet.setEnabled(mTabIndex != INDEX_TAB_STREET);
        mTvTabVillage.setEnabled(mTabIndex != INDEX_TAB_VILLAGE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (mTabIndex) {
            case INDEX_TAB_PROVINCE:
                JdProvince province = mProvinceAdapter.getItem(position);

                // 更新当前级别及子级标签文本
                mTvTabProvince.setText(province.name);
                mTvTabCity.setText(TEXT_UNCHECKED);
                mTvTabCounty.setText(TEXT_UNCHECKED);
                mTvTabStreet.setText(TEXT_UNCHECKED);
                mTvTabVillage.setText(TEXT_UNCHECKED);

                // 清空子级数据
                mCityData = null;
                mCountyData = null;
                mStreetData = null;
                mVillageData = null;
                mCityAdapter.notifyDataSetChanged();
                mCountyAdapter.notifyDataSetChanged();
                mStreetAdapter.notifyDataSetChanged();
                mVillageAdapter.notifyDataSetChanged();

                // 更新已选中项
                this.mProvinceSelectIndex = position;
                this.mCitySelectIndex = INDEX_INVALID;
                this.mCountySelectIndex = INDEX_INVALID;
                this.mStreetSelectIndex = INDEX_INVALID;
                this.mVillageSelectIndex = INDEX_INVALID;

                // 更新选中效果
                mProvinceAdapter.notifyDataSetChanged();

                // 有缓存则直接使用缓存,否则去重新请求
                if (mProvince2city.containsKey(province.id)) {
                    setCities(mProvince2city.get(province.id));
                } else {
                    mLoadingPb.setVisibility(View.VISIBLE);
                    mOnSelectedListener.onProvinceSelected(province);
                }

                break;

            case INDEX_TAB_CITY:
                JdCity city = mCityAdapter.getItem(position);

                mTvTabCity.setText(city.name);
                mTvTabCounty.setText(TEXT_UNCHECKED);
                mTvTabStreet.setText(TEXT_UNCHECKED);
                mTvTabVillage.setText(TEXT_UNCHECKED);

                mCountyData = null;
                mStreetData = null;
                mVillageData = null;
                mCountyAdapter.notifyDataSetChanged();
                mStreetAdapter.notifyDataSetChanged();
                mVillageAdapter.notifyDataSetChanged();

                this.mCitySelectIndex = position;
                this.mCountySelectIndex = INDEX_INVALID;
                this.mStreetSelectIndex = INDEX_INVALID;
                this.mVillageSelectIndex = INDEX_INVALID;

                mCityAdapter.notifyDataSetChanged();
                System.out.println(mCity2county.toString());

                // 有缓存则直接使用缓存,否则去重新请求
                if (mCity2county.containsKey(city.id)) {
                    System.out.println("parentId = " + city.id);
                    setCounties(mCity2county.get(city.id));
                } else {
                    mLoadingPb.setVisibility(View.VISIBLE);
                    mOnSelectedListener.onCitySelected(city);
                }

                break;

            case INDEX_TAB_COUNTY:
                JdCounty county = mCountyAdapter.getItem(position);

                mTvTabCounty.setText(county.name);
                mTvTabStreet.setText(TEXT_UNCHECKED);
                mTvTabVillage.setText(TEXT_UNCHECKED);

                mStreetData = null;
                mVillageData = null;
                mStreetAdapter.notifyDataSetChanged();
                mVillageAdapter.notifyDataSetChanged();

                this.mCountySelectIndex = position;
                this.mStreetSelectIndex = INDEX_INVALID;
                this.mVillageSelectIndex = INDEX_INVALID;

                mCountyAdapter.notifyDataSetChanged();

                // 有缓存则直接使用缓存,否则去重新请求
                if (mCounty2street.containsKey(county.id)) {
                    setStreets(mCounty2street.get(county.id));
                } else {
                    mLoadingPb.setVisibility(View.VISIBLE);
                    mOnSelectedListener.onCountySelected(county);
                }

                break;

            case INDEX_TAB_STREET:
                JdStreet street = mStreetAdapter.getItem(position);

                mTvTabStreet.setText(street.name);
                mTvTabVillage.setText(TEXT_UNCHECKED);

                mVillageData = null;
                mVillageAdapter.notifyDataSetChanged();

                this.mStreetSelectIndex = position;
                this.mVillageSelectIndex = INDEX_INVALID;

                mStreetAdapter.notifyDataSetChanged();
                // 有缓存则直接使用缓存,否则去重新请求
                if (mStreet2Village.containsKey(street.id)) {
                    setVillages(mStreet2Village.get(street.id));
                } else {
                    mLoadingPb.setVisibility(View.VISIBLE);
                    mOnSelectedListener.onStreetSelected(street);
                }
                break;
            case INDEX_TAB_VILLAGE:
                JdVillage village = mVillageAdapter.getItem(position);
                mTvTabVillage.setText(village.name);
                this.mVillageSelectIndex = position;
                mVillageAdapter.notifyDataSetChanged();
                callbackInternal();
                break;
            default:
                break;
        }
        updateTabsVisibility();
        updateIndicator();
    }

    /**
     * 地址选择完成时调用的方法
     */
    private void callbackInternal() {
        if (mOnSelectedListener != null) {
            JdProvince province = mProvinceData == null || mProvinceSelectIndex == INDEX_INVALID ? null : mProvinceData.get(mProvinceSelectIndex);
            JdCity city = mCityData == null || mCitySelectIndex == INDEX_INVALID ? null : mCityData.get(mCitySelectIndex);
            JdCounty county = mCountyData == null || mCountySelectIndex == INDEX_INVALID ? null : mCountyData.get(mCountySelectIndex);
            JdStreet street = mStreetData == null || mStreetSelectIndex == INDEX_INVALID ? null : mStreetData.get(mStreetSelectIndex);
            JdVillage village = mVillageData == null || mVillageSelectIndex == INDEX_INVALID ? null : mVillageData.get(mVillageSelectIndex);

            mOnSelectedListener.onAddressSelected(province, city, county, street, village);
        }
    }

    private void updateProgressVisibility() {
        ListAdapter adapter = mListView.getAdapter();
        int itemCount = adapter.getCount();
        mLoadingPb.setVisibility(itemCount > 0 ? View.GONE : View.VISIBLE);
    }

    private class ProvinceAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mProvinceData == null ? 0 : mProvinceData.size();
        }

        @Override
        public JdProvince getItem(int position) {
            return mProvinceData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            JdProvince item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = mProvinceSelectIndex != INDEX_INVALID && mProvinceData.get(mProvinceSelectIndex).id.equals(item.id);
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    private class CityAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mCityData == null ? 0 : mCityData.size();
        }

        @Override
        public JdCity getItem(int position) {
            return mCityData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            JdCity item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = mCitySelectIndex != INDEX_INVALID && mCityData.get(mCitySelectIndex).id.equals(item.id);
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    private class CountyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mCountyData == null ? 0 : mCountyData.size();
        }

        @Override
        public JdCounty getItem(int position) {
            return mCountyData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            JdCounty item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = mCountySelectIndex != INDEX_INVALID && mCountyData.get(mCountySelectIndex).id.equals(item.id);
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    private class StreetAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mStreetData == null ? 0 : mStreetData.size();
        }

        @Override
        public JdStreet getItem(int position) {
            return mStreetData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            JdStreet item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = mStreetSelectIndex != INDEX_INVALID && mStreetData.get(mStreetSelectIndex).id.equals(item.id);
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    private class VillageAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mVillageData == null ? 0 : mVillageData.size();
        }

        @Override
        public JdVillage getItem(int position) {
            return mVillageData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            JdVillage item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = mVillageSelectIndex != INDEX_INVALID && mVillageData.get(mVillageSelectIndex).id.equals(item.id);
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    private void setProvinces(List<JdProvince> provinces, int position) {
        if (ListUtils.isEmpty(provinces)) {
            return;
        }
        this.mProvinceData = provinces;
        mTabIndex = INDEX_TAB_PROVINCE;
        this.mProvinceSelectIndex = position;
        JdProvince province = this.mProvinceData.get(position);
        mTvTabProvince.setText(province.name);

        mListView.setAdapter(mProvinceAdapter);
        if (mProvinceSelectIndex != INDEX_INVALID) {
            mListView.setSelection(mProvinceSelectIndex);
        }
    }

    private void setCities(List<JdCity> cities, int position) {
        if (ListUtils.isEmpty(cities)) {
            return;
        }
        this.mCityData = cities;
        mTabIndex = INDEX_TAB_CITY;
        this.mCitySelectIndex = position;
        JdCity city = this.mCityData.get(position);
        mTvTabCity.setText(city.name);
        // 缓存省-市数据
        String provinceId = cities.get(0).parentId;
        if (!mProvince2city.containsKey(provinceId)) {
            List<JdCity> cityList = new ArrayList<>();
            ListUtils.copy(cities, cityList);
            mProvince2city.put(provinceId, cityList);
        }
        mListView.setAdapter(mCityAdapter);
        if (mCitySelectIndex != INDEX_INVALID) {
            mListView.setSelection(mCitySelectIndex);
        }
    }

    private void setCounties(List<JdCounty> counties, int position) {
        if (ListUtils.isEmpty(counties)) {
            return;
        }
        this.mCountyData = counties;
        mTabIndex = INDEX_TAB_COUNTY;
        this.mCountySelectIndex = position;
        JdCounty county = this.mCountyData.get(position);
        mTvTabCounty.setText(county.name);
        // 缓存市-区数据
        String cityId = counties.get(0).parentId;
        if (!mCity2county.containsKey(cityId)) {
            List<JdCounty> countyList = new ArrayList<>();
            ListUtils.copy(counties, countyList);
            mCity2county.put(cityId, countyList);
        }
        mListView.setAdapter(mCountyAdapter);
        if (mCountySelectIndex != INDEX_INVALID) {
            mListView.setSelection(mCountySelectIndex);
        }
    }

    private void setStreets(List<JdStreet> streetList, int position) {
        if (ListUtils.isEmpty(streetList)) {
            return;
        }
        this.mStreetData = streetList;
        mTabIndex = INDEX_TAB_STREET;
        this.mStreetSelectIndex = position;
        JdStreet street = this.mStreetData.get(position);
        mTvTabStreet.setText(street.name);
        // 缓存区-街道数据
        String countyId = streetList.get(0).parentId;
        if (!mCounty2street.containsKey(countyId)) {
            List<JdStreet> jdStreets = new ArrayList<>();
            ListUtils.copy(streetList, jdStreets);
            mCounty2street.put(countyId, jdStreets);
        }
        mListView.setAdapter(mStreetAdapter);
        if (mStreetSelectIndex != INDEX_INVALID) {
            mListView.setSelection(mStreetSelectIndex);
        }
    }

    private void setVillages(List<JdVillage> villageList, int position) {
        if (ListUtils.isEmpty(villageList)) {
            return;
        }
        this.mVillageData = villageList;
        mTabIndex = INDEX_TAB_VILLAGE;
        this.mVillageSelectIndex = position;
        JdVillage village = this.mVillageData.get(position);
        mTvTabVillage.setText(village.name);
        // 缓存街道-村数据
        String streetId = villageList.get(0).parentId;
        if (!mStreet2Village.containsKey(streetId)) {
            List<JdVillage> jdVillages = new ArrayList<>();
            ListUtils.copy(villageList, jdVillages);
            mStreet2Village.put(streetId, jdVillages);
        }
        mListView.setAdapter(mVillageAdapter);
        if (mVillageSelectIndex != INDEX_INVALID) {
            mListView.setSelection(mVillageSelectIndex);
        }
    }

    /**
     * 刷新地址选择器
     */
    private void refreshSelector() {
        mLoadingPb.setVisibility(View.GONE);
        updateTabsVisibility();
        updateIndicator();
    }

    //--------------------------------------对外方法api------------------------------------------------

    /**
     * 选择器关闭时清空缓存，默认不清空
     */
    public void clearCacheData() {
        mProvince2city.clear();
        mCity2county.clear();
        mCounty2street.clear();
        mStreet2Village.clear();

        // 清空子级数据
        mProvinceData = null;
        mCityData = null;
        mCountyData = null;
        mStreetData = null;
        mVillageData = null;
        mProvinceAdapter.notifyDataSetChanged();
        mCityAdapter.notifyDataSetChanged();
        mCountyAdapter.notifyDataSetChanged();
        mStreetAdapter.notifyDataSetChanged();
        mVillageAdapter.notifyDataSetChanged();

        mProvinceSelectIndex = INDEX_INVALID;
        mCitySelectIndex = INDEX_INVALID;
        mCountySelectIndex = INDEX_INVALID;
        mStreetSelectIndex = INDEX_INVALID;
        mVillageSelectIndex = INDEX_INVALID;

        mTabIndex = INDEX_TAB_PROVINCE;
        mTvTabProvince.setText(TEXT_UNCHECKED);
        updateTabsVisibility();
        updateProgressVisibility();
        updateIndicator();
    }

    public OnAddressSelectedListener getOnSelectedListener() {
        return mOnSelectedListener;
    }

    /**
     * 设置回调接口
     *
     * @param listener
     */
    public void setOnAddressSelectedListener(OnAddressSelectedListener listener) {
        this.mOnSelectedListener = listener;
    }

    /**
     * 设置省列表
     *
     * @param provinceList 省份列表
     */
    public void setProvinces(List<JdProvince> provinceList) {
        mHandler.sendMessage(Message.obtain(mHandler, WHAT_PROVINCES_PROVIDED, provinceList));
    }

    /**
     * 设置市列表
     *
     * @param cityList 城市列表
     */
    public void setCities(List<JdCity> cityList) {
        mHandler.sendMessage(Message.obtain(mHandler, WHAT_CITIES_PROVIDED, cityList));
    }

    /**
     * 设置区列表
     *
     * @param countyList 区/县列表
     */
    public void setCounties(List<JdCounty> countyList) {
        mHandler.sendMessage(Message.obtain(mHandler, WHAT_COUNTIES_PROVIDED, countyList));
    }

    /**
     * 设置街道列表
     *
     * @param streetList 街道列表
     */
    public void setStreets(List<JdStreet> streetList) {
        mHandler.sendMessage(Message.obtain(mHandler, WHAT_STREETS_PROVIDED, streetList));
    }

    /**
     * 设置村列表
     *
     * @param villageList 村列表
     */
    public void setVillages(List<JdVillage> villageList) {
        mHandler.sendMessage(Message.obtain(mHandler, WHAT_VILLAGES_PROVIDED, villageList));
    }


    /**
     * 有地址数据的时候,直接设置地址选择器
     *
     * @param provinces     省份列表
     * @param provinceIndex 当前省在列表中的位置
     * @param cities        当前省份的城市列表
     * @param cityIndex     当前城市在列表中的位置
     * @param counties      当前城市的区县列表
     * @param countyIndex   当前区县在列表中的位置
     */
    public void setAddressSelector(@Nullable List<JdProvince> provinces, int provinceIndex,
                                   @Nullable List<JdCity> cities, int cityIndex,
                                   @Nullable List<JdCounty> counties, int countyIndex,
                                   @Nullable List<JdStreet> streets, int streetIndex,
                                   @Nullable List<JdVillage> villages, int villageIndex) {
        if (ListUtils.notEmpty(provinces)) {
            setProvinces(provinces, provinceIndex);
        }
        if (ListUtils.notEmpty(cities)) {
            setCities(cities, cityIndex);
        }
        if (ListUtils.notEmpty(counties)) {
            setCounties(counties, countyIndex);
        }
        if (ListUtils.notEmpty(streets)) {
            setStreets(streets, streetIndex);
        }
        if (ListUtils.notEmpty(villages)) {
            setVillages(villages, villageIndex);
        }
        refreshSelector();
    }

    /**
     * 隐藏loading
     */
    public void hideLoading() {
        mLoadingPb.setVisibility(View.GONE);
    }


    public interface OnCloseClickListener {
        void onCloseClick();
    }

    private OnCloseClickListener onCloseClickListener;

    public void setOnCloseClickListener(OnCloseClickListener onCloseClickListener) {
        this.onCloseClickListener = onCloseClickListener;
    }
}
