package com.cuvent.experiences.friends.nativemodules.ContextMenu;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupMenu;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.queue.MessageQueueThreadImpl;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.swmansion.gesturehandler.GestureHandler;
import com.swmansion.gesturehandler.LongPressGestureHandler;
import com.swmansion.gesturehandler.OnTouchEventListener;
import com.swmansion.gesturehandler.react.RNGestureHandlerModule;

import javax.annotation.Nullable;

public class ContextMenuView extends ReactViewGroup implements PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener {

    private final MessageQueueThreadImpl nativeModulesThread;
    private final RNGestureHandlerModule gestureHandlerModule;

    public class Action {
        String title;
        boolean disabled;

        public Action(String title, boolean disabled) {
            this.title = title;
            this.disabled = disabled;
        }
    }

    PopupMenu contextMenu;

    LongPressGestureHandler longPressGestureHandler;

    boolean cancelled = true;

    public ContextMenuView(final ReactContext context) {
        super(context);

        contextMenu = new PopupMenu(getContext(), this);
        contextMenu.setOnMenuItemClickListener(this);
        contextMenu.setOnDismissListener(this);

        nativeModulesThread = (MessageQueueThreadImpl) context.getCatalystInstance().getReactQueueConfiguration().getNativeModulesQueueThread();
        gestureHandlerModule = ((ReactContext)getContext()).getNativeModule(RNGestureHandlerModule.class);

        initLongPressGestureHandler();
    }

    protected void initLongPressGestureHandler() {
        this.longPressGestureHandler = new LongPressGestureHandler(getContext());
        this.longPressGestureHandler.setOnTouchEventListener(new OnTouchEventListener<LongPressGestureHandler>() {
            @Override
            public void onTouchEvent(LongPressGestureHandler handler, MotionEvent event) {
                // swallow
            }

            @Override
            public void onStateChange(LongPressGestureHandler handler, int newState, int oldState) {
                if (newState == GestureHandler.STATE_ACTIVE) {
                    contextMenu.show();
                }
            }
        });
        gestureHandlerModule.getRegistry().registerHandler(this.longPressGestureHandler);
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);

        child.setClickable(false);

        if (this.longPressGestureHandler == null) {
            initLongPressGestureHandler();
        }

        dispatchInAppropriateThread(() -> {
            Log.d("ContextMenuView", "This tag: " + this.longPressGestureHandler.getTag() + " this id: " + child.getId());
            gestureHandlerModule.attachGestureHandler(this.longPressGestureHandler.getTag(), child.getId());
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        gestureHandlerModule.dropGestureHandler(this.longPressGestureHandler.getTag());
        this.longPressGestureHandler = null;
    }

    public void setActions(@Nullable ReadableArray actions) {
        Menu menu = contextMenu.getMenu();
        menu.clear();

        for (int i = 0; i < actions.size(); i++) {
            ReadableMap action = actions.getMap(i);
            int order = i;
            menu.add(Menu.NONE, Menu.NONE, order, action.getString("title"));
            menu.getItem(i).setEnabled(!action.hasKey("disabled") || !action.getBoolean("disabled"));
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        cancelled = false;
        ReactContext reactContext = (ReactContext) getContext();
        WritableMap event = Arguments.createMap();
        event.putInt("index", menuItem.getOrder());
        event.putString("name", menuItem.getTitle().toString());
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onPress", event);
        return false;
    }

    @Override
    public void onDismiss(PopupMenu popupMenu) {
        if (cancelled) {
            ReactContext reactContext = (ReactContext) getContext();
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onCancel", null);
        }

        cancelled = true;
    }

    /**
     * Helper function. It can happen that {@link #addView(View, int)} gets called from
     * the native module queue thread. This ensures that any action is being called from
     * the currently active thread.
     * From: https://stackoverflow.com/questions/48896212/which-thread-am-i-supposed-to-use-to-call-uimanager-calls-inside-a-react-native
     * @param runnable
     */
    protected void dispatchInAppropriateThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }

        if (nativeModulesThread.getLooper().getThread().isAlive()) {
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(runnable);
        } else {
            this.post(runnable);
        }
    }
}
