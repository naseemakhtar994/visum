package io.reist.visum.view;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import io.reist.visum.ComponentCache;
import io.reist.visum.ComponentCacheProvider;
import io.reist.visum.presenter.BasePresenter;

public abstract class BaseFragment<P extends BasePresenter> extends Fragment implements BaseView {

    private static final String ARG_STATE_COMPONENT_ID = "ARG_STATE_COMPONENT_ID";

    public interface FragmentController {

        /**
         * @param fragment - fragment to display
         * @param remove   - boolean, stays for whether current fragment should be thrown away or stay in a back stack.
         *                 false to stay in a back stack
         */
        void showFragment(BaseFragment fragment, boolean remove);
    }

    private Long componentId;
    private boolean stateSaved;
    private int layoutResId;

    public BaseFragment(@LayoutRes int layoutResId) {
        this.layoutResId = layoutResId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        componentId = savedInstanceState == null ? null : savedInstanceState.getLong(ARG_STATE_COMPONENT_ID);
        stateSaved = false;

        inject(getComponent());
    }

    /// --- ///

    public final String getName() {
        return getClass().getName();
    }

    /// --- ///

    @Override
    public Context context() {
        return getActivity();
    }

    @Override
    public final Long getComponentId() {
        return componentId;
    }

    /**
     * Sets component id for current view
     *
     * @param componentId - unique id generated by ComponentCache
     */
    @Override
    public final void setComponentId(Long componentId) {
        this.componentId = componentId;
    }

    @Override
    public final Object getComponent() {
        if (getComponentCache() != null) {
            return getComponentCache().getComponentFor(this);
        } else {
            return null;
        }
    }

    private ComponentCache getComponentCache() {
        ComponentCacheProvider application = (ComponentCacheProvider) getActivity().getApplication();
        return application.getComponentCache();
    }

    /// --- ///

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(layoutResId, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @SuppressWarnings("unchecked") //todo setView should be checked call
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final P presenter = getPresenter();
        if (presenter != null) {
            ButterKnife.bind(presenter, view);
        }
        if (presenter != null) {
            presenter.setView(this);
        }
        ready();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARG_STATE_COMPONENT_ID, componentId);
        stateSaved = true;
    }

    @SuppressWarnings("unchecked") //todo setView should be type safe call
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!stateSaved) {
            getComponentCache().invalidateComponentFor(this);
        }
        if (getPresenter() != null)
            getPresenter().setView(null);
    }

    /// --- ///

    protected abstract void inject(Object from);

    public abstract P getPresenter();

    /**
     * this is called once view is inflated and ready
     * Put your initialization code here instead of in onViewCreated()
     */
    protected abstract void ready();

    protected FragmentController getFragmentController() {
        Object a = getActivity();
        if (a instanceof FragmentController) {
            return (FragmentController) a;
        } else {
            throw new IllegalArgumentException("Can't find " + FragmentController.class.getSimpleName());
        }
    }

}
