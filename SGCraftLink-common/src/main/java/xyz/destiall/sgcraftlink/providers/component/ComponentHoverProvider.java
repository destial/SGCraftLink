package xyz.destiall.sgcraftlink.providers.component;

import java.util.function.Supplier;

public abstract class ComponentHoverProvider<T> {
    private static Supplier<?> provider;
    public static void setProvider(Supplier<?> provider) {
        ComponentHoverProvider.provider = provider;
    }

    public static ComponentHoverProvider<?> of(Action action, String hoverText) {
        ComponentHoverProvider<?> component = (ComponentHoverProvider<?>) provider.get();
        component.setAction(action);
        component.setText(hoverText);
        return component;
    }

    public static ComponentHoverProvider<?> of(Action action, ComponentProvider<?> textComponent) {
        ComponentHoverProvider<?> component = (ComponentHoverProvider<?>) provider.get();
        component.setAction(action);
        component.setText(textComponent);
        return component;
    }

    protected T hover;

    public ComponentHoverProvider(T hover) {
        this.hover = hover;
    }

    public abstract void setAction(Action action);
    public abstract void setText(String text);
    public abstract void setText(ComponentProvider<?> component);

    public T getHover() {
        return hover;
    }

    public enum Action {
        SHOW_TEXT,
        SHOW_ITEM,
        SHOW_ENTITY,
    }
}
