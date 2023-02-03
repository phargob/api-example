package rsb.methods;

import net.runelite.api.WorldType;

public class WorldHopper {

    private MethodContext ctx;
    WorldHopper(final MethodContext ctx) {
        this.ctx = ctx;
    }

    public boolean isCurrentWorldMembers() {
        return ctx.proxy.getWorldType().stream().anyMatch((worldType) -> worldType == WorldType.MEMBERS);
    }
}
