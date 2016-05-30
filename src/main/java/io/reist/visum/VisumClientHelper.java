package io.reist.visum;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A helper class for implementations of {@link VisumClient}. It provides callback for typical
 * Android components such as {@link android.app.Service}.
 *
 * Created by Reist on 19.05.16.
 */
public final class VisumClientHelper<C extends VisumClient> {

    private static final String TAG = VisumClientHelper.class.getSimpleName();

    protected final C client;

    public VisumClientHelper(@NonNull C client) {
        this.client = client;
    }

    public C getClient() {
        return client;
    }

    @NonNull
    public ComponentCache getComponentCache() {
        return ((ComponentCacheProvider) client.getContext().getApplicationContext()).getComponentCache();
    }

    @SuppressWarnings("unchecked")
    public void onCreate() {
        Object component = client.getComponentCache().start(client);
        Class clazz = client.getClass();

        try {
            for (Method method : component.getClass().getMethods()) {
                Class types[] = method.getParameterTypes();
                if (types != null && types.length == 1 && clazz.isAssignableFrom(types[0])) {
                    method.invoke(component, client);
                    Log.d(TAG, String.format("Client [%s] was injected by [%s] with [%s]",
                            client.getClass().getSimpleName(),
                            component.getClass().getSimpleName(),
                            method.getName()));
                    return; // all ok
                }
            }
        } catch (IllegalAccessException e) {
            Log.wtf(TAG, e);
        } catch (InvocationTargetException e) {
            Log.wtf(TAG, e);
        }

        client.inject(component);
    }

    public void onDestroy() {
        client.getComponentCache().stop(client);
    }

    @NonNull
    public Context getContext() {
        return client.getContext();
    }

    public void onRestartClient() {
       client.inject(client.getComponentCache().findComponentEntryByClientOrThrow(client).component);
    }

}
