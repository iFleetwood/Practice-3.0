package cc.kasumi.practice.nametag.type;

import cc.kasumi.practice.nametag.NametagProvider;
import cc.kasumi.practice.player.PlayerState;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ProviderResolver {

    private final Map<PlayerState, NametagProvider> nametagProviders = new HashMap<>();

    public ProviderResolver() {
        nametagProviders.put(PlayerState.LOBBY, new LobbyNametagProvider());
        nametagProviders.put(PlayerState.QUEUEING, new QueueingNametagProvider());
        nametagProviders.put(PlayerState.EDITING, new LobbyNametagProvider());
        nametagProviders.put(PlayerState.SPECTATING, new SpectatingNametagProvider());
        nametagProviders.put(PlayerState.PLAYING, new PlayingNametagProvider());
    }
}