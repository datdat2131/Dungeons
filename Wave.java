package com.astrax.dungeon.mob;

import java.util.List;

/** kind: NORMAL, ELITE, SWARM, MINIBOSS. */
public record Wave(int index, String kind, List<MobSpec> mobs) {}
