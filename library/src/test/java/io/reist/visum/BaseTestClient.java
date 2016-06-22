package io.reist.visum;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by Reist on 26.05.16.
 */
public abstract class BaseTestClient implements VisumClient {

    private final ComponentCache componentCache;

    protected BaseTestClient(ComponentCache componentCache) {
        this.componentCache = componentCache;
    }

    @Override
    public ComponentCache getComponentCache() {
        return componentCache;
    }

    @Override
    public void onStartClient() {
        componentCache.start(this);
    }

    @Override
    public void onStopClient() {
        componentCache.stop(this, false);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Context getContext() {
        return null;
    }

}
