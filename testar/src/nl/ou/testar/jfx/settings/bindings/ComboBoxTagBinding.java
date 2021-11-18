package nl.ou.testar.jfx.settings.bindings;

import javafx.scene.control.ComboBox;
import org.fruit.alayer.Tag;
import org.fruit.monkey.Settings;

public class ComboBoxTagBinding<T> extends ConfigTagBinding<ComboBox<T>, T> {
    public ComboBoxTagBinding(Settings settings, ComboBox<T> control, Tag<T> tag) {
        super(settings, control, tag);
    }

    @Override
    public void onBind() {
        control.setValue(value);
        control.valueProperty().addListener((observable, oldValue, newValue) -> {
            value = newValue;
        });
    }
}
