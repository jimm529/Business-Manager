package com.example.marketplace.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.marketplace.R;

/**
 * The one place that says which screens each role may open, and which tabs each role sees.
 * The operator (buyer) and the vendor (seller) share the login and the Home / Messages / More
 * tabs, but everything else belongs to exactly one of them. The server also enforces this
 * with its own permissions; this keeps the app from even showing a screen that would be refused.
 */
public final class RoleAccess {

    /** Look of the second tab and the round middle button, which differ per role. */
    public static final class TabSet {
        @IdRes public final int second;
        @DrawableRes public final int secondIcon;
        @StringRes public final int secondLabel;
        @IdRes public final int action;
        @StringRes public final int actionLabel;

        private TabSet(int second, int secondIcon, int secondLabel, int action, int actionLabel) {
            this.second = second;
            this.secondIcon = secondIcon;
            this.secondLabel = secondLabel;
            this.action = action;
            this.actionLabel = actionLabel;
        }
    }

    private static final TabSet OPERATOR_TABS = new TabSet(
            R.id.marketFragment, R.drawable.ic_nav_market, R.string.nav_market,
            R.id.registeredVendorsFragment, R.string.nav_action);

    private static final TabSet VENDOR_TABS = new TabSet(
            R.id.vendorRequestsFragment, R.drawable.ic_orders, R.string.nav_requests,
            R.id.vendorListingsFragment, R.string.nav_listings);

    private RoleAccess() { }

    /** Anything that is not clearly a vendor is treated as an operator, like the Home screen does. */
    public static boolean isVendor(@Nullable String role) {
        return Roles.VENDOR.equalsIgnoreCase(role);
    }

    @NonNull
    public static TabSet tabsFor(@Nullable String role) {
        return isVendor(role) ? VENDOR_TABS : OPERATOR_TABS;
    }

    /** May this role open this screen? Screens not listed as role-specific are open to both. */
    public static boolean canOpen(@Nullable String role, @IdRes int destinationId) {
        if (isOperatorOnly(destinationId)) {
            return !isVendor(role);
        }
        if (isVendorOnly(destinationId)) {
            return isVendor(role);
        }
        return true;
    }

    private static boolean isOperatorOnly(int id) {
        return id == R.id.marketFragment
                || id == R.id.ordersFragment
                || id == R.id.purchaseHistoryFragment
                || id == R.id.registeredVendorsFragment
                || id == R.id.vendorInventoryFragment
                || id == R.id.operatorSettingsFragment
                || id == R.id.counterPriceFragment
                || id == R.id.offerAcceptedFragment;
    }

    private static boolean isVendorOnly(int id) {
        return id == R.id.vendorRequestsFragment
                || id == R.id.vendorSalesFragment
                || id == R.id.vendorSettingsFragment
                || id == R.id.vendorListingsFragment;
    }
}
