package com.minecolonies.gametest.framework;

import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Splits batches of the colony environment ({@link ColonyFixture#COLONY_ENVIRONMENT}) into one batch per test, so colony tests run one at a
 * time. Other batches are left alone and run first; colony tests follow in a stable order (by test id).
 */
public final class SerialBatches
{
    private SerialBatches()
    {
    }

    public static List<GameTestBatch> split(final Collection<GameTestBatch> batches)
    {
        final List<GameTestBatch> parallel = new ArrayList<>();
        final List<GameTestBatch> serial = new ArrayList<>();
        for (final GameTestBatch batch : batches)
        {
            if (!batch.environment().is(ColonyFixture.COLONY_ENVIRONMENT_ID))
            {
                parallel.add(batch);
                continue;
            }

            final List<GameTestInfo> infos = new ArrayList<>(batch.gameTestInfos());
            infos.sort(Comparator.comparing(info -> info.id().toString()));
            for (final GameTestInfo info : infos)
            {
                serial.add(new GameTestBatch(serial.size(), List.of(info), batch.environment(), batch.dimension()));
            }
        }

        final List<GameTestBatch> result = new ArrayList<>(parallel);
        result.addAll(serial);
        return result;
    }
}
