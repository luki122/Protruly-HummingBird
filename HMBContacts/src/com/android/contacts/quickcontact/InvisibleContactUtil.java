package com.android.contacts.quickcontact;


import com.google.common.collect.Iterables;

import com.android.contacts.ContactSaveService;
import com.android.contacts.common.GroupMetaData;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.GroupMembershipDataItem;

import android.content.Context;
import android.content.Intent;
import hb.provider.ContactsContract.CommonDataKinds.GroupMembership;

import java.util.List;

/**
 * Utility class to support adding invisible contacts. Ie, contacts that don't belong to the
 * default group.
 */
public class InvisibleContactUtil {

    public static boolean isInvisibleAndAddable(Contact contactData, Context context) {
        // Only local contacts
        if (contactData == null || contactData.isDirectoryEntry()) return false;

        // User profile cannot be added to contacts
        if (contactData.isUserProfile()) return false;

        // Only if exactly one raw contact
        if (contactData.getRawContacts().size() != 1) return false;

        // test if the default group is assigned
        final List<GroupMetaData> groups = contactData.getGroupMetaData();

        // For accounts without group support, groups is null
        if (groups == null) return false;

        // remember the default group id. no default group? bail out early
        final long defaultGroupId = getDefaultGroupId(groups);
        if (defaultGroupId == -1) return false;

        final RawContact rawContact = (RawContact) contactData.getRawContacts().get(0);
        final AccountType type = rawContact.getAccountType(context);
        // Offline or non-writeable account? Nothing to fix
        if (type == null || !type.areContactsWritable()) return false;

        // Check whether the contact is in the default group
        boolean isInDefaultGroup = false;
        for (DataItem dataItem : Iterables.filter(
                rawContact.getDataItems(), GroupMembershipDataItem.class)) {
            GroupMembershipDataItem groupMembership = (GroupMembershipDataItem) dataItem;
            final Long groupId = groupMembership.getGroupRowId();
            if (groupId != null && groupId == defaultGroupId) {
                isInDefaultGroup = true;
                break;
            }
        }

        return !isInDefaultGroup;
    }

    public static void addToDefaultGroup(Contact contactData, Context context) {
        final long defaultGroupId = getDefaultGroupId(contactData.getGroupMetaData());
        // there should always be a default group (otherwise the button would be invisible),
        // but let's be safe here
        if (defaultGroupId == -1) return;

        // add the group membership to the current state
        final RawContactDeltaList contactDeltaList = contactData.createRawContactDeltaList();
        final RawContactDelta rawContactEntityDelta = contactDeltaList.get(0);

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(
                context);
        final AccountType type = rawContactEntityDelta.getAccountType(accountTypes);
        final DataKind groupMembershipKind = type.getKindForMimetype(
                GroupMembership.CONTENT_ITEM_TYPE);
        final ValuesDelta entry = RawContactModifier.insertChild(rawContactEntityDelta,
                groupMembershipKind);
        if (entry == null) return;
        entry.setGroupRowId(defaultGroupId);

        // and fire off the intent. we don't need a callback, as the database listener
        // should update the ui
        final Intent intent = ContactSaveService.createSaveContactIntent(
                context,
                contactDeltaList, "", 0, false, QuickContactActivity.class,
                Intent.ACTION_VIEW, null, /* backPressed =*/ false);
        context.startService(intent);
    }

    /** return default group id or -1 if no group or several groups are marked as default */
    private static long getDefaultGroupId(List<GroupMetaData> groups) {
        long defaultGroupId = -1;
        for (GroupMetaData group : groups) {
            if (group.isDefaultGroup()) {
                // two default groups? return neither
                if (defaultGroupId != -1) return -1;
                defaultGroupId = group.getGroupId();
            }
        }
        return defaultGroupId;
    }
}
