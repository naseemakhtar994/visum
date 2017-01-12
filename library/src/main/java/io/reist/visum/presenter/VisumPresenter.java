/*
 * Copyright (C) 2017 Renat Sarymsakov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reist.visum.presenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.reist.visum.view.VisumView;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * A MVP presenter which is capable of handling multiple views.
 *
 * VisumPresenter also manages {@link rx.Subscription}s which are used to populate the attached
 * views. If the last view is removed then all subscription performed by views will be stopped.
 *
 * Created by Reist on 10/15/15.
 *
 * @param <V> a type of views to be handled
 */
public abstract class VisumPresenter<V extends VisumView> {

    /**
     * View id is for old visum implementations. Do not rely on this constant in new code.
     *
     * @deprecated  specify view ids explicitly
     */
    @Deprecated
    public static final int VIEW_ID_DEFAULT = 0;

    private static class ViewHolder<V> {

        private V view;
        private int viewId;
        private CompositeSubscription subscriptions;

        public ViewHolder(int id, V view) {
            this.viewId = id;
            this.view = view;
            this.subscriptions = new CompositeSubscription();
        }

    }

    private final List<ViewHolder<V>> viewHolders = new ArrayList<>();

    private CompositeSubscription subscriptions;

    private ViewHolder<V> findViewHolderByViewId(int id) {
        for (ViewHolder<V> viewHolder : viewHolders) {
            if (viewHolder.viewId == id) {
                return viewHolder;
            }
        }
        return null;
    }

    /**
     * Attaches the given view to this presenter. For the view,
     * {@link #onViewAttached(int, VisumView)} will be called and the view will be able to subscribe
     * to {@link Observable}s and {@link Single}s via {@link #subscribe(Observable, Observer)},
     * {@link #subscribe(Single, Action1)} and {@link #subscribe(Single, SingleSubscriber)}.
     *
     * If null is passed as a view then a view with the given id will be detached from the
     * presenter. Method {@link #onViewDetached(int, VisumView)} will be called. If there are no
     * remaining views after removal then all existing subscriptions made by attached views will be
     * stopped.
     *
     * @param id        an id used by the presenter to distinguish the view from the others
     * @param view      a MVP view; use null to stop the view with the given id
     */
    public final void setView(int id, @Nullable V view) {

        ViewHolder<V> viewHolder = findViewHolderByViewId(id);

        if (viewHolder != null) {

            // remove the old view
            viewHolder.subscriptions.unsubscribe();
            viewHolder.subscriptions = null;
            onViewDetached(id, viewHolder.view);
            viewHolders.remove(viewHolder);

            if (getViewCount() == 0) {
                subscriptions.unsubscribe();
                subscriptions = null;
                onStop();
            }

        }

        if (view != null) {

            if (getViewCount() == 0) {
                subscriptions = new CompositeSubscription();
                onStart();
            }

            // start the given view
            viewHolders.add(new ViewHolder<>(id, view));
            onViewAttached(id, view);

        }

    }

    public void onStop() {}

    public void onStart() {}

    @NonNull
    private ViewHolder<V> findViewHolderByViewIdOrThrow(int id) {
        ViewHolder<V> viewHolder = findViewHolderByViewId(id);
        if (viewHolder == null) {
            throw new ViewNotFoundException(id);
        }
        return viewHolder;
    }

    @SuppressWarnings("unused")
    public final <T> Subscription subscribe(int viewId, Observable<T> observable, Observer<? super T> observer) {
        ViewHolder<V> viewHolder = findViewHolderByViewIdOrThrow(viewId);
        Subscription subscription = startSubscription(observable, observer);

        viewHolder.subscriptions.add(subscription);

        return subscription;
    }

    @SuppressWarnings("unused")
    public final <T> Subscription subscribe(int viewId, Single<T> single, Action1<T> action) {
        ViewHolder<V> viewHolder = findViewHolderByViewIdOrThrow(viewId);
        Subscription subscription = startSubscription(single, action);

        viewHolder.subscriptions.add(subscription);

        return subscription;
    }

    @SuppressWarnings("unused")
    public final <T> Subscription subscribe(int viewId, Single<T> single, SingleSubscriber<T> subscriber) {
        ViewHolder<V> viewHolder = findViewHolderByViewIdOrThrow(viewId);
        Subscription subscription = startSubscription(single, subscriber);

        viewHolder.subscriptions.add(subscription);

        return subscription;
    }

    @SuppressWarnings("unused")
    public final <T> Subscription subscribe(Observable<T> observable, @NonNull final ViewNotifier<V, T> viewNotifier) {
        Subscription subscription = startSubscription(observable, new Observer<T>() {

            @Override
            public void onCompleted() {
                notifyCompleted(viewNotifier);
            }

            @Override
            public void onError(Throwable e) {
                notifyError(viewNotifier, e);
            }

            @Override
            public void onNext(T t) {
                notifyResult(viewNotifier, t);
            }

        });

        subscriptions.add(subscription);

        return subscription;
    }

    @SuppressWarnings("unused")
    public final <T> Subscription subscribe(Single<T> single, @NonNull final ViewNotifier<V, T> viewNotifier) {
        Subscription subscription = startSubscription(single, new SingleSubscriber<T>() {

            @Override
            public void onSuccess(T t) {
                notifyResult(viewNotifier, t);
            }

            @Override
            public void onError(Throwable e) {
                notifyError(viewNotifier, e);
            }

        });

        subscriptions.add(subscription);

        return subscription;
    }

    private <T> Subscription startSubscription(Observable<T> observable, Observer<? super T> observer) {
        return observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private <T> Subscription startSubscription(Single<T> single, Action1<T> action) {
        return single
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action);
    }

    private <T> Subscription startSubscription(Single<T> single, SingleSubscriber<T> subscriber) {
        return single
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
    }

    @SuppressWarnings({"unused", "deprecation"})
    protected void onViewAttached(int id, @NonNull V view) {
        onViewAttached();
    }

    @SuppressWarnings({"unused", "deprecation"})
    protected void onViewDetached(int id, @NonNull V view) {
        onViewDetached();
    }

    @NonNull
    public final V view(int id) {
        return findViewHolderByViewIdOrThrow(id).view;
    }

    /**
     * @deprecated use {@link #setView(int, VisumView)} instead
     */
    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    public final void setView(V view) {
        setView(VIEW_ID_DEFAULT, view);
    }

    /**
     * @deprecated use {@link #onViewAttached(int, VisumView)} instead
     */
    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    protected void onViewAttached() {}

    /**
     * @deprecated use {@link #onViewDetached(int, VisumView)} instead
     */
    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    protected void onViewDetached() {}

    /**
     * @deprecated use {@link #view(int)} instead
     */
    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    @NonNull
    public final V view() {
        return view(VIEW_ID_DEFAULT);
    }

    /**
     * @deprecated use {@link #subscribe(int, Observable, Observer)} instead
     */
    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    public final <T> Subscription subscribe(Observable<T> observable, Observer<? super T> observer) {
        return subscribe(VIEW_ID_DEFAULT, observable, observer);
    }

    /**
     * @deprecated use {@link #subscribe(int, Single, Action1)} instead
     */
    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    public final <T> Subscription subscribe(Single<T> single, Action1<T> action) {
        return subscribe(VIEW_ID_DEFAULT, single, action);
    }
    /**
     * @deprecated use {@link #subscribe(int, Single, SingleSubscriber)} instead
     */
    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    public final <T> Subscription subscribe(Single<T> single, SingleSubscriber<T> subscriber) {
        return subscribe(VIEW_ID_DEFAULT, single, subscriber);
    }

    @SuppressWarnings("unused")
    public final void forEachView(@NonNull Action1<V> action1) {
        for (ViewHolder<V> viewHolder : viewHolders) {
            action1.call(viewHolder.view);
        }
    }

    private <T> void notifyCompleted(@NonNull ViewNotifier<V, T> viewNotifier) {
        for (ViewHolder<V> viewHolder : viewHolders) {
            viewNotifier.notifyCompleted(viewHolder.view);
        }
    }

    private <T> void notifyResult(@NonNull ViewNotifier<V, T> viewNotifier, T t) {
        for (ViewHolder<V> viewHolder : viewHolders) {
            viewNotifier.notifyResult(viewHolder.view, t);
        }
    }

    private <T> void notifyError(@NonNull ViewNotifier<V, T> viewNotifier, Throwable e) {
        for (ViewHolder<V> viewHolder : viewHolders) {
            viewNotifier.notifyError(viewHolder.view, e);
        }
    }

    public final int getViewCount() {
        return viewHolders.size();
    }

    public final boolean hasSubscriptions() {
        return subscriptions != null && subscriptions.hasSubscriptions();
    }

    public final boolean hasSubscriptions(int viewId) {
        try {
            ViewHolder<V> viewHolder = findViewHolderByViewIdOrThrow(viewId);
            return viewHolder.subscriptions != null && viewHolder.subscriptions.hasSubscriptions();
        } catch (ViewNotFoundException e) {
            return false;
        }
    }

    public static class ViewNotFoundException extends RuntimeException {

        private final int viewId;

        public ViewNotFoundException(int viewId) {
            super("No view with id = " + viewId);
            this.viewId = viewId;
        }

        public int getViewId() {
            return viewId;
        }

    }

}
