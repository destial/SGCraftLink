package xyz.destiall.sgcraftlink.providers.component;

import java.util.function.Supplier;

public abstract class ComponentClickProvider<T> {
    private static Supplier<?> provider;
    public static void setProvider(Supplier<?> provider) {
        ComponentClickProvider.provider = provider;
    }

    public static ComponentClickProvider<?> of(ComponentClickProvider.Action action, String clickText) {
        ComponentClickProvider<?> component = (ComponentClickProvider<?>) provider.get();
        component.setAction(action);
        component.setText(clickText);
        return component;
    }

    protected T click;

    public ComponentClickProvider(T click) {
        this.click = click;
    }

    public abstract void setAction(Action action);
    public abstract void setText(String text);

    public T getClick() {
        return click;
    }

    public enum Action {
        OPEN_URL,
        OPEN_FILE,
        RUN_COMMAND,
        SUGGEST_COMMAND,
        CHANGE_PAGE,
        COPY_TO_CLIPBOARD
    }
}
