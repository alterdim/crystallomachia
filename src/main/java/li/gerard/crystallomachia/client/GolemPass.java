package li.gerard.crystallomachia.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemPart;
import li.gerard.crystallomachia.golem.GolemParts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;

public record GolemPass(Set<GolemPart> parts, Identifier texture, int color) {
    public static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(crystallomachia.MODID, "textures/golem/stone.png");

    public static List<GolemPass> forParts(GolemParts parts) {
        Map<Optional<ResourceKey<GolemMaterial>>, Set<GolemPart>> groups = new LinkedHashMap<>();
        for (GolemPart part : GolemPart.values()) {
            groups.computeIfAbsent(Optional.ofNullable(parts.material(part)), key -> EnumSet.noneOf(GolemPart.class)).add(part);
        }
        List<GolemPass> passes = new ArrayList<>();
        groups.forEach((key, group) -> {
            GolemMaterial material = key.map(GolemPass::lookup).orElse(null);
            if (material == null) {
                passes.add(new GolemPass(group, DEFAULT_TEXTURE, 0xFFFFFFFF));
            } else if (material.texture().isPresent()) {
                passes.add(new GolemPass(group, material.texture().get(), 0xFFFFFFFF));
            } else {
                passes.add(new GolemPass(group, DEFAULT_TEXTURE, ARGB.opaque(material.color())));
            }
        });
        return passes;
    }

    private static @Nullable GolemMaterial lookup(ResourceKey<GolemMaterial> key) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return null;
        }
        return connection.registryAccess().lookup(GolemMaterial.REGISTRY_KEY)
                .flatMap(registry -> registry.get(key))
                .map(holder -> holder.value())
                .orElse(null);
    }
}
