package com.akshaykadam.pixelboard.extension.updater;

import org.junit.Assert;
import org.junit.Test;

public final class BouUpdateNotifierTest {

    private static final long DAY = BouUpdateNotifier.CHECK_INTERVAL_MS;

    @Test
    public void checksOncePerDay() {
        long now = 10 * DAY;
        Assert.assertTrue("never checked", BouUpdateNotifier.isDue(0, now));
        Assert.assertFalse("checked an hour ago", BouUpdateNotifier.isDue(now - DAY / 24, now));
        Assert.assertTrue("checked a day ago", BouUpdateNotifier.isDue(now - DAY, now));
        Assert.assertTrue("clock moved backwards", BouUpdateNotifier.isDue(now + 5000, now));
    }

    @Test
    public void ignoresMissingContext() {
        BouUpdateNotifier.maybeCheck(null);
    }
}
