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

    public static ComponentProvider<?> of(String message, ColorProvider.ChatColor color) {
        ComponentProvider<?> component = of(message);
        component.setColor(color);
        return component;
    }

    protected T component;
    public ComponentProvider(T component) {
        this.component = component;
    }

    public abstract void setMessage(String message);

    public abstract void addExtra(ComponentProvider<?> other);

    public abstract void setColor(ColorProvider.ChatColor color);

    public abstract void setHover(ComponentHoverProvider<?> hover);

    public abstract void setClick(ComponentClickProvider<?> click);

    public T getComponent() {
        return component;
    }

    public static class Builder {
        public static Builder newBuilder(String text) {
            return new Builder(ComponentProvider.of(ColorProvider.of(text)));
        }

        public static Builder newBuilder(String text, ColorProvider.ChatColor color) {
            return new Builder(ComponentProvider.of(text, color));
        }

        private ComponentProvider<?> component;
        private Builder(ComponentProvider<?> component) {
            this.component = component;
        }

        public Builder text(String text) {
            this.component.setMessage(ColorProvider.of(text));
            return this;
        }

        public Builder color(ColorProvider.ChatColor color) {
            this.component.setColor(color);
            return this;
        }

        public Builder append(String text) {
            return append(text, null);
        }

        public Builder append(String text, ColorProvider.ChatColor color) {
            ComponentProvider<?> c = ComponentProvider.of(ColorProvider.of(text));
            if (color != null) {
                c.setColor(color);
            }
            return append(c);
        }

        public Builder append(ComponentProvider<?> component) {
            this.component.addExtra(component);
            return this;
        }

        public Builder hoverEvent(ComponentHoverProvider.Action action, ComponentProvider<?> component) {
            component.setHover(ComponentHoverProvider.of(action, component));
            return this;
        }

        public Builder hoverEvent(ComponentHoverProvider.Action action, String text, ColorProvider.ChatColor color) {
            ComponentProvider<?> c = ComponentProvider.of(ColorProvider.of(text));
            if (color != null) {
                c.setColor(color);
            }
            return hoverEvent(action, c);
        }

        public Builder clickEvent(ComponentClickProvider.Action action, String text) {
            component.setClick(ComponentClickProvider.of(action, text));
            return this;
        }

        public ComponentProvider<?> build() {
            return component;
        }
    }
}
