package com.fabricharvester.client;

import com.fabricharvester.mixin.ClientPlayerInteractionManagerAccessor;
import com.fabricharvester.mixin.MinecraftClientAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CooldownManagerTest {
    @Test
    void safeProfileCapsButDoesNotIncreaseBreakCooldown() {
        FakeBreakAccessor accessor = new FakeBreakAccessor(5);
        FastBreakManager.tick(true, 1, accessor);
        assertEquals(1, accessor.getBlockBreakingCooldown());

        accessor.setBlockBreakingCooldown(0);
        FastBreakManager.tick(true, 1, accessor);
        assertEquals(0, accessor.getBlockBreakingCooldown());
    }

    @Test
    void trustedProfileClearsPlaceCooldown() {
        FakePlaceAccessor accessor = new FakePlaceAccessor(4);
        FastPlaceManager.tick(true, 0, accessor);
        assertEquals(0, accessor.getItemUseCooldown());
    }

    @Test
    void disabledManagersLeaveCooldownsUntouched() {
        FakeBreakAccessor breakAccessor = new FakeBreakAccessor(5);
        FakePlaceAccessor placeAccessor = new FakePlaceAccessor(4);
        FastBreakManager.tick(false, 0, breakAccessor);
        FastPlaceManager.tick(false, 0, placeAccessor);
        assertEquals(5, breakAccessor.getBlockBreakingCooldown());
        assertEquals(4, placeAccessor.getItemUseCooldown());
    }

    private static final class FakeBreakAccessor implements ClientPlayerInteractionManagerAccessor {
        private int cooldown;

        private FakeBreakAccessor(int cooldown) {
            this.cooldown = cooldown;
        }

        @Override
        public int getBlockBreakingCooldown() {
            return cooldown;
        }

        @Override
        public void setBlockBreakingCooldown(int cooldown) {
            this.cooldown = cooldown;
        }
    }

    private static final class FakePlaceAccessor implements MinecraftClientAccessor {
        private int cooldown;

        private FakePlaceAccessor(int cooldown) {
            this.cooldown = cooldown;
        }

        @Override
        public int getItemUseCooldown() {
            return cooldown;
        }

        @Override
        public void setItemUseCooldown(int cooldown) {
            this.cooldown = cooldown;
        }
    }
}

