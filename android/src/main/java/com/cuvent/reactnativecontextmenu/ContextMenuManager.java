package com.cuvent.reactnativecontextmenu;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nullable;

public class ContextMenuManager extends ViewGroupManager<ContextMenuView> {

    public static final String REACT_CLASS = "ContextMenu";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public ContextMenuView createViewInstance(ThemedReactContext context) {
        ContextMenuView reactViewGroup = new ContextMenuView(context);
        return reactViewGroup;
    }

    @ReactProp(name = "title")
    public void setTitle(ContextMenuView view, @Nullable String title) {
        // TODO: Maybe support this? IDK if its necessary though
    }

    @ReactProp(name = "actions")
    public void setActions(ContextMenuView view, @Nullable ReadableArray actions) {
        view.setActions(actions);
    }

    @androidx.annotation.Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("onPress", MapBuilder.of("registrationName", "onPress"))
                .put("onCancel", MapBuilder.of("registrationName", "onCancel"))
                .build();
    }
}
