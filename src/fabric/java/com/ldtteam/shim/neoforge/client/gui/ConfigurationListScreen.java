package com.ldtteam.shim.neoforge.client.gui;

import com.ldtteam.shim.neoforge.common.ModConfigSpec.ListValueSpec;
import com.ldtteam.shim.neoforge.common.ModConfigSpec.Range;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Edits a list value of a config: one text box per element (red while invalid), a remove button per element and an add button. Done hands
 * the edited list back to the section screen (which saves it with the other values). Lists whose element type can't be typed as text
 * (or a read-only config) are shown without editing.
 */
public class ConfigurationListScreen extends Screen
{
    private static final int ROW_WIDTH   = 310;
    private static final int TEXT_COLOR  = 0xFFE0E0E0;
    private static final int ERROR_COLOR = 0xFFFF5555;

    private final Screen                   parent;
    private final ListValueSpec            listSpec;
    private final Consumer<List<Object>>   onDone;
    private final boolean                  editable;
    @Nullable
    private final Function<String, Object> parser;
    private final List<ElementEntry>       elements = new ArrayList<>();
    private final HeaderAndFooterLayout    layout   = new HeaderAndFooterLayout(this, 9 + 4 + 9 + 8, 33);
    @Nullable
    private       ElementList              list;
    @Nullable
    private       Button                   doneButton;
    @Nullable
    private       Button                   addButton;

    public ConfigurationListScreen(final Screen parent, final Component name, final ListValueSpec listSpec, final List<Object> current, final boolean editable,
        final Consumer<List<Object>> onDone)
    {
        super(name);
        this.parent = parent;
        this.listSpec = listSpec;
        this.onDone = onDone;
        this.parser = ConfigurationSectionScreen.parserFor(elementType(listSpec, current));
        this.editable = editable && parser != null;
        for (final Object element : current)
        {
            elements.add(new ElementEntry(String.valueOf(element)));
        }
    }

    @Nullable
    private static Class<?> elementType(final ListValueSpec listSpec, final List<Object> current)
    {
        final Object def = listSpec.getDefault();
        if (def instanceof final List<?> defList && !defList.isEmpty() && defList.get(0) != null)
        {
            return defList.get(0).getClass();
        }
        final Supplier<?> supplier = listSpec.getNewElementSupplier();
        final Object sample = supplier != null ? supplier.get() : null;
        if (sample != null)
        {
            return sample.getClass();
        }
        return current.isEmpty() || current.get(0) == null ? String.class : current.get(0).getClass();
    }

    @Override
    protected void init()
    {
        final LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(title, font));
        final Component subtitle = editable
            ? Component.translatableWithFallback("minecolonies_shim.configuration.list.hint", "One entry per row").withStyle(ChatFormatting.GRAY)
            : Component.translatableWithFallback("minecolonies_shim.configuration.list.readonly", "Read-only").withStyle(ChatFormatting.GOLD);
        header.addChild(new StringWidget(subtitle, font));

        list = layout.addToContents(new ElementList());
        list.fill();

        final LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        if (editable)
        {
            addButton = footer.addChild(Button.builder(Component.translatableWithFallback("minecolonies_shim.configuration.list.add", "Add entry"), b -> addElement())
                .width(100).build());
            doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> done()).width(100).build());
            footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).width(100).build());
        }
        else
        {
            footer.addChild(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).width(200).build());
        }
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        update();
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

    private void addElement()
    {
        final Supplier<?> supplier = listSpec.getNewElementSupplier();
        final Object sample = supplier != null ? supplier.get() : null;
        elements.add(new ElementEntry(sample == null ? "" : String.valueOf(sample)));
        if (list != null)
        {
            list.fill();
            list.setScrollAmount(list.maxScrollAmount());
        }
        update();
    }

    private void removeElement(final ElementEntry entry)
    {
        elements.remove(entry);
        if (list != null)
        {
            list.fill();
        }
        update();
    }

    @Nullable
    private List<Object> parsed()
    {
        final List<Object> out = new ArrayList<>();
        for (final ElementEntry entry : elements)
        {
            if (entry.parsed == null)
            {
                return null;
            }
            out.add(entry.parsed);
        }
        return out;
    }

    private void update()
    {
        final Range<Integer> size = listSpec.getSizeRange();
        if (addButton != null)
        {
            addButton.active = listSpec.getNewElementSupplier() != null && elements.size() < size.getMax();
        }
        if (doneButton != null)
        {
            final List<Object> parsed = parsed();
            doneButton.active = parsed != null && size.test(parsed.size()) && listSpec.test(parsed);
        }
    }

    private void done()
    {
        final List<Object> parsed = parsed();
        if (parsed != null && listSpec.test(parsed))
        {
            onDone.accept(parsed);
            onClose();
        }
    }

    @Override
    public void onClose()
    {
        minecraft.gui.setScreen(parent);
    }

    private final class ElementEntry extends ContainerObjectSelectionList.Entry<ElementEntry>
    {
        private final EditBox box;
        private final Button  remove;
        @Nullable
        private       Object  parsed;

        private ElementEntry(final String text)
        {
            box = new EditBox(Minecraft.getInstance().font, 0, 0, ROW_WIDTH - 24, 20, Component.empty());
            box.setMaxLength(32767);
            box.setValue(text);
            box.moveCursorToStart(false);
            box.setEditable(editable);
            box.setResponder(this::onChanged);
            remove = Button.builder(Component.literal("-"), b -> removeElement(this)).size(20, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatableWithFallback("minecolonies_shim.configuration.list.remove", "Remove entry")))
                .build();
            remove.active = editable;
            onChanged(text);
        }

        private void onChanged(final String text)
        {
            Object value;
            try
            {
                value = parser == null ? null : parser.apply(text);
            }
            catch (final RuntimeException e)
            {
                value = null;
            }
            parsed = value != null && listSpec.testElement(value) ? value : null;
            box.setTextColor(parsed != null ? TEXT_COLOR : ERROR_COLOR);
            update();
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a)
        {
            box.setPosition(getContentX(), getContentY());
            box.extractRenderState(graphics, mouseX, mouseY, a);
            remove.setPosition(getContentRight() - 20, getContentY());
            remove.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(box, remove);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of(box, remove);
        }
    }

    private final class ElementList extends ContainerObjectSelectionList<ElementEntry>
    {
        private ElementList()
        {
            super(Minecraft.getInstance(), ConfigurationListScreen.this.width, layout.getContentHeight(), layout.getHeaderHeight(), 24);
            this.centerListVertically = false;
        }

        private void fill()
        {
            replaceEntries(elements);
        }

        @Override
        public int getRowWidth()
        {
            return ROW_WIDTH;
        }
    }
}
