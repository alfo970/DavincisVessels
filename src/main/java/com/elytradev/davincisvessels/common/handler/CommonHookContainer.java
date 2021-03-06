package com.elytradev.davincisvessels.common.handler;


import com.elytradev.davincisvessels.common.content.DavincisVesselsContent;
import com.elytradev.davincisvessels.common.entity.EntitySeat;
import com.elytradev.davincisvessels.common.entity.EntityShip;
import com.elytradev.davincisvessels.common.content.block.BlockHelm;
import com.elytradev.davincisvessels.common.tileentity.TileCrate;
import com.elytradev.davincisvessels.common.tileentity.TileEntitySecuredBed;
import com.elytradev.movingworld.common.chunk.LocatedBlock;
import com.elytradev.movingworld.common.event.DisassembleBlockEvent;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class CommonHookContainer {
    @SubscribeEvent
    public void onInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntityPlayer() != null) {
            int x = MathHelper.floor(event.getTarget().posX);
            int y = MathHelper.floor(event.getTarget().posY);
            int z = MathHelper.floor(event.getTarget().posZ);

            TileEntity te = event.getEntity().world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileCrate && ((TileCrate) te).getContainedEntity() == event.getTarget()) {
                ((TileCrate) te).releaseEntity();
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerSpawnSet(PlayerSetSpawnEvent e) {
        if (e.isCanceled())
            return;

        if (e.getEntityPlayer().getGameProfile() != null && e.getEntityPlayer().getGameProfile().getId() != null &&
                ConnectionHandler.playerBedMap.containsKey(e.getEntityPlayer().getGameProfile().getId())) {
            //Spawn for the player is changing and they use a secured bed, clear the map of the player.

            TileEntitySecuredBed bed = ConnectionHandler.playerBedMap.get(e.getEntityPlayer().getGameProfile().getId());

            if (bed.getPos().equals(e.getNewSpawn()))
                return;

            ConnectionHandler.playerBedMap.remove(e.getEntityPlayer().getGameProfile().getId());
        }
    }

    @SubscribeEvent
    public void onDisassembleBlock(DisassembleBlockEvent event) {
        // Used to transform user position when a ship is disassembled.
        if (event.movingWorld instanceof EntityShip) {
            EntityShip ship = (EntityShip) event.movingWorld;
            LocatedBlock lb = event.block;
            if (lb.state.getBlock() == DavincisVesselsContent.blockMarkShip) {
                Entity passenger = ship.controllingPassenger != null ? ship.controllingPassenger : ship.prevRiddenByEntity;

                if (passenger != null) {
                    BlockPos position = lb.pos.offset(lb.state.getValue(BlockHelm.FACING));
                    passenger.stopRiding();
                    passenger.setPositionAndUpdate(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
                    System.out.println(passenger.getPositionVector().toString());
                }
            } else if (lb.state.getBlock() == DavincisVesselsContent.blockSeat) {
                Optional<EntitySeat> matchingSeatEntity = ship.capabilities.getSeats().stream().filter(s -> s.getChunkPos().equals(lb.posNoOffset)).findFirst();

                if (matchingSeatEntity.isPresent()) {
                    EntitySeat matchingSeat = matchingSeatEntity.get();
                    if (matchingSeat.getControllingPassenger() != null) {
                        matchingSeat.getControllingPassenger().dismountRidingEntity();
                        matchingSeat.getControllingPassenger().setPosition(lb.pos.getX() + 0.5D, lb.pos.getY() + 0.5D, lb.pos.getZ() + 0.5D);
                    }
                }
            }
        }
    }
}
