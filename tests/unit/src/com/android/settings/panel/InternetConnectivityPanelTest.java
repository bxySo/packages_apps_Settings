/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.AirplaneModePreferenceController;
import com.android.settings.network.InternetUpdater;
import com.android.settings.network.ProviderModelSliceHelper;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InternetConnectivityPanelTest {

    public static final String TITLE_INTERNET = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "provider_internet_settings");
    public static final String TITLE_APM = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "airplane_mode");
    public static final String SUBTITLE_TEXT_WIFI_IS_OFF =
            ResourcesUtils.getResourcesString(ApplicationProvider.getApplicationContext(),
                    "wifi_is_off");
    public static final String SUBTITLE_TEXT_TAP_A_NETWORK_TO_CONNECT =
            ResourcesUtils.getResourcesString(ApplicationProvider.getApplicationContext(),
                    "tap_a_network_to_connect");
    public static final String SUBTITLE_NON_CARRIER_NETWORK_UNAVAILABLE =
            ResourcesUtils.getResourcesString(ApplicationProvider.getApplicationContext(),
                    "non_carrier_network_unavailable");
    public static final String SUBTITLE_ALL_NETWORK_UNAVAILABLE =
            ResourcesUtils.getResourcesString(ApplicationProvider.getApplicationContext(),
                    "all_network_unavailable");
    public static final String BUTTON_TURN_ON_WIFI = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "turn_on_wifi");
    public static final String BUTTON_TURN_OFF_WIFI = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "turn_off_wifi");

    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Mock
    Handler mMainThreadHandler;
    @Mock
    PanelContentCallback mPanelContentCallback;
    @Mock
    InternetUpdater mInternetUpdater;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ProviderModelSliceHelper mProviderModelSliceHelper;
    @Mock
    private FragmentActivity mPanelActivity;

    private Context mContext;
    private InternetConnectivityPanel mPanel;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getMainThreadHandler()).thenReturn(mMainThreadHandler);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);

        mPanel = InternetConnectivityPanel.create(mContext);
        mPanel.registerCallback(mPanelContentCallback);
        mPanel.mIsProviderModelEnabled = true;
        mPanel.mInternetUpdater = mInternetUpdater;
        mPanel.mProviderModelSliceHelper = mProviderModelSliceHelper;
    }

    @Test
    public void getTitle_apmOff_shouldBeInternet() {
        doReturn(false).when(mInternetUpdater).isAirplaneModeOn();

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_INTERNET);
    }

    @Test
    public void getTitle_apmOn_shouldBeApm() {
        doReturn(true).when(mInternetUpdater).isAirplaneModeOn();

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_APM);
    }

    @Test
    public void getSubTitle_apmOnWifiOff_shouldBeNull() {
        doReturn(true).when(mInternetUpdater).isAirplaneModeOn();
        doReturn(false).when(mInternetUpdater).isWifiEnabled();

        assertThat(mPanel.getSubTitle()).isNull();
    }

    @Test
    public void getSubTitle_apmOnWifiOn_shouldBeNull() {
        doReturn(true).when(mInternetUpdater).isAirplaneModeOn();
        doReturn(true).when(mInternetUpdater).isWifiEnabled();

        assertThat(mPanel.getSubTitle()).isNull();
    }

    @Test
    public void getSubTitle_apmOffWifiOff_wifiIsOn() {
        doReturn(false).when(mInternetUpdater).isAirplaneModeOn();
        doReturn(false).when(mInternetUpdater).isWifiEnabled();

        mPanel.updatePanelTitle();

        assertThat(mPanel.getSubTitle()).isEqualTo(SUBTITLE_TEXT_WIFI_IS_OFF);
    }

    @Test
    public void getSubTitle_apmOffWifiOnNoWifiListHasCarrierData_NonCarrierNetworkUnavailable() {
        List wifiList = new ArrayList<ScanResult>();
        mockCondition(false, true, true, true, true, true, wifiList);

        mPanel.updatePanelTitle();

        assertThat(mPanel.getSubTitle()).isEqualTo(SUBTITLE_NON_CARRIER_NETWORK_UNAVAILABLE);
    }

    @Test
    public void getSubTitle_apmOffWifiOnNoWifiListNoCarrierItem_AllNetworkUnavailable() {
        List wifiList = new ArrayList<ScanResult>();
        mockCondition(false, false, false, false, false, true, wifiList);

        mPanel.updatePanelTitle();

        assertThat(mPanel.getSubTitle()).isEqualTo(SUBTITLE_ALL_NETWORK_UNAVAILABLE);
    }

    @Test
    public void getSubTitle_apmOffWifiOnNoWifiListNoDataSimActive_AllNetworkUnavailable() {
        List wifiList = new ArrayList<ScanResult>();
        mockCondition(false, true, false, true, true, true, wifiList);

        mPanel.updatePanelTitle();

        assertThat(mPanel.getSubTitle()).isEqualTo(SUBTITLE_ALL_NETWORK_UNAVAILABLE);
    }

    @Test
    public void getSubTitle_apmOffWifiOnNoWifiListNoService_AllNetworkUnavailable() {
        List wifiList = new ArrayList<ScanResult>();
        mockCondition(false, true, false, true, false, true, wifiList);

        mPanel.updatePanelTitle();

        assertThat(mPanel.getSubTitle()).isEqualTo(SUBTITLE_ALL_NETWORK_UNAVAILABLE);
    }

    @Test
    public void getSubTitle_apmOffWifiOnTwoWifiItemsNoCarrierData_tapANetworkToConnect() {
        List wifiList = new ArrayList<ScanResult>();
        wifiList.add(new ScanResult());
        wifiList.add(new ScanResult());
        mockCondition(false, true, false, true, true, true, wifiList);

        mPanel.updatePanelTitle();

        assertThat(mPanel.getSubTitle()).isEqualTo(SUBTITLE_TEXT_TAP_A_NETWORK_TO_CONNECT);
    }

    @Test
    public void getCustomizedButtonTitle_wifiOff_turnOnWifi() {
        doReturn(false).when(mInternetUpdater).isWifiEnabled();

        assertThat(mPanel.getCustomizedButtonTitle()).isEqualTo(BUTTON_TURN_ON_WIFI);
    }

    @Test
    public void getCustomizedButtonTitle_wifiOn_turnOffWifi() {
        doReturn(true).when(mInternetUpdater).isWifiEnabled();

        assertThat(mPanel.getCustomizedButtonTitle()).isEqualTo(BUTTON_TURN_OFF_WIFI);
    }

    @Test
    public void getSlices_providerModelDisabled_containsNecessarySlices() {
        mPanel.mIsProviderModelEnabled = false;
        List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                AirplaneModePreferenceController.SLICE_URI,
                CustomSliceRegistry.MOBILE_DATA_SLICE_URI,
                CustomSliceRegistry.WIFI_SLICE_URI);
    }

    @Test
    public void getSlices_providerModelEnabled_containsNecessarySlices() {
        List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI);
    }

    @Test
    public void getSeeMoreIntent_shouldBeNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNull();
    }

    @Test
    public void onClickCustomizedButton_wifiOn_setWifiOff() {
        doReturn(true).when(mInternetUpdater).isWifiEnabled();

        mPanel.onClickCustomizedButton(mPanelActivity);

        verify(mWifiManager).setWifiEnabled(false);
    }

    @Test
    public void onClickCustomizedButton_wifiOff_setWifiOn() {
        doReturn(false).when(mInternetUpdater).isWifiEnabled();

        mPanel.onClickCustomizedButton(mPanelActivity);

        verify(mWifiManager).setWifiEnabled(true);
    }

    @Test
    public void onClickCustomizedButton_shouldNotFinishActivity() {
        mPanel.onClickCustomizedButton(mPanelActivity);

        verify(mPanelActivity, never()).finish();
    }

    @Test
    public void updatePanelTitle_onHeaderChanged() {
        clearInvocations(mPanelContentCallback);

        mPanel.updatePanelTitle();

        verify(mPanelContentCallback).onHeaderChanged();
    }

    @Test
    public void onWifiEnabledChanged_wifiOff_onCustomizedButtonStateChanged() {
        doReturn(false).when(mInternetUpdater).isWifiEnabled();
        clearInvocations(mPanelContentCallback);

        mPanel.onWifiEnabledChanged(false);

        verify(mPanelContentCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void onWifiEnabledChanged_wifiOn_onCustomizedButtonStateChanged() {
        doReturn(true).when(mInternetUpdater).isWifiEnabled();
        clearInvocations(mPanelContentCallback);

        mPanel.onWifiEnabledChanged(true);

        verify(mPanelContentCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void showProgressBar_wifiDisabled_hideProgress() {
        mPanel.mIsProgressBarVisible = true;
        doReturn(false).when(mInternetUpdater).isWifiEnabled();
        clearInvocations(mPanelContentCallback);

        mPanel.showProgressBar();

        assertThat(mPanel.isProgressBarVisible()).isFalse();
        verify(mPanelContentCallback).onProgressBarVisibleChanged();
    }

    @Test
    public void showProgressBar_noWifiScanResults_showProgressForever() {
        mPanel.mIsProgressBarVisible = false;
        doReturn(true).when(mInternetUpdater).isWifiEnabled();
        List<ScanResult> noWifiScanResults = new ArrayList<>();
        doReturn(noWifiScanResults).when(mWifiManager).getScanResults();
        clearInvocations(mPanelContentCallback);

        mPanel.showProgressBar();

        assertThat(mPanel.isProgressBarVisible()).isTrue();
        verify(mPanelContentCallback).onProgressBarVisibleChanged();
        verify(mPanelContentCallback).onHeaderChanged();
        verify(mMainThreadHandler, never())
                .postDelayed(any() /* mHideProgressBarRunnable */, anyLong());
    }

    @Test
    public void showProgressBar_hasWifiScanResults_showProgressDelayedHide() {
        mPanel.mIsProgressBarVisible = false;
        doReturn(true).when(mInternetUpdater).isWifiEnabled();
        List<ScanResult> hasWifiScanResults = mock(ArrayList.class);
        doReturn(1).when(hasWifiScanResults).size();
        doReturn(hasWifiScanResults).when(mWifiManager).getScanResults();
        clearInvocations(mPanelContentCallback);

        mPanel.showProgressBar();

        assertThat(mPanel.isProgressBarVisible()).isTrue();
        verify(mPanelContentCallback).onProgressBarVisibleChanged();
        verify(mMainThreadHandler).postDelayed(any() /* mHideProgressBarRunnable */, anyLong());
    }

    @Test
    public void setProgressBarVisible_onProgressBarVisibleChanged() {
        mPanel.mIsProgressBarVisible = false;
        doReturn(true).when(mInternetUpdater).isWifiEnabled();
        clearInvocations(mPanelContentCallback);

        mPanel.setProgressBarVisible(true);

        assertThat(mPanel.mIsProgressBarVisible).isTrue();
        verify(mPanelContentCallback).onProgressBarVisibleChanged();
        verify(mPanelContentCallback).onHeaderChanged();
    }

    private void mockCondition(boolean airplaneMode, boolean hasCarrier,
            boolean isDataSimActive, boolean isMobileDataEnabled, boolean isServiceInService,
            boolean isWifiEnabled, List<ScanResult> wifiItems) {
        doReturn(airplaneMode).when(mInternetUpdater).isAirplaneModeOn();
        when(mProviderModelSliceHelper.hasCarrier()).thenReturn(hasCarrier);
        when(mProviderModelSliceHelper.isDataSimActive()).thenReturn(isDataSimActive);
        when(mProviderModelSliceHelper.isMobileDataEnabled()).thenReturn(isMobileDataEnabled);
        when(mProviderModelSliceHelper.isDataStateInService()).thenReturn(isServiceInService);
        when(mProviderModelSliceHelper.isVoiceStateInService()).thenReturn(isServiceInService);
        doReturn(isWifiEnabled).when(mInternetUpdater).isWifiEnabled();
        doReturn(wifiItems).when(mWifiManager).getScanResults();
    }
}
