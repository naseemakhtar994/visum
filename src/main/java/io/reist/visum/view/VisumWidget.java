package io.reist.visum.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.LayoutRes;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import butterknife.ButterKnife;
import io.reist.visum.ComponentCache;
import io.reist.visum.ComponentCacheProvider;
import io.reist.visum.presenter.VisumPresenter;

/**
 * Created by defuera on 26/01/2016.
 * Base class for FrameLayout providing visum mvp inteface
 * @param <P> - subclass of VisumPresenter
 */
public abstract class VisumWidget<P extends VisumPresenter> extends FrameLayout implements VisumView<P>, VisumClient {

    private static final String ARG_STATE_COMPONENT_ID = "ARG_STATE_COMPONENT_ID";

    @LayoutRes
    private final int layoutRes;

    private Long componentId;
    private boolean stateSaved;

    public VisumWidget(Context context, @LayoutRes int layoutRes) {
        super(context);
        this.layoutRes = layoutRes;
    }

    public VisumWidget(Context context, AttributeSet attrs, @LayoutRes int layoutRes) {
        super(context, attrs);
        this.layoutRes = layoutRes;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        inject(getComponent());

        inflate(getContext(), layoutRes, this);
        ButterKnife.bind(this);
        attachPresenter();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (!stateSaved) {
            getComponentCache().invalidateComponentFor(this);
        }
        detachPresenter();
    }

    //region VisumView

    @SuppressWarnings("unchecked") //todo setView should be checked call
    @Override
    public void attachPresenter() {
        final P presenter = getPresenter();
        if (presenter != null) {
            presenter.setView(this);
        }
    }

    @SuppressWarnings("unchecked") //todo setView should be type safe call
    @Override
    public void detachPresenter() {
        if (getPresenter() != null)
            getPresenter().setView(null);
    }

    //endregion

    private ComponentCache getComponentCache() {
        ComponentCacheProvider application = (ComponentCacheProvider) getContext().getApplicationContext();
        return application.getComponentCache();
    }

    //region VisumClient

    @Override
    public Long getComponentId() {
        return componentId;
    }

    @Override
    public void setComponentId(Long componentId) {
        this.componentId = componentId;
    }

    @Override
    public Object getComponent() {
        if (getComponentCache() != null) {
            return getComponentCache().getComponentFor(this);
        } else {
            return null;
        }
    }

    //endregion

    @Override
    protected Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();

        stateSaved = true;

        Bundle bundle = new Bundle();
        bundle.putLong(ARG_STATE_COMPONENT_ID, componentId);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        if (state instanceof Bundle) {
            componentId = ((Bundle) state).getLong(ARG_STATE_COMPONENT_ID);
        }

        stateSaved = false;
    }

}