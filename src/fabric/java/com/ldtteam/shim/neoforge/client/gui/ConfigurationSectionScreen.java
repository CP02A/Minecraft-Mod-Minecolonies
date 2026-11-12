package com.ldtteam.shim.neoforge.client.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.ldtteam.shim.fml.config.ModConfig;
import com.ldtteam.shim.neoforge.common.ModConfigSpec;
import com.ldtteam.shim.neoforge.common.ModConfigSpec.ConfigValue;
import com.ldtteam.shim.neoforge.common.ModConfigSpec.ListValueSpec;
import com.ldtteam.shim.neoforge.common.ModConfigSpec.RestartType;
import com.ldtteam.shim.neoforge.common.ModConfigSpec.ValueSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Edits the values of one config file (vanilla widgets, laid out like the game rules screen): on/off and enum values as cycle buttons,
 * numbers and strings as text boxes (red while invalid), lists in a {@link ConfigurationListScreen}. Done writes the changed values into
 * the loaded config and saves it to its file (the same file the shim loads), which fires the config reloading event like an edit of the
 * file. Values the spec marks as needing a restart only take effect after it; the screen says so.
 * <p>
 * Read-only when the config can't be saved here: a server config outside a singleplayer world (main menu: default values; on a server:
 * the values the server sent).
 */
public class ConfigurationSectionScreen extends Screen
{
    private static final int ROW_WIDTH    = 340;
    private static final int WIDGET_WIDTH = 110;
    private static final int RESET_WIDTH  = 40;
    private static final int TEXT_COLOR   = 0xFFE0E0E0;
    private static final int ERROR_COLOR  = 0xFFFF5555;

    /**
     * Whether a config can be changed in this screen.
     */
    public enum Access
    {
        EDITABLE("minecolonies_shim.configuration.access.editable", "Changes are saved to the config file."),
        NO_WORLD("minecolonies_shim.configuration.access.no_world",
            "Read-only: default values. Open a singleplayer world to change them."),
        REMOTE_SERVER("minecolonies_shim.configuration.access.remote",
            "Read-only: the server's values. Change them on the server."),
        NOT_LOADED("minecolonies_shim.configuration.access.not_loaded", "Read-only: this config is not loaded right now.");

        private final Component description;

        Access(final String key, final String fallback)
        {
            this.description = Component.translatableWithFallback(key, fallback);
        }

        public Component description()
        {
            return description;
        }
    }

    /**
     * @return whether the config can be changed now: client and common configs always (they have a file), server configs only in a
     * singleplayer world (the integrated server loaded them from the world's serverconfig folder).
     */
    public static Access access(final Minecraft minecraft, final ModConfig config)
    {
        if (config.getLoadedConfig() == null)
        {
            return Access.NOT_LOADED;
        }
        if (config.getType() == ModConfig.Type.SERVER)
        {
            if (!minecraft.hasSingleplayerServer())
            {
                return minecraft.level == null && minecraft.getConnection() == null ? Access.NO_WORLD : Access.REMOTE_SERVER;
            }
        }
        return config.getFullPath() != null ? Access.EDITABLE : Access.NOT_LOADED;
    }

    private final Screen                 parent;
    private final ModConfig              config;
    private final ModConfigSpec          spec;
    private final Access                 access;
    private final HeaderAndFooterLayout  layout;
    private final List<ValueEntry>       valueEntries = new ArrayList<>();
    private final List<Row>            allEntries   = new ArrayList<>();
    @Nullable
    private       ValueList              list;
    @Nullable
    private       EditBox                searchBox;
    @Nullable
    private       Button                 doneButton;
    private       String                 filter       = "";

    public ConfigurationSectionScreen(final Screen parent, final ModConfig config, final String modName)
    {
        super(Component.translatableWithFallback("minecolonies_shim.configuration.section_title", "%s: %s", modName,
            ConfigurationScreen.typeName(config.getType())));
        this.parent = parent;
        this.config = config;
        if (!(config.getSpec() instanceof final ModConfigSpec modSpec))
        {
            throw new IllegalArgumentException("Config " + config.getFileName() + " has no ModConfigSpec");
        }
        this.spec = modSpec;
        this.access = access(Minecraft.getInstance(), config);
        this.layout = new HeaderAndFooterLayout(this, 9 + 4 + 9 + 4 + 15 + 8, 33);
    }

    public ModConfig config()
    {
        return config;
    }

    public boolean isEditable()
    {
        return access == Access.EDITABLE;
    }

    public Access access()
    {
        return access;
    }

    @Override
    protected void init()
    {
        if (allEntries.isEmpty())
        {
            buildEntries(spec.getValues(), new ArrayList<>());
        }

        final LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(title, font));
        final Component subtitle = access == Access.EDITABLE
            ? Component.literal(config.getFileName()).withStyle(ChatFormatting.GRAY)
            : access.description().copy().withStyle(ChatFormatting.GOLD);
        header.addChild(new StringWidget(Math.min(width - 20, font.width(subtitle)), 9, subtitle, font));
        searchBox = header.addChild(new EditBox(font, 200, 15, Component.empty()));
        searchBox.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE));
        searchBox.setValue(filter);
        searchBox.setResponder(this::setFilter);

        list = layout.addToContents(new ValueList());
        list.fill();

        final LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        if (isEditable())
        {
            doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose()).build());
            footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).build());
        }
        else
        {
            doneButton = footer.addChild(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).width(200).build());
        }
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        updateDoneButton();
    }

    @Override
    protected void repositionElements()
    {
        layout.arrangeElements();
        if (list != null)
        {
            list.updateSize(width, layout);
        }
    }

    @Override
    protected void setInitialFocus()
    {
        if (searchBox != null)
        {
            setInitialFocus(searchBox);
        }
    }

    private void setFilter(final String filter)
    {
        this.filter = filter;
        if (list != null)
        {
            list.fill();
            list.setScrollAmount(0);
            repositionElements();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Building the entries
    // ------------------------------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void buildEntries(final UnmodifiableConfig values, final List<String> path)
    {
        // Plain values of this level first, then the sub categories (each with its header).
        for (final Map.Entry<String, Object> e : values.valueMap().entrySet())
        {
            if (e.getValue() instanceof final ConfigValue<?> value)
            {
                final ValueEntry entry = new ValueEntry((ConfigValue<Object>) value);
                valueEntries.add(entry);
                allEntries.add(entry);
            }
        }
        for (final Map.Entry<String, Object> e : values.valueMap().entrySet())
        {
            if (e.getValue() instanceof final UnmodifiableConfig sub)
            {
                final List<String> subPath = new ArrayList<>(path);
                subPath.add(e.getKey());
                allEntries.add(new CategoryEntry(subPath));
                buildEntries(sub, subPath);
            }
        }
    }

    private static Component translatedOr(@Nullable final String key, final String fallback)
    {
        return key == null ? Component.literal(fallback) : Component.translatableWithFallback(key, fallback);
    }

    private Component categoryName(final List<String> path)
    {
        Component name = Component.empty();
        for (int i = 1; i <= path.size(); i++)
        {
            final List<String> sub = path.subList(0, i);
            final Component part = translatedOr(spec.getLevelTranslationKey(sub), sub.get(i - 1));
            name = i == 1 ? part.copy() : name.copy().append(" > ").append(part);
        }
        return name;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------------------------------------------------

    private void updateDoneButton()
    {
        if (doneButton != null && isEditable())
        {
            doneButton.active = valueEntries.stream().allMatch(e -> e.editor.valid());
        }
    }

    /**
     * @return whether every value is valid (Done is clickable).
     */
    public boolean canSave()
    {
        return isEditable() && valueEntries.stream().allMatch(e -> e.editor.valid());
    }

    /**
     * Writes the changed values into the loaded config, saves it to its file and goes back. Does nothing while a value is invalid or if
     * the screen is read-only (then it just goes back).
     */
    public void saveAndClose()
    {
        if (!isEditable())
        {
            onClose();
            return;
        }
        if (!canSave())
        {
            return;
        }
        final List<ValueEntry> changed = valueEntries.stream().filter(ValueEntry::changed).toList();
        if (changed.isEmpty())
        {
            onClose();
            return;
        }
        for (final ValueEntry entry : changed)
        {
            entry.value.set(entry.editor.value());
        }
        spec.save();

        final RestartType restart = changed.stream().map(e -> e.valueSpec.restartType()).reduce(RestartType.NONE, RestartType::with);
        if (restart == RestartType.NONE)
        {
            onClose();
            return;
        }
        final Component message = restart == RestartType.GAME
            ? Component.translatableWithFallback("minecolonies_shim.configuration.restart.game", "Some of the changed settings take effect after restarting the game.")
            : Component.translatableWithFallback("minecolonies_shim.configuration.restart.world", "Some of the changed settings take effect after reloading the world.");
        minecraft.gui.setScreen(new AlertScreen(this::onClose,
            Component.translatableWithFallback("minecolonies_shim.configuration.restart.title", "Restart needed"), message));
    }

    @Override
    public void onClose()
    {
        minecraft.gui.setScreen(parent);
    }

    /**
     * @return the widget that edits the value at this path (dot separated, e.g. {@code gameplay.enablecitizenvoices}), or null.
     */
    @Nullable
    public AbstractWidget widgetFor(final String path)
    {
        final List<String> wanted = Arrays.asList(path.split("\\."));
        for (final ValueEntry entry : valueEntries)
        {
            if (entry.value.getPath().equals(wanted))
            {
                return entry.editor.widget();
            }
        }
        return null;
    }

    /**
     * Scrolls the list so the row of this value is visible.
     */
    public void scrollTo(final String path)
    {
        final List<String> wanted = Arrays.asList(path.split("\\."));
        if (list == null)
        {
            return;
        }
        for (final ValueEntry entry : valueEntries)
        {
            if (entry.value.getPath().equals(wanted) && list.children().contains(entry))
            {
                list.scrollToEntry(entry);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Editors
    // ------------------------------------------------------------------------------------------------------------------------------------

    private interface Editor
    {
        AbstractWidget widget();

        Object value();

        boolean valid();

        void show(Object value);
    }

    private record CycleEditor<T>(CycleButton<T> widget) implements Editor
    {
        @Override
        public Object value()
        {
            return widget.getValue();
        }

        @Override
        public boolean valid()
        {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void show(final Object value)
        {
            widget.setValue((T) value);
        }
    }

    private final class TextEditor implements Editor
    {
        private final EditBox                  box;
        private final Function<String, Object> parser;
        private final ValueSpec                valueSpec;
        @Nullable
        private       Object                   parsed;

        private TextEditor(final Component name, final Object current, final Function<String, Object> parser, final ValueSpec valueSpec)
        {
            this.parser = parser;
            this.valueSpec = valueSpec;
            this.box = new EditBox(font, 0, 0, WIDGET_WIDTH, 20, name);
            box.setMaxLength(32767);
            box.setValue(String.valueOf(current));
            box.moveCursorToStart(false);
            box.setResponder(this::onChanged);
            box.setEditable(isEditable());
            onChanged(box.getValue());
        }

        private void onChanged(final String text)
        {
            Object value;
            try
            {
                value = parser.apply(text);
            }
            catch (final RuntimeException e)
            {
                value = null;
            }
            parsed = value != null && valueSpec.test(value) ? value : null;
            box.setTextColor(parsed != null ? TEXT_COLOR : ERROR_COLOR);
            updateDoneButton();
        }

        @Override
        public AbstractWidget widget()
        {
            return box;
        }

        @Override
        public Object value()
        {
            return parsed;
        }

        @Override
        public boolean valid()
        {
            return parsed != null;
        }

        @Override
        public void show(final Object value)
        {
            box.setValue(String.valueOf(value));
            box.moveCursorToStart(false);
        }
    }

    private final class ListEditor implements Editor
    {
        private final Button       button;
        private final ListValueSpec listSpec;
        private final Component    name;
        private       List<Object> current;

        private ListEditor(final Component name, final List<?> current, final ListValueSpec listSpec)
        {
            this.name = name;
            this.listSpec = listSpec;
            this.current = new ArrayList<>(current);
            this.button = Button.builder(Component.empty(), b -> open()).size(WIDGET_WIDTH, 20).build();
            updateMessage();
        }

        private void open()
        {
            minecraft.gui.setScreen(new ConfigurationListScreen(ConfigurationSectionScreen.this, name, listSpec, current, isEditable(), edited -> {
                current = new ArrayList<>(edited);
                updateMessage();
                updateDoneButton();
            }));
        }

        private void updateMessage()
        {
            button.setMessage(Component.translatableWithFallback("minecolonies_shim.configuration.list.button", "Edit list (%s)", current.size()));
        }

        @Override
        public AbstractWidget widget()
        {
            return button;
        }

        @Override
        public Object value()
        {
            return List.copyOf(current);
        }

        @Override
        public boolean valid()
        {
            return listSpec.test(current);
        }

        @Override
        public void show(final Object value)
        {
            current = new ArrayList<>((List<?>) value);
            updateMessage();
        }
    }

    private record ReadOnlyEditor(Button widget, Object value) implements Editor
    {
        @Override
        public boolean valid()
        {
            return true;
        }

        @Override
        public void show(final Object value)
        {
        }
    }

    /**
     * @return a parser for text typed for a value of this class, or null if the class can't be edited as text.
     */
    @Nullable
    static Function<String, Object> parserFor(@Nullable final Class<?> type)
    {
        if (type == Integer.class)
        {
            return s -> Integer.parseInt(s.trim());
        }
        if (type == Long.class)
        {
            return s -> Long.parseLong(s.trim());
        }
        if (type == Double.class)
        {
            return s -> Double.parseDouble(s.trim());
        }
        if (type == Float.class)
        {
            return s -> Float.parseFloat(s.trim());
        }
        if (type == String.class)
        {
            return s -> s;
        }
        if (type == Boolean.class)
        {
            return s -> switch (s.trim().toLowerCase(Locale.ROOT))
            {
                case "true" -> Boolean.TRUE;
                case "false" -> Boolean.FALSE;
                default -> null;
            };
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Editor createEditor(final Component name, final ConfigValue<Object> value, final ValueSpec valueSpec, final Object current)
    {
        final Object def = value.getDefault();
        if (valueSpec instanceof final ListValueSpec listSpec && current instanceof final List<?> currentList)
        {
            return new ListEditor(name, currentList, listSpec);
        }
        if (current instanceof Boolean)
        {
            final CycleButton<Boolean> button = CycleButton.onOffBuilder((Boolean) current).displayOnlyValue()
                .create(0, 0, WIDGET_WIDTH, 20, name, (b, v) -> {});
            button.active = isEditable();
            return new CycleEditor<>(button);
        }
        if (current instanceof final Enum<?> enumValue)
        {
            final List<Enum> allowed = new ArrayList<>();
            for (final Enum constant : enumValue.getDeclaringClass().getEnumConstants())
            {
                if (valueSpec.test(constant) || valueSpec.test(constant.name()))
                {
                    allowed.add(constant);
                }
            }
            if (!allowed.contains(enumValue))
            {
                allowed.add(enumValue);
            }
            final CycleButton<Enum> button = CycleButton.<Enum>builder(e -> Component.literal(e.name()), enumValue).withValues(allowed).displayOnlyValue()
                .create(0, 0, WIDGET_WIDTH, 20, name, (b, v) -> {});
            button.active = isEditable();
            return new CycleEditor<>(button);
        }
        final Function<String, Object> parser = parserFor(def != null ? def.getClass() : current.getClass());
        if (parser != null)
        {
            return new TextEditor(name, current, parser, valueSpec);
        }
        final Button button = Button.builder(Component.literal(String.valueOf(current)), b -> {}).size(WIDGET_WIDTH, 20).build();
        button.active = false;
        return new ReadOnlyEditor(button, current);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // List and rows
    // ------------------------------------------------------------------------------------------------------------------------------------

    abstract static class Row extends ContainerObjectSelectionList.Entry<Row>
    {
        @Nullable
        abstract List<FormattedCharSequence> tooltip();
    }

    private final class CategoryEntry extends Row
    {
        private final List<String>        path;
        private final Component           name;
        private final FocusableTextWidget widget;
        @Nullable
        private final List<FormattedCharSequence> tooltip;

        private CategoryEntry(final List<String> path)
        {
            this.path = List.copyOf(path);
            this.name = categoryName(path);
            this.widget = FocusableTextWidget.builder(name.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), font)
                .alwaysShowBorder(false)
                .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
                .build();
            final String comment = spec.getLevelComment(path);
            this.tooltip = comment == null || comment.isBlank() ? null : font.split(Component.literal(comment), 200);
        }

        @Override
        List<FormattedCharSequence> tooltip()
        {
            return tooltip;
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a)
        {
            widget.setPosition(getContentXMiddle() - widget.getWidth() / 2, getContentYMiddle() - widget.getHeight() / 2);
            widget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of(widget);
        }
    }

    private final class ValueEntry extends Row
    {
        private final ConfigValue<Object>         value;
        private final ValueSpec                   valueSpec;
        private final Component                   name;
        private final List<FormattedCharSequence> label;
        private final List<FormattedCharSequence> tooltip;
        private final Object                      initial;
        private final Editor                      editor;
        private final Button                      reset;
        private final String                      searchText;

        private ValueEntry(final ConfigValue<Object> value)
        {
            this.value = value;
            this.valueSpec = value.getSpec();
            final List<String> path = value.getPath();
            final String key = path.get(path.size() - 1);
            this.name = translatedOr(valueSpec.getTranslationKey(), key);
            this.label = font.split(name, ROW_WIDTH - WIDGET_WIDTH - RESET_WIDTH - 12);

            final List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.literal(String.join(".", path)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            final String comment = valueSpec.getComment();
            if (comment != null && !comment.isBlank())
            {
                lines.addAll(font.split(Component.literal(comment), 220));
            }
            if (valueSpec.restartType() == RestartType.WORLD)
            {
                lines.add(Component.translatableWithFallback("minecolonies_shim.configuration.needs_world_restart", "Takes effect after reloading the world")
                    .withStyle(ChatFormatting.RED).getVisualOrderText());
            }
            else if (valueSpec.restartType() == RestartType.GAME)
            {
                lines.add(Component.translatableWithFallback("minecolonies_shim.configuration.needs_game_restart", "Takes effect after restarting the game")
                    .withStyle(ChatFormatting.RED).getVisualOrderText());
            }
            this.tooltip = lines;
            this.searchText = (String.join(".", path) + " " + name.getString() + " " + Objects.requireNonNullElse(comment, "")).toLowerCase(Locale.ROOT);

            this.initial = value.getRaw();
            this.editor = createEditor(name, value, valueSpec, initial);
            this.reset = Button.builder(Component.translatable("controls.reset"), b -> {
                editor.show(value.getDefault());
                updateDoneButton();
            }).size(RESET_WIDTH, 20).build();
            this.reset.active = isEditable() && !(editor instanceof ReadOnlyEditor);
        }

        private boolean changed()
        {
            final Object now = editor.value();
            return !(editor instanceof ReadOnlyEditor) && now != null && !Objects.equals(now, initial);
        }

        @Override
        List<FormattedCharSequence> tooltip()
        {
            return tooltip;
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a)
        {
            final int top = getContentY();
            final int left = getContentX();
            final int color = changed() ? 0xFFFFFF55 : 0xFFFFFFFF;
            if (label.size() == 1)
            {
                graphics.text(font, label.get(0), left, top + 6, color);
            }
            else if (label.size() >= 2)
            {
                graphics.text(font, label.get(0), left, top + 1, color);
                graphics.text(font, label.get(1), left, top + 11, color);
            }
            reset.setPosition(getContentRight() - RESET_WIDTH, top);
            editor.widget().setPosition(getContentRight() - RESET_WIDTH - 4 - WIDGET_WIDTH, top);
            editor.widget().extractRenderState(graphics, mouseX, mouseY, a);
            reset.active = isEditable() && !(editor instanceof ReadOnlyEditor) && !Objects.equals(editor.value(), value.getDefault());
            reset.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(editor.widget(), reset);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of(editor.widget(), reset);
        }
    }

    private final class ValueList extends ContainerObjectSelectionList<Row>
    {
        private ValueList()
        {
            super(Minecraft.getInstance(), ConfigurationSectionScreen.this.width, layout.getContentHeight(), layout.getHeaderHeight(), 24);
            this.centerListVertically = false;
        }

        private void fill()
        {
            clearEntries();
            final String lower = filter.toLowerCase(Locale.ROOT).trim();
            CategoryEntry pendingHeader = null;
            for (final Row entry : allEntries)
            {
                if (entry instanceof final CategoryEntry category)
                {
                    pendingHeader = category;
                }
                else if (entry instanceof final ValueEntry value && (lower.isEmpty() || value.searchText.contains(lower)))
                {
                    if (pendingHeader != null)
                    {
                        addEntry(pendingHeader);
                        pendingHeader = null;
                    }
                    addEntry(value);
                }
            }
        }

        @Override
        public int getRowWidth()
        {
            return ROW_WIDTH;
        }

        @Override
        public void scrollToEntry(final Row entry)
        {
            super.scrollToEntry(entry);
        }

        @Override
        public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a)
        {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
            final Row hovered = getHovered();
            if (hovered != null && hovered.tooltip() != null)
            {
                graphics.setTooltipForNextFrame(hovered.tooltip(), mouseX, mouseY);
            }
        }
    }
}
