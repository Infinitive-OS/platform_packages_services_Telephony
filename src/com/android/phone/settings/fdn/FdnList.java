/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.phone.settings.fdn;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.phone.ADNList;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.internal.telephony.ConfigResourceUtil;

/**
 * Fixed Dialing Number (FDN) List UI for the Phone app. FDN is a feature of the service provider
 * that allows a user to specify a limited set of phone numbers that the SIM can dial.
 */
public class FdnList extends ADNList {
    private static final int MENU_ADD = 1;
    private static final int MENU_EDIT = 2;
    private static final int MENU_DELETE = 3;
    private static final int MENU_DIAL = 4;

    private static final String INTENT_EXTRA_NAME = "name";
    private static final String INTENT_EXTRA_NUMBER = "number";

    private static final Uri FDN_CONTENT_URI = Uri.parse("content://icc/fdn");
    private static final String FDN_CONTENT_PATH_WITH_SUB_ID = "content://icc/fdn/subId/";
    private ConfigResourceUtil mConfigResUtil = new ConfigResourceUtil();

    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.fdn_list_with_label);
    }

    @Override
    protected Uri resolveIntent() {
        Intent intent = getIntent();
        intent.setData(getContentUri(mSubscriptionInfoHelper));
        return intent.getData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Resources r = getResources();

        // Added the icons to the context menu
        menu.add(0, MENU_ADD, 0, r.getString(R.string.menu_add))
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, MENU_EDIT, 0, r.getString(R.string.menu_edit))
                .setIcon(android.R.drawable.ic_menu_edit);
        menu.add(0, MENU_DELETE, 0, r.getString(R.string.menu_delete))
                .setIcon(android.R.drawable.ic_menu_delete);
        menu.add(0, MENU_DIAL, 0, r.getString(R.string.menu_dial_string));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean hasSelection = (getSelectedItemPosition() >= 0);

        menu.findItem(MENU_ADD).setVisible(true);
        menu.findItem(MENU_EDIT).setVisible(hasSelection);
        menu.findItem(MENU_DELETE).setVisible(hasSelection);
        menu.findItem(MENU_DIAL).setVisible(hasSelection
                && mConfigResUtil.getBooleanValue(this,
                        "config_fdn_direct_dial"));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:  // See ActionBar#setDisplayHomeAsUpEnabled()
                Intent intent = mSubscriptionInfoHelper.getIntent(FdnSetting.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;

            case MENU_ADD:
                addContact();
                return true;

            case MENU_EDIT:
                editSelected();
                return true;

            case MENU_DELETE:
                deleteSelected();
                return true;

            case MENU_DIAL:
                dialSelected();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO: is this what we really want?
        if (!mConfigResUtil.getBooleanValue(this, "config_fdn_direct_dial")) {
            editSelected(position);
        } else {
            SelectionPopUpMenu menu = new SelectionPopUpMenu(getApplicationContext(), v, position);
            menu.showPopUp();
        }
    }

    private void addContact() {
        //If there is no INTENT_EXTRA_NAME provided, EditFdnContactScreen treats it as an "add".
        Intent intent = mSubscriptionInfoHelper.getIntent(EditFdnContactScreen.class);
        startActivity(intent);
    }

    /**
     * Overloaded to call editSelected with the current selection
     * by default.  This method may have a problem with touch UI
     * since touch UI does not really have a concept of "selected"
     * items.
     */
    private void editSelected() {
        editSelected(getSelectedItemPosition());
    }

    /**
     * Edit the item at the selected position in the list.
     */
    private void editSelected(int position) {
        if (mCursor.moveToPosition(position)) {
            String name = mCursor.getString(NAME_COLUMN);
            String number = mCursor.getString(NUMBER_COLUMN);

            Intent intent = mSubscriptionInfoHelper.getIntent(EditFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_NAME, name);
            intent.putExtra(INTENT_EXTRA_NUMBER, number);
            startActivity(intent);
        }
    }

    private void deleteSelected() {
        if (mCursor.moveToPosition(getSelectedItemPosition())) {
            String name = mCursor.getString(NAME_COLUMN);
            String number = mCursor.getString(NUMBER_COLUMN);

            Intent intent = mSubscriptionInfoHelper.getIntent(DeleteFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_NAME, name);
            intent.putExtra(INTENT_EXTRA_NUMBER, number);
            startActivity(intent);
        }
    }

    /**
     * Returns the uri for updating the ICC FDN entry, taking into account the subscription id.
     */
    public static Uri getContentUri(SubscriptionInfoHelper subscriptionInfoHelper) {
        return subscriptionInfoHelper.hasSubId()
                ? Uri.parse(FDN_CONTENT_PATH_WITH_SUB_ID + subscriptionInfoHelper.getSubId())
                : FDN_CONTENT_URI;
    }

    private void dialSelected() {
        dialSelected(getSelectedItemPosition());
    }

    private void dialSelected(int position) {
        int prviousPos = mCursor.getPosition();
        String number = null;
        if (mCursor.moveToPosition(position)) {
            number = mCursor.getString(NUMBER_COLUMN);
        }
        mCursor.moveToPosition(prviousPos);
        if (number != null) {
            Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
            final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, uri);
            startActivity(intent);
        }
    }

    class SelectionPopUpMenu extends PopupMenu {

        private int position = 0;
        private OnMenuItemClickListener mMenuItemListener = new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == MENU_DIAL) {
                    dialSelected(position);
                } else if (item.getItemId() == MENU_EDIT) {
                    editSelected(position);
                }
                return true;
            }
        };

        public SelectionPopUpMenu(Context context, View anchor, int position) {
            super(context, anchor, Gravity.RIGHT);
            this.position = position;
        }

        public void showPopUp() {
            getMenu().add(0, MENU_DIAL, 0, getString(R.string.menu_dial_string));
            getMenu().add(0, MENU_EDIT, 0, getString(R.string.menu_edit));
            setOnMenuItemClickListener(mMenuItemListener);
            show();
        }
    }

}
