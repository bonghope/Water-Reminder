package com.aqua.water;

import com.aqua.water.ui.PetLevels;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PetLevelsTest {
    @Test public void eachLevelUnlocksAtItsThreshold() {
        int[] thresholds = {0, 601, 1401, 2001};
        for (int level = 1; level < thresholds.length; level++) {
            int threshold = thresholds[level];
            assertEquals(threshold, PetLevels.requiredMl(level, 2001));
            assertEquals(level - 1, PetLevels.level(threshold - 1, 2001));
            assertEquals(level, PetLevels.level(threshold, 2001));
        }
    }
}
