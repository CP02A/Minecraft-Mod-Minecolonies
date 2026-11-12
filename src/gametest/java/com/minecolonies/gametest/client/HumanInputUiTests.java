package com.minecolonies.gametest.client;

import com.minecolonies.gametest.framework.E2EMode;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.SDLEventHandler;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.Main;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.minecolonies.gametest.client.UiTestSupport.assertThat;
import static com.minecolonies.gametest.client.UiTestSupport.closeScreen;
import static com.minecolonies.gametest.client.UiTestSupport.screenshot;

/**
 * UI-08 (docs/e2e-tests/ui.md): a person at the computer can't affect a client game test run (docs/e2e-tests/framework.md, "No human
 * input"). Real input is simulated by pushing SDL events into the window's event queue, the same queue the operating system's keyboard,
 * mouse and window events arrive in; the real window is resized, maximized and minimized through SDL. Nothing here touches the person's
 * real mouse or keyboard.
 */
final class HumanInputUiTests
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HumanInputUiTests.class);

    private static final String OWN ="com.minecolonies.gametest.mixin.";
    private static final String FABRIC = "net.fabricmc.fabric.mixin.client.gametest.";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";

    /**
     * SDL event types (SDL3 {@code SDL_EventType}).
     */
    private static final int KEY_DOWN               = 0x300;
    private static final int KEY_UP                 = 0x301;
    private static final int TEXT_EDITING           = 0x302;
    private static final int TEXT_INPUT             = 0x303;
    private static final int MOUSE_MOTION           = 0x400;
    private static final int MOUSE_BUTTON_DOWN      = 0x401;
    private static final int MOUSE_BUTTON_UP        = 0x402;
    private static final int MOUSE_WHEEL            = 0x403;
    private static final int DROP_FILE              = 0x1000;
    private static final int DROP_BEGIN             = 0x1002;
    private static final int DROP_COMPLETE          = 0x1003;
    private static final int WINDOW_MINIMIZED       = 0x209;
    private static final int WINDOW_RESTORED        = 0x20B;
    private static final int WINDOW_FOCUS_GAINED    = 0x20E;
    private static final int WINDOW_FOCUS_LOST      = 0x20F;
    private static final int WINDOW_ENTER_FULLSCREEN = 0x217;
    private static final int DISPLAY_MODE_CHANGED   = 0x156;

    /**
     * SDL scancodes and key codes.
     */
    private static final int SCANCODE_E      = 8;
    private static final int SCANCODE_W      = 26;
    private static final int SCANCODE_ESCAPE = 41;
    private static final int KEY_E           = 'e';
    private static final int KEY_W           = 'w';
    private static final int KEY_ESCAPE      = 0x1B;

    /**
     * Longer than Minecraft's 500 ms grace time before "Pause on Lost Focus" pauses the game.
     */
    private static final long FOCUS_LOSS_WAIT_MS = 1500;

    private HumanInputUiTests()
    {
    }

    /**
     * Runs all UI-08 checks. Needs a world; leaves no screen open.
     */
    static void run(final ClientGameTestContext context, final TestServerContext server, final List<Path> shots)
    {
        assertThat(E2EMode.isClientTestRun(), "Not a client game test run (fabric.client.gametest not set)");
        checkWiring();
        closeScreen(context);
        final boolean pauseOnLostFocus = context.computeOnClient(mc -> mc.options.pauseOnLostFocus);
        context.runOnClient(mc -> mc.options.pauseOnLostFocus = true);
        try
        {
            inWorldInputAndFocus(context, server);
            screenInput(context);
            windowChanges(context, server, shots);
            cursorNotCaptured(context);
        }
        finally
        {
            context.runOnClient(mc -> {
                mc.options.pauseOnLostFocus = pauseOnLostFocus;
                final int id = SDLVideo.SDL_GetWindowID(mc.getWindow().handle());
                push(e -> e.window().set(WINDOW_FOCUS_GAINED, 0, id, 0, 0));
                push(e -> e.window().set(WINDOW_RESTORED, 0, id, 0, 0));
            });
            closeScreen(context);
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // Wiring: every blocker is applied (ours and Fabric's)
    // --------------------------------------------------------------------------------------------------------------------------------------

    private static void checkWiring()
    {
        // Every human input handler of SDLEventHandler is blocked by both. All handle*Event methods are input except the key map change,
        // which only refreshes the key binds screen. A new handler in a future Minecraft version fails here until it is looked at.
        final Set<String> inputHandlers = new TreeSet<>();
        for (final Method method : SDLEventHandler.class.getDeclaredMethods())
        {
            if (method.getName().startsWith("handle") && method.getName().endsWith("Event") && !method.getName().equals("handleKeymapChangedEvent")
                  && method.getAnnotation(MixinMerged.class) == null)
            {
                inputHandlers.add(method.getName());
            }
        }
        assertThat(inputHandlers.size() >= 9, "Expected at least 9 input handlers in SDLEventHandler, found " + inputHandlers);
        for (final String mixin : List.of(OWN + "SDLEventHandlerMixin", FABRIC + "input.SDLEventHandlerMixin"))
        {
            final Set<String> blocked = injectTargets(SDLEventHandler.class, mixin);
            final Set<String> missing = new TreeSet<>(inputHandlers);
            missing.removeAll(blocked);
            assertThat(missing.isEmpty(), mixin + " doesn't block these SDLEventHandler input handlers: " + missing + " (blocks " + blocked + ")");
        }

        assertApplied(Window.class, OWN + "WindowMixin", FABRIC + "input.WindowMixin");
        assertThat(injectTargets(Window.class, OWN + "WindowMixin").containsAll(List.of("onFocus", "onIconified", "handleEvent")),
          "WindowMixin doesn't cover onFocus, onIconified and handleEvent: " + injectTargets(Window.class, OWN + "WindowMixin"));
        assertThat(injectTargets(Window.class, FABRIC + "input.WindowMixin").containsAll(List.of("onFocus", "onIconified", "onResize", "onFramebufferResize")),
          "Fabric's WindowMixin no longer covers focus, minimize and resize: " + injectTargets(Window.class, FABRIC + "input.WindowMixin"));
        assertApplied(Minecraft.class, OWN + "MinecraftMixin");
        assertApplied(InputConstants.class, OWN + "InputConstantsMixin", FABRIC + "input.InputConstantsMixin");
        assertApplied(MouseHandler.class, OWN + "MouseHandlerMixin");
        assertApplied(Main.class, OWN + "MainMixin");
        assertThat(injectTargets(Window.class, OWN + "WindowMixin").contains("requestClose"), "WindowMixin doesn't watch requestClose (window closed by hand)");
    }

    private static Set<String> appliedMixins(final Class<?> target)
    {
        final Set<String> mixins = new HashSet<>();
        for (final Method method : target.getDeclaredMethods())
        {
            final MixinMerged merged = method.getAnnotation(MixinMerged.class);
            if (merged != null)
            {
                mixins.add(merged.mixin());
            }
        }
        return mixins;
    }

    private static void assertApplied(final Class<?> target, final String... mixins)
    {
        final Set<String> applied = appliedMixins(target);
        for (final String mixin : mixins)
        {
            assertThat(applied.contains(mixin), mixin + " is not applied to " + target.getSimpleName() + ". Applied: " + applied);
        }
    }

    /**
     * @return the target methods of the {@code @Inject} handlers of {@code mixin}, if it is applied to {@code target} (empty otherwise).
     * Mixin removes the annotations when it merges a handler into its target, so they are read from the mixin's class file.
     */
    private static Set<String> injectTargets(final Class<?> target, final String mixin)
    {
        final Set<String> methods = new TreeSet<>();
        if (!appliedMixins(target).contains(mixin))
        {
            return methods;
        }
        final ClassNode node = new ClassNode();
        try (InputStream in = HumanInputUiTests.class.getClassLoader().getResourceAsStream(mixin.replace('.', '/') + ".class"))
        {
            if (in == null)
            {
                throw new AssertionError("Class file of " + mixin + " not found");
            }
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
        }
        catch (final IOException e)
        {
            throw new AssertionError("Can't read the class file of " + mixin, e);
        }
        for (final MethodNode method : node.methods)
        {
            final List<AnnotationNode> annotations = new ArrayList<>();
            if (method.visibleAnnotations != null)
            {
                annotations.addAll(method.visibleAnnotations);
            }
            if (method.invisibleAnnotations != null)
            {
                annotations.addAll(method.invisibleAnnotations);
            }
            for (final AnnotationNode annotation : annotations)
            {
                if (!annotation.desc.equals(INJECT) || annotation.values == null)
                {
                    continue;
                }
                for (int i = 0; i + 1 < annotation.values.size(); i += 2)
                {
                    if ("method".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof final List<?> names)
                    {
                        for (final Object name : names)
                        {
                            final String text = String.valueOf(name);
                            final int descriptor = text.indexOf('(');
                            methods.add(descriptor < 0 ? text : text.substring(0, descriptor));
                        }
                    }
                }
            }
        }
        return methods;
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // In the world: keys, mouse, focus loss, minimize
    // --------------------------------------------------------------------------------------------------------------------------------------

    private record WorldState(Vec3 pos, float yRot, float xRot, int slot, int width, int height, boolean leftPressed, boolean upDown,
                              boolean attackDown, boolean useDown)
    {
        static WorldState of(final Minecraft mc)
        {
            return new WorldState(mc.player.position(), mc.player.getYRot(), mc.player.getXRot(), mc.player.getInventory().getSelectedSlot(),
              mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.mouseHandler.isLeftPressed(), mc.options.keyUp.isDown(),
              mc.options.keyAttack.isDown(), mc.options.keyUse.isDown());
        }
    }

    /**
     * Real key presses (W, E, Escape), mouse buttons, mouse movement and the wheel, focus loss, minimizing and a display mode change arrive
     * while no screen is open: the player doesn't move or turn, no inventory or pause menu opens, the hotbar slot stays, the window keeps
     * its size, and neither the client nor the integrated server pauses, even with "Pause on Lost Focus" on and well past its 500 ms.
     */
    private static void inWorldInputAndFocus(final ClientGameTestContext context, final TestServerContext server)
    {
        final WorldState before = context.computeOnClient(WorldState::of);
        final long serverTimeBefore = server.computeOnServer(s -> s.overworld().getGameTime());
        final boolean fullscreenBefore = context.computeOnClient(mc -> mc.options.fullscreen().get());
        context.runOnClient(mc -> {
            final int id = SDLVideo.SDL_GetWindowID(mc.getWindow().handle());
            push(e -> e.key().set(KEY_DOWN, 0, id, 0, SCANCODE_W, KEY_W, (short) 0, (short) 0, true, false));
            push(e -> e.key().set(KEY_DOWN, 0, id, 0, SCANCODE_E, KEY_E, (short) 0, (short) 0, true, false));
            push(e -> e.key().set(KEY_DOWN, 0, id, 0, SCANCODE_ESCAPE, KEY_ESCAPE, (short) 0, (short) 0, true, false));
            push(e -> e.button().set(MOUSE_BUTTON_DOWN, 0, id, 0, (byte) 1, true, (byte) 1, 100, 100));
            push(e -> e.button().set(MOUSE_BUTTON_DOWN, 0, id, 0, (byte) 3, true, (byte) 1, 100, 100));
            push(e -> e.motion().set(MOUSE_MOTION, 0, id, 0, 0, 300, 200, 250, 120));
            push(e -> e.wheel().set(MOUSE_WHEEL, 0, id, 0, 0, -3, 0, 300, 200, 0, -3));
            push(e -> e.window().set(WINDOW_FOCUS_LOST, 0, id, 0, 0));
            push(e -> e.window().set(WINDOW_MINIMIZED, 0, id, 0, 0));
            push(e -> e.window().set(WINDOW_ENTER_FULLSCREEN, 0, id, 0, 0));
            push(e -> e.display().set(DISPLAY_MODE_CHANGED, 0, SDLVideo.SDL_GetPrimaryDisplay(), 0, 0));
        });
        waitMillis(context, FOCUS_LOSS_WAIT_MS);

        final WorldState after = context.computeOnClient(WorldState::of);
        final String screen = context.computeOnClient(mc -> String.valueOf(mc.gui.screen()));
        assertThat(screen.equals("null"), "A screen opened after real key presses or focus loss: " + screen);
        assertThat(!context.computeOnClient(Minecraft::isPaused), "The client is paused after real input or focus loss");
        assertThat(!server.computeOnServer(s -> s.isPaused()), "The integrated server is paused after real input or focus loss");
        assertThat(server.computeOnServer(s -> s.overworld().getGameTime()) > serverTimeBefore, "The integrated server stopped ticking");
        assertThat(context.computeOnClient(mc -> mc.getWindow().isFocused()), "The window counts as not focused after a focus loss event");
        assertThat(!context.computeOnClient(mc -> mc.getWindow().isIconified()), "The window counts as minimized after a minimize event");
        assertThat(context.computeOnClient(mc -> mc.options.fullscreen().get()) == fullscreenBefore,
          "The fullscreen option changed after a fullscreen event from the window system");
        assertThat(after.pos().distanceTo(before.pos()) < 0.01, "The player moved: " + before.pos() + " -> " + after.pos());
        assertThat(after.yRot() == before.yRot() && after.xRot() == before.xRot(),
          "The camera turned: " + before.yRot() + "/" + before.xRot() + " -> " + after.yRot() + "/" + after.xRot());
        assertThat(after.slot() == before.slot(), "The hotbar slot changed: " + before.slot() + " -> " + after.slot());
        assertThat(!after.leftPressed() && !after.upDown() && !after.attackDown() && !after.useDown(),
          "A key or button counts as held: " + after);
        assertThat(after.width() == before.width() && after.height() == before.height(),
          "The framebuffer size changed: " + before.width() + "x" + before.height() + " -> " + after.width() + "x" + after.height());

        context.runOnClient(mc -> {
            final int id = SDLVideo.SDL_GetWindowID(mc.getWindow().handle());
            push(e -> e.key().set(KEY_UP, 0, id, 0, SCANCODE_W, KEY_W, (short) 0, (short) 0, false, false));
            push(e -> e.key().set(KEY_UP, 0, id, 0, SCANCODE_E, KEY_E, (short) 0, (short) 0, false, false));
            push(e -> e.key().set(KEY_UP, 0, id, 0, SCANCODE_ESCAPE, KEY_ESCAPE, (short) 0, (short) 0, false, false));
            push(e -> e.button().set(MOUSE_BUTTON_UP, 0, id, 0, (byte) 1, false, (byte) 1, 100, 100));
            push(e -> e.button().set(MOUSE_BUTTON_UP, 0, id, 0, (byte) 3, false, (byte) 1, 100, 100));
            push(e -> e.window().set(WINDOW_FOCUS_GAINED, 0, id, 0, 0));
            push(e -> e.window().set(WINDOW_RESTORED, 0, id, 0, 0));
        });
        context.waitTicks(2);
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // On a screen: every input channel, then simulated input as the control
    // --------------------------------------------------------------------------------------------------------------------------------------

    /**
     * A screen that records every input it gets.
     */
    private static final class RecordingScreen extends Screen
    {
        private final List<String> received = Collections.synchronizedList(new ArrayList<>());

        RecordingScreen()
        {
            super(Component.literal("UI-08 input recorder"));
        }

        @Override
        public boolean isPauseScreen()
        {
            return false;
        }

        @Override
        public boolean keyPressed(final KeyEvent event)
        {
            received.add("keyPressed " + event.key());
            return true;
        }

        @Override
        public boolean keyReleased(final KeyEvent event)
        {
            received.add("keyReleased " + event.key());
            return true;
        }

        @Override
        public boolean charTyped(final CharacterEvent event)
        {
            received.add("charTyped " + event.codepoint());
            return true;
        }

        @Override
        public boolean preeditUpdated(@Nullable final PreeditEvent event)
        {
            if (event != null)
            {
                received.add("preedit " + event);
            }
            return true;
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick)
        {
            received.add("mouseClicked " + event.button());
            return true;
        }

        @Override
        public boolean mouseReleased(final MouseButtonEvent event)
        {
            received.add("mouseReleased " + event.button());
            return true;
        }

        @Override
        public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY)
        {
            received.add("mouseScrolled " + scrollY);
            return true;
        }

        @Override
        public void onFilesDrop(final List<Path> files)
        {
            received.add("filesDropped " + files);
        }

        boolean got(final String prefix)
        {
            synchronized (received)
            {
                return received.stream().anyMatch(s -> s.startsWith(prefix));
            }
        }
    }

    /**
     * With a screen open, real keys, text input, text editing (IME), mouse movement, clicks, the wheel and a dropped file reach neither
     * the screen nor the game's cursor position. Then the same kinds of input from the test ({@code context.getInput()}) do reach the
     * screen, so the blocking doesn't hit simulated input and the screen really listens.
     */
    private static void screenInput(final ClientGameTestContext context)
    {
        final RecordingScreen screen = context.computeOnClient(mc -> new RecordingScreen());
        context.runOnClient(mc -> mc.gui.setScreen(screen));
        context.waitFor(mc -> mc.gui.screen() == screen, 40);
        context.getInput().setCursorPos(40, 30);
        context.waitTicks(2);
        final double[] cursorBefore = context.computeOnClient(mc -> new double[] {mc.mouseHandler.xpos(), mc.mouseHandler.ypos()});
        screen.received.clear();

        // Text pointers in pushed events must stay valid until the event is read; these few bytes are never freed on purpose.
        final ByteBuffer text = MemoryUtil.memUTF8("zz");
        final ByteBuffer preedit = MemoryUtil.memUTF8("kana");
        final ByteBuffer file = MemoryUtil.memUTF8(Path.of("build.gradle").toAbsolutePath().toString());
        context.runOnClient(mc -> {
            final int id = SDLVideo.SDL_GetWindowID(mc.getWindow().handle());
            push(e -> e.key().set(KEY_DOWN, 0, id, 0, SCANCODE_W, KEY_W, (short) 0, (short) 0, true, false));
            push(e -> e.key().set(KEY_UP, 0, id, 0, SCANCODE_W, KEY_W, (short) 0, (short) 0, false, false));
            push(e -> e.edit().set(TEXT_EDITING, 0, id, preedit, 0, 4));
            push(e -> e.text().set(TEXT_INPUT, 0, id, text));
            push(e -> e.motion().set(MOUSE_MOTION, 0, id, 0, 0, 500, 300, 460, 270));
            push(e -> e.button().set(MOUSE_BUTTON_DOWN, 0, id, 0, (byte) 1, true, (byte) 1, 500, 300));
            push(e -> e.button().set(MOUSE_BUTTON_UP, 0, id, 0, (byte) 1, false, (byte) 1, 500, 300));
            push(e -> e.wheel().set(MOUSE_WHEEL, 0, id, 0, 0, 2, 0, 500, 300, 0, 2));
            push(e -> e.drop().set(DROP_BEGIN, 0, id, 0, 0, null, null));
            push(e -> e.drop().set(DROP_FILE, 0, id, 500, 300, null, file));
            push(e -> e.drop().set(DROP_COMPLETE, 0, id, 500, 300, null, null));
        });
        context.waitTicks(10);
        final double[] cursorAfter = context.computeOnClient(mc -> new double[] {mc.mouseHandler.xpos(), mc.mouseHandler.ypos()});
        assertThat(screen.received.isEmpty(), "Real input reached the screen: " + screen.received);
        assertThat(Arrays.equals(cursorBefore, cursorAfter),
          "Real mouse movement moved the game's cursor: " + Arrays.toString(cursorBefore) + " -> " + Arrays.toString(cursorAfter));
        assertThat(context.computeOnClient(mc -> mc.gui.screen() == screen), "The screen was closed by real input");

        // Resizing the GUI reads the real cursor position in a normal game; in a test run the test's cursor position stays.
        context.runOnClient(Minecraft::resizeGui);
        final double[] cursorResized = context.computeOnClient(mc -> new double[] {mc.mouseHandler.xpos(), mc.mouseHandler.ypos()});
        assertThat(Arrays.equals(cursorBefore, cursorResized),
          "Resizing the GUI took the real mouse position: " + Arrays.toString(cursorBefore) + " -> " + Arrays.toString(cursorResized));

        // Control: simulated input still arrives.
        context.getInput().pressKey(InputConstants.KEY_A);
        context.getInput().typeChars("ok");
        context.getInput().setCursorPos(60, 50);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.getInput().scroll(1);
        context.waitTicks(2);
        for (final String kind : List.of("keyPressed", "keyReleased", "charTyped", "mouseClicked", "mouseReleased", "mouseScrolled"))
        {
            assertThat(screen.got(kind), "Simulated input (" + kind + ") didn't reach the screen: " + screen.received);
        }
        final double[] cursorMoved = context.computeOnClient(mc -> new double[] {mc.mouseHandler.xpos(), mc.mouseHandler.ypos()});
        assertThat(!Arrays.equals(cursorBefore, cursorMoved), "Simulated cursor movement didn't move the game's cursor: " + Arrays.toString(cursorMoved));
        closeScreen(context);
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // The real window: resize, maximize, minimize
    // --------------------------------------------------------------------------------------------------------------------------------------

    /**
     * The real window is resized, maximized and minimized (through SDL, as the window system would): the game's framebuffer and the
     * screenshots keep their size, and while minimized the game keeps ticking and rendering and doesn't pause.
     */
    private static void windowChanges(final ClientGameTestContext context, final TestServerContext server, final List<Path> shots)
    {
        final int[] fb = context.computeOnClient(mc -> new int[] {mc.getWindow().getWidth(), mc.getWindow().getHeight()});
        final int[] real = context.computeOnClient(mc -> windowSize(mc.getWindow().handle()));
        final Path reference = screenshot(context, "ui08-reference");
        shots.add(reference);
        final int[] shot = imageSize(reference);
        try
        {
            context.runOnClient(mc -> SDLVideo.SDL_SetWindowSize(mc.getWindow().handle(), real[0] + 160, real[1] + 90));
            settle(context);
            assertSameSize(context, fb, shot, shots, "ui08-resized", "resizing the window");

            context.runOnClient(mc -> SDLVideo.SDL_MaximizeWindow(mc.getWindow().handle()));
            settle(context);
            assertSameSize(context, fb, shot, shots, "ui08-maximized", "maximizing the window");
            context.runOnClient(mc -> SDLVideo.SDL_RestoreWindow(mc.getWindow().handle()));
            settle(context);

            final long clientTime = context.computeOnClient(mc -> mc.level.getGameTime());
            final long serverTime = server.computeOnServer(s -> s.overworld().getGameTime());
            context.runOnClient(mc -> SDLVideo.SDL_MinimizeWindow(mc.getWindow().handle()));
            context.runOnClient(mc -> SDLVideo.SDL_SyncWindow(mc.getWindow().handle()));
            waitMillis(context, FOCUS_LOSS_WAIT_MS);
            final boolean reallyMinimized = context.computeOnClient(mc -> (SDLVideo.SDL_GetWindowFlags(mc.getWindow().handle()) & SDLVideo.SDL_WINDOW_MINIMIZED) != 0);
            LOGGER.info("UI-08: the window is minimized for the window system: {}", reallyMinimized);
            assertThat(!context.computeOnClient(mc -> mc.getWindow().isIconified()), "The game counts the window as minimized");
            assertThat(context.computeOnClient(mc -> String.valueOf(mc.gui.screen())).equals("null"), "A screen opened while minimized");
            assertThat(!context.computeOnClient(Minecraft::isPaused), "The client paused while minimized");
            assertThat(!server.computeOnServer(s -> s.isPaused()), "The integrated server paused while minimized");
            assertThat(context.computeOnClient(mc -> mc.level.getGameTime()) > clientTime, "The client stopped ticking while minimized");
            assertThat(server.computeOnServer(s -> s.overworld().getGameTime()) > serverTime, "The integrated server stopped ticking while minimized");
            assertSameSize(context, fb, shot, shots, "ui08-minimized", "minimizing the window (really minimized: " + reallyMinimized + ")");
        }
        finally
        {
            context.runOnClient(mc -> {
                SDLVideo.SDL_RestoreWindow(mc.getWindow().handle());
                SDLVideo.SDL_SetWindowSize(mc.getWindow().handle(), real[0], real[1]);
            });
            settle(context);
        }
    }

    private static void assertSameSize(final ClientGameTestContext context, final int[] fb, final int[] shot, final List<Path> shots, final String name, final String what)
    {
        final int[] fbNow = context.computeOnClient(mc -> new int[] {mc.getWindow().getWidth(), mc.getWindow().getHeight()});
        assertThat(Arrays.equals(fb, fbNow), "The framebuffer size changed after " + what + ": " + Arrays.toString(fb) + " -> " + Arrays.toString(fbNow));
        final Path path = screenshot(context, name);
        shots.add(path);
        final int[] shotNow = imageSize(path);
        assertThat(Arrays.equals(shot, shotNow), "The screenshot size changed after " + what + ": " + Arrays.toString(shot) + " -> " + Arrays.toString(shotNow));
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // The cursor stays free
    // --------------------------------------------------------------------------------------------------------------------------------------

    /**
     * In the world without a screen Minecraft would capture the mouse (relative mouse mode: hidden, locked in the window). In a test run it
     * doesn't, so the person at the computer keeps the mouse.
     */
    private static void cursorNotCaptured(final ClientGameTestContext context)
    {
        closeScreen(context);
        context.runOnClient(mc -> mc.mouseHandler.grabMouse());
        context.waitTicks(2);
        assertThat(!context.computeOnClient(mc -> SDLMouse.SDL_GetWindowRelativeMouseMode(mc.getWindow().handle())),
          "The test window captured the mouse (relative mouse mode is on)");
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface EventFiller
    {
        void fill(SDL_Event event);
    }

    /**
     * Pushes one event into the window's SDL event queue, where real events from the operating system arrive.
     */
    private static void push(final EventFiller filler)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final SDL_Event event = SDL_Event.calloc(stack);
            filler.fill(event);
            if (!SDLEvents.SDL_PushEvent(event))
            {
                throw new AssertionError("SDL_PushEvent failed for event type 0x" + Integer.toHexString(event.type()));
            }
        }
    }

    private static int[] windowSize(final long handle)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final IntBuffer w = stack.mallocInt(1);
            final IntBuffer h = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSize(handle, w, h);
            return new int[] {w.get(0), h.get(0)};
        }
    }

    private static int[] imageSize(final Path path)
    {
        try
        {
            final BufferedImage image = ImageIO.read(path.toFile());
            return new int[] {image.getWidth(), image.getHeight()};
        }
        catch (final IOException e)
        {
            throw new AssertionError("Can't read screenshot " + path, e);
        }
    }

    /**
     * Lets the window system apply a window change and the game handle its events.
     */
    private static void settle(final ClientGameTestContext context)
    {
        context.runOnClient(mc -> SDLVideo.SDL_SyncWindow(mc.getWindow().handle()));
        waitMillis(context, 500);
    }

    /**
     * Waits (ticking) for at least {@code millis} of real time: "Pause on Lost Focus" counts real time.
     */
    private static void waitMillis(final ClientGameTestContext context, final long millis)
    {
        final long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end)
        {
            context.waitTick();
        }
    }
}
