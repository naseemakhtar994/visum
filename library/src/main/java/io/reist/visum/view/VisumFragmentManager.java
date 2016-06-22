package io.reist.visum.view;

import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * Use this class to manage your {@link VisumFragment}s.
 *
 * Created by defuera on 29/01/2016.
 *
 * TODO perhaps it should be removed from Visum
 */
public class VisumFragmentManager {

    /**
     * @param fragment - fragment to display
     * @param remove   - boolean, stays for whether current fragment should be thrown away or stay in a back stack.
     *                 false to stay in a back stack
     * @param resId    - id of view group to put fragment to
     */
    @SuppressWarnings("unused")
    public static void showFragment(FragmentManager fragmentManager, VisumFragment fragment, @IdRes int resId, boolean remove) {
        showFragment(fragmentManager, fragment, resId, remove, false);
    }

    /**
     * @param popBackStackInclusive all entries up to but not including that entry will be removed
     */
    public static void showFragment(
            FragmentManager fragmentManager,
            VisumFragment fragment,
            @IdRes int resId,
            boolean remove,
            boolean popBackStackInclusive
    ) {

        Fragment topmostFragment = findTopmostFragment(fragmentManager);
        replace(fragmentManager, topmostFragment, fragment, resId, remove, popBackStackInclusive);
    }

    private static void replace(
            FragmentManager fragmentManager,
            @Nullable Fragment what,
            VisumFragment with,
            @IdRes int resId,
            boolean remove,
            boolean popBackStackInclusive
    ) {

        if (popBackStackInclusive && fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate(fragmentManager.getBackStackEntryAt(0).getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (what != null && !popBackStackInclusive) {
            if (remove) {
                transaction = transaction.remove(what);
            } else {
                transaction = transaction.hide(what);
            }
        }

        String fragmentName = with.getName();

        if (with.isAdded()) {
            transaction = transaction.show(with);
        } else {
            transaction = transaction.add(resId, with, fragmentName);
        }

        transaction.addToBackStack(fragmentName).commit();
    }

    @Nullable
    public static Fragment findTopmostFragment(FragmentManager fragmentManager) {
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();
        Fragment topmostFragment;
        if (backStackEntryCount > 0) {
            String fragmentName = fragmentManager.getBackStackEntryAt(backStackEntryCount - 1).getName();
            topmostFragment = fragmentManager.findFragmentByTag(fragmentName);
        } else {
            topmostFragment = null;
        }
        return topmostFragment;
    }

}