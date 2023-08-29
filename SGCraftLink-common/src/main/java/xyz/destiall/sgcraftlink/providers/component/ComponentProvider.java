package xyz.destiall.sgcraftlink.providers.component;

import xyz.destiall.sgcraftlink.providers.ColorProvider;

import java.util.function.Supplier;

public abstract class ComponentProvider<T> {
    private static Supplier<?> provider;

    public static void setProvider(Supplier<?> provider) {
        ComponentProvider.provider = provider;
    }

    public static ComponentProvider<?> of(String message) {
        ComponentProvider<?> component = (ComponentProvider<?>) provider.get();
        component.setMessage(message);
        return component;
    }

    protected T component;
    public ComponentProvider(T component) {
        this.component = component;
    }

    public abstract void setMessage(String message);

    public abstract void setColor(ColorProvider.ChatColor color);

    public abstract void setHover(ComponentHoverProvider<?> hover);

    public abstract void setClick(ComponentClickProvider<?> click);

    public T getComponent() {
        return component;
    }
}
